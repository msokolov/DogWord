package net.falutin.dogword;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.splunk.mint.Mint;
import com.splunk.mint.MintLogLevel;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Word search game. Run your finger over the letters and find all the words.
 * TODO: prune dictionary
 * TODO: create objectives, timing
 * TODO: classify words as rare/common
 *   figure out expected score based on number of available words, rarity of words
 *   give a rating A-F based on score vs expected score
 *   learn a handicap
 *   achievements:
 *     score over 50, 80, 100, 150
 *     get a six, seven- or eight+ letter word
 * TODO: More even letter distribution?
 *   guarantee at least one seven-letter word?
 */
public class DogWord extends ActionBarActivity {

    public static final String TAG = "DogWord";

    private static final String SCORE_PLAUDITS[] = new String[] {
            "C- Game Over",
            "C  Ok",
            "C+ Fair",
            "B- Good",
            "B+ Very Good",
            "A- Excellent!",
            "A  Superb!!",
            "A+ Genius!!!"
    };

    private static final int INIT_TIMER_MSEC = 3 * 60 * 1000;
    private static final int MSEC_PER_POINT = 2000;

    private CellGridLayout gridLayout;
    private TextView displayArea;
    private TextView progressArea;
    private ScrollView scrollView;
    private PopupWindow popup;

    private final Handler handler = new Handler();
    private final Runnable timerCB = createTimerCallback();

    private LetterTree words;
    private GridWordFinder wordFinder;
    private HashSet<String> wordsFound;

    private int numWordsToFind;
    private int score;
    private boolean gameOver;
    private long startTime;
    private int timeSavedMillis; // saved when paused

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // released:
        Mint.initAndStartSession(DogWord.this, "39338683");
        // testing:
        // Mint.initAndStartSession(this, "7de6e2e0");
        Mint.logEvent("Start");
        setContentView(R.layout.activity_bog_word);
        gridLayout = (CellGridLayout) findViewById(R.id.grid);
        displayArea = (TextView) findViewById(R.id.display);
        scrollView = (ScrollView) findViewById(R.id.scroller);
        progressArea = (TextView) findViewById(R.id.progress);
        gridLayout.setCanvasView((CanvasView) findViewById(R.id.canvas));

        try {
            // load the dictionary
            // Our dictionary is WORDS trimmed down to words that occurred at least 500 times
            // in the Google Books corpus in 2008, plus more obscure words, proper names,
            // abbreviations removed by other means.
            if (words == null) {
                words = LetterTree.read(new DataInputStream(getResources().openRawResource(R.raw.words)));
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to read dictionary");
        }
        wordFinder = new GridWordFinder(words);
        wordsFound = new HashSet<>();
        if (savedInstanceState != null) {
            onRestoreGame(savedInstanceState);
        } else {
            onNewGame();
        }
    }

    @Override
    public void onResume () {
        super.onResume();
        if (timeSavedMillis > 0) {
            resetStartTime();
        }
        updateProgress();
        startTimer();
    }

    @Override
    public void onPause () {
        super.onPause();
        stopTimer();
        timeSavedMillis = millisRemaining();
    }

    public void onNewGame () {
        CellGrid grid = new CellGrid(4, 4);
        grid.randomize();
        gridLayout.setGrid(grid);
        gridLayout.clearSelection();
        displayArea.setText("");
        if (popup != null) {
            dismissPopup();
        }
        wordsFound.clear();
        numWordsToFind = wordFinder.findWords(grid).size();
        score = 0;
        startTime = System.currentTimeMillis();
        timeSavedMillis = -1;
        updateProgress();
        gameOver = false;
        startTimer();
    }

    public void onRestoreGame(Bundle state) {
        CellGrid grid = new CellGrid(4, 4);
        grid.setCells(state.getString("grid"));
        gridLayout.setGrid(grid);
        String[] wf = state.getStringArray("wordsFound");
        displayArea.setText(join(", ", wf));
        wordsFound.addAll(Arrays.asList(wf));
        numWordsToFind = state.getInt("numWords");
        gameOver = state.getBoolean("gameOver");
        score = state.getInt("score");
        timeSavedMillis = state.getInt("secondsRemaining") * 1000;
        resetStartTime();
        startTimer();
    }

    private void resetStartTime () {
        startTime = System.currentTimeMillis() - getTotalTime() + timeSavedMillis;
    }

    private int getTotalTime () {
        return INIT_TIMER_MSEC + MSEC_PER_POINT * score;
    }

    private int millisRemaining () {
        int totalTime = getTotalTime();
        long now = System.currentTimeMillis();
        int remaining = (int) (startTime + totalTime - now);
        if (remaining > 0) {
            return remaining;
        }
        return 0;
    }

