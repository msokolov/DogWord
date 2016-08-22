package net.falutin.dogword;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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
 * Word search game. Run your finger over the letters and find all the dictionary.
 * TODO: classify dictionary as rare/common
 *   figure out expected score based on number of available dictionary, rarity of dictionary
 *   learn a handicap
 *   achievements:
 *     score over 50, 80, 100, 150
 *     get a six, seven- or eight+ letter word
 * TODO: More even letter distribution?
 *   guarantee at least one seven-letter word?
 */
public class DogWord extends ActionBarActivity {

    public static final String TAG = DogWord.class.getName();

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

    /**
     * Indicates the method of showing words that have not yet been found. Initially this is None,
     *
     */
    private enum HintStyle {
        None,
        Length,
        RevealWord
    }

    private int initTimerMillis = 3 * 60 * 1000;
    private static final int MSEC_PER_POINT = 2000;

    private CellGridLayout gridLayout;
    private TextView displayArea;
    private TextView progressArea;
    private PopupWindow popup;

    private final Handler handler = new Handler();
    private final Runnable timerCB = createTimerCallback();

    private LetterTree dictionary;
    private GridWordFinder wordFinder;
    private HashSet<String> wordsFound;
    private String[] gridWords;
    private int score;
    private boolean isTimed;
    private boolean gameOver;
    private long startTime;
    private long elapsedMillis; // saved when paused
    private HintStyle hintStyle = HintStyle.None;

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
        progressArea = (TextView) findViewById(R.id.progress);
        gridLayout.setCanvasView((CanvasView) findViewById(R.id.canvas));

