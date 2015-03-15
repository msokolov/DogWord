package net.falutin.wordbog;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.splunk.mint.Mint;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Word search game. Run your finger over the letters and find all the words.
 * TODO: prune dictionary
 * TODO: create objectives, timing (add score and time for each word found)
 * TODO: More even letter distribution - and handle "QU"
 * TODO: visual feedback on found word (flash the word brightly), already found word (dim the word)
 */
public class DogWord extends ActionBarActivity {

    public static final String TAG = "DogWord";

    private CellGridLayout gridLayout;
    private TextView displayArea;
    private TextView progressArea;
    private ScrollView scrollView;
    private CanvasView canvasView;
    private LetterTree words;
    private GridWordFinder wordFinder;
    private HashSet<String> wordsFound;
    private int numWordsToFind;
    private boolean gameOver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 854b552a43ef65ffd873779
        // 7de6e2e0
        //Mint.initAndStartSession(this, "854b552a43ef65ffd873779");
        // Mint.initAndStartSession(DogWord.this, "39338683");
        Mint.initAndStartSession(this, "7de6e2e0");
        Mint.logEvent("Hello, World!");
        setContentView(R.layout.activity_bog_word);
        gridLayout = (CellGridLayout) findViewById(R.id.grid);
        displayArea = (TextView) findViewById(R.id.display);
        scrollView = (ScrollView) findViewById(R.id.scroller);
        progressArea = (TextView) findViewById(R.id.progress);
        canvasView = (CanvasView) findViewById(R.id.canvas);
        gridLayout.setCanvasView(canvasView);

        try {
            // load the dictionary
            // Our dictionary is WORDS trimmed down to words that occurred at least 500 times
            // in the Google Books corpus in 2008.
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

    public void onNewGame () {
        CellGrid grid = new CellGrid(4, 4);
        grid.randomize();
        gridLayout.setGrid (grid);
        displayArea.setText("");
        wordsFound.clear();
        numWordsToFind = wordFinder.findWords(grid).size();
        updateProgress();
        gameOver = false;
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
    }

    private static String join(String by, String[] strings) {
        if (strings.length == 0) {
            return "";
        }
        StringBuilder statusText = new StringBuilder();
        statusText.append(strings[0]);
        for (int i = 1; i < strings.length; i++) {
            statusText.append(by).append(strings[i]);
        }
        return statusText.toString();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("grid", gridLayout.getGrid().toString());
        outState.putBoolean("gameOver", gameOver);
        outState.putStringArray("wordsFound", wordsFound.toArray(new String[wordsFound.size()]));
        outState.putInt("numWords", numWordsToFind);
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
                if (!gameOver) {
                    onGameOver();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart () {
        updateProgress();
        super.onStart();
    }

    public void updateProgress() {
        String status = String.format("%d/%d", wordsFound.size(), numWordsToFind);
        Log.d(DogWord.TAG, "progress: " + status);
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
                    updateProgress();
                }
            } else {
                gridLayout.highlightSelection(CellGridLayout.SelectionKind.NONE);
            }
            gridLayout.clearPath();
        }
        return false;
    }

    private void onGameOver() {
        gameOver = true;
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
        scrollView.fullScroll(View.FOCUS_DOWN);
        gridLayout.highlightSelection(CellGridLayout.SelectionKind.NONE);
    }
}