    private static String join(String by, String[] strings) {
        if (strings.length == 0) {
            return "";
        }
        StringBuilder buf = new StringBuilder();
        buf.append(strings[0]);
        for (int i = 1; i < strings.length; i++) {
            buf.append(by).append(strings[i]);
        }
        return buf.toString();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("grid", gridLayout.getGrid().toString());
        outState.putBoolean("gameOver", gameOver);
        outState.putStringArray("wordsFound", wordsFound.toArray(new String[wordsFound.size()]));
        outState.putInt("numWords", numWordsToFind);
        outState.putInt("score", score);
        outState.putInt("millisRemaining", millisRemaining());
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bog_word, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.new_game:
                onNewGame();
                return true;
            case R.id.game_over:
                onGameOver();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Runnable createTimerCallback () {
        return new Runnable() {
            @Override
            public void run() {
                updateProgress();
                if (millisRemaining() <= 0) {
                    onGameOver();
                } else {
                    handler.postDelayed(this, 1000);
                }
            }
        };
    }

    private void startTimer() {
        handler.postDelayed(timerCB, 1000);
    }

    private void stopTimer() {
        handler.removeCallbacks(timerCB);
    }

    public void updateProgress() {
        int secs = millisRemaining() / 1000;
        int mins = secs / 60;
        secs = secs % 60;
        String status = String.format("Score: %d (%d/%d) %02d:%02d", score, wordsFound.size(),
                numWordsToFind, mins, secs);
        progressArea.setText(status);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) {
            return false;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            String word = gridLayout.getSelectedWord();
            if (word.length() >= 3 && words.contains(word.toLowerCase())) {
                if (wordsFound.contains(word)) {
                    gridLayout.highlightSelection(CellGridLayout.SelectionKind.ALREADY);
                } else {
                    wordsFound.add(word);
                    if (wordsFound.size() > 1) {
                        displayArea.setText(displayArea.getText() + " " + word);
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    } else {
                        displayArea.setText(word);
                    }
                    gridLayout.highlightSelection(CellGridLayout.SelectionKind.FOUND);
                    score += fibonacci(word.length() - 2);
                    updateProgress();
                }
            } else {
                gridLayout.highlightSelection(CellGridLayout.SelectionKind.NONE);
            }
            gridLayout.clearPath();
        }
        return false;
    }

    public static int fibonacci (int n) {
        int sum1 = 1, sum2 = 0;
        while (n-- > 0) {
            int tmp = sum1;
            sum1 += sum2;
            sum2 = tmp;
        }
        return sum1;
    }

    private void onGameOver() {
        if (gameOver || isFinishing()) {
            return;
        }
        gameOver = true;
        stopTimer();
        showRemainingWords();
        showAchievements();
        gridLayout.invalidate();
        fileMintReport();
    }

    private void showAchievements() {
        gridLayout.dimGrid();
        int maxScore = wordFinder.computeMaxScore(gridLayout.getGrid());
        //Log.d(DogWord.TAG, achievement);
        showPopup(describeAchievement(score, maxScore));
    }

    private void showPopup (String text) {
        LayoutInflater layoutInflater = (LayoutInflater)getBaseContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        LinearLayout popupView = (LinearLayout) layoutInflater.inflate(R.layout.popup_window, null);
        popup = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        ((TextView)popupView.getChildAt(0)).setText(text);
        popup.setTouchable(true);
        popup.setBackgroundDrawable(getResources().getDrawable(R.drawable.found_tile_bg));
        popup.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                dismissPopup();
                return false;
            }
        });
        popup.showAtLocation(gridLayout, Gravity.CENTER, 0, 0);
    }

    private void dismissPopup() {
        popup.dismiss();
        popup = null;
    }

    private String describeAchievement (int score, int maxScore) {
        // assume a normal distribution with mean = maxScore / 2
        // and variance s.t. 4 std.dev. covers from 0-max
        return SCORE_PLAUDITS[SCORE_PLAUDITS.length * score / maxScore];
    }

    private void fileMintReport () {
        HashMap<String,Object> report = new HashMap<>();
        report.put("grid", gridLayout.getGrid().toString());
        report.put("found", wordsFound);
        Mint.logEvent("GameOver", MintLogLevel.Info, "grid", gridLayout.toString());
        Mint.logEvent("GameOver", MintLogLevel.Info, "found", wordsFound.toString());
        Mint.logEvent("GameOver", MintLogLevel.Info, report);
    }

    private void showRemainingWords() {
        displayArea.setText(displayArea.getText() + "\n +");
        Set<String> wordSet = wordFinder.findWords(gridLayout.getGrid());
        String[] words = wordSet.toArray(new String[wordSet.size()]);
        Arrays.sort(words);
        for (String word : words) {
            word = word.toUpperCase();
            if (!wordsFound.contains(word)) {
                displayArea.setText(displayArea.getText() + " " + word);
            }
        }
        displayArea.scrollTo(0, 100);
        scrollView.fullScroll(View.FOCUS_DOWN);
    }

}