        try {
            // load the dictionary
            // Our dictionary is WORDS trimmed down to words that occurred at least 500 times
            // in the Google Books corpus in 2008, plus more obscure words, proper names,
            // abbreviations removed by other means.
            if (dictionary == null) {
                dictionary = LetterTree.read(new DataInputStream(getResources().openRawResource(R.raw.words)));
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to read dictionary");
        }
        wordFinder = new GridWordFinder(dictionary);
        wordsFound = new HashSet<>();
        if (savedInstanceState != null) {
            onRestoreGame(savedInstanceState);
        } else {
            onNewGame();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resume();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        resume();
    }

    private void resume() {
        Log.d(TAG, "resume " + elapsedMillis);
        if (elapsedMillis > 0) {
            resetStartTime();
            elapsedMillis = -1;
        }
        updateProgress();
        startTimer();
    }

    @Override
    public void onPause () {
        super.onPause();
        stop();
    }

    @Override
    public void onStop() {
        super.onStop();
        stop();
    }

    private void stop() {
        stopTimer();
        if (elapsedMillis < 0) {
            elapsedMillis = elapsedMillis();
        }
        Log.d(TAG, "stop " + elapsedMillis);
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
        gridWords = findAllWords();
        score = 0;
        startTime = System.currentTimeMillis();
        elapsedMillis = -1;
        hintStyle = HintStyle.None;
        updateProgress();
        gameOver = false;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        isTimed = sharedPref.getBoolean("pref_enable_timer", true);
        if (isTimed) {
            startTimer();
        } else {
            stopTimer();
        }
        initTimerMillis = Integer.valueOf(sharedPref.getString("pref_timer_minutes", "3")) * 60 * 1000;
    }

    public void onRestoreGame(Bundle state) {
        CellGrid grid = new CellGrid(4, 4);
        grid.setCells(state.getString("grid"));
        gridLayout.setGrid(grid);
        gridWords = findAllWords();
        String[] wf = state.getStringArray("wordsFound");
        if (wf != null) {
            wordsFound.addAll(Arrays.asList(wf));
        }
        gameOver = state.getBoolean("gameOver");
        score = state.getInt("score");
        elapsedMillis = state.getInt("elapsedSeconds") * 1000;
        updateWordList();
        resetStartTime();
        startTimer();
    }

    private void resetStartTime () {
        startTime = System.currentTimeMillis() - elapsedMillis;
    }

    private int getTotalTime () {
        return initTimerMillis + MSEC_PER_POINT * score;
    }

    private long elapsedMillis () {
        return System.currentTimeMillis() - startTime;
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("grid", gridLayout.getGrid().toString());
        outState.putBoolean("gameOver", gameOver);
        outState.putStringArray("wordsFound", wordsFound.toArray(new String[wordsFound.size()]));
        outState.putInt("score", score);
        outState.putInt("elapsedSeconds", (int) (elapsedMillis() / 1000));
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
            case R.id.new_game:
                onNewGame();
                return true;
            case R.id.pause:
                // hides this activity as if the user pressed the back button
                onBackPressed();
                return true;
            case R.id.game_over:
                onGameOver();
                return true;
            case R.id.action_settings:
                startActivity(new Intent().setClass(getApplicationContext(), SettingsActivity.class));
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
                if (isTimed) { // how else did we get here?
                    if (millisRemaining() <= 0) {
                        onTimeExpired();
                    } else {
                        handler.postDelayed(this, 1000);
                    }
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
        String status;
        if (isTimed) {
            status = String.format("Score: %d (%d/%d) %02d:%02d", score, wordsFound.size(),
                    gridWords.length, mins, secs);
        } else {
            status = String.format("Score: %d (%d/%d)", score, wordsFound.size(),
                    gridWords.length);
        }
        progressArea.setText(status);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) {
            return false;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            String word = gridLayout.getSelectedWord();
            if (word.length() >= 3 && dictionary.contains(word.toLowerCase())) {
                if (wordsFound.contains(word)) {
                    gridLayout.highlightSelection(CellGridLayout.SelectionKind.ALREADY);
                } else {
                    wordsFound.add(word);
                    updateWordList();
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

    private void onTimeExpired() {
        if (isTimed) {
            stopTimer();
        }
        if (gameOver || isFinishing()) {
            return;
        }
        showAchievements();
        hintStyle = HintStyle.Length;
        updateWordList();
    }

    private void onGameOver() {
        if (gameOver || isFinishing()) {
            return;
        }
        gameOver = true;
        if (isTimed) {
            stopTimer();
        }
        gridLayout.setEnabled(false);
        gridLayout.invalidate();
        hintStyle = HintStyle.RevealWord;
        updateWordList();
        fileMintReport();
    }

    private void showAchievements() {
        gridLayout.dimGrid();
        int maxScore = wordFinder.computeMaxScore(gridLayout.getGrid());
        //Log.d(gWord.TAG, achievement);
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
        if (popup != null) {
            popup.dismiss();
            popup = null;
        }
    }

    private String describeAchievement (int score, int maxScore) {
        // assume a normal distribution with mean = maxScore / 2
        // and variance s.t. 4 std.dev. covers from 0-max
        return SCORE_PLAUDITS[SCORE_PLAUDITS.length * score / (maxScore + 1)];
    }

    private void fileMintReport () {
        HashMap<String,Object> report = new HashMap<>();
        report.put("grid", gridLayout.getGrid().toString());
        report.put("found", wordsFound);
        Mint.logEvent("GameOver", MintLogLevel.Info, "grid", gridLayout.toString());
        Mint.logEvent("GameOver", MintLogLevel.Info, "found", wordsFound.toString());
        Mint.logEvent("GameOver", MintLogLevel.Info, report);
    }

    private String[] findAllWords () {
        Set<String> wordSet = wordFinder.findWords(gridLayout.getGrid());
        String[] words = wordSet.toArray(new String[wordSet.size()]);
        Arrays.sort(words);
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].toUpperCase();
        }
        return words;
    }

    private void updateWordList () {
        displayArea.setText(formatWordList(hintStyle));
    }

    // return a string listing all the words in alphabetical order, with unfound words represented
    // by a string of underscores.
    private String formatWordList(HintStyle hintStyle) {
        StringBuilder buf = new StringBuilder();
        int seqLen = 0;
        for (String word : gridWords) {
            if (buf.length() != 0) {
                buf.append(", ");
            }
            if (!wordsFound.contains(word)) {
                seqLen = 0;
                switch (hintStyle) {
                    case RevealWord:
                        buf.append('(').append(word).append(')');
                        break;
                    case Length:
                        buf.append("(").append(word.length()).append(")");
                        break;
                    case None:
                }
            } else {
                if (hintStyle == HintStyle.RevealWord || seqLen < 3) {
                    buf.append(word);
                } else if (seqLen == 3) {
                    buf.append ("...");
                }
                ++seqLen;
            }
        }
        return buf.toString();
    }

}
