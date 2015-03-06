package net.falutin.wordbog;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;


public class DogWord extends ActionBarActivity {

    public static final String TAG = "BogWord";

    private CellGridLayout grid;
    private TextView displayArea;
    private TextView progressArea;
    private LetterTree words;
    private GridWordFinder wordFinder;
    private HashSet<String> wordsFound;
    private int numWordsToFind;
    private boolean gameOver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bog_word);
        grid = (CellGridLayout) findViewById(R.id.grid);
        displayArea = (TextView) findViewById(R.id.display);
        progressArea = (TextView) findViewById(R.id.progress);
        // TODO: implement onSaveInstanceState() and save all the stuff there
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
        // FIXME: separate grid data from grid UI and initialize game state independently
        // of onLayout (so that progress view is updated correctly).
        onNewGame();
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
                gameOver = true;
                showWordsNotFound();
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

    public void onNewGame () {
        grid.randomize();
        displayArea.setText("");
        wordsFound.clear();
        numWordsToFind = wordFinder.findWords(grid).size();
        updateProgress();
        gameOver = false;
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
            String word = grid.finishPath();
            if (word.length() >= 3) {
                Log.d(DogWord.TAG, "lookup " + word);
                if (words.contains(word.toLowerCase()) && !wordsFound.contains(word)) {
                    Log.d(DogWord.TAG, "found " + word);
                    wordsFound.add(word);
                    if (wordsFound.size() > 1) {
                        displayArea.setText(displayArea.getText() + " " + word);
                    } else {
                        displayArea.setText(word);
                    }
                    updateProgress();
                }
            }
            grid.clearSelection();
            return true;
        }
        return false;
    }

    private void showWordsNotFound() {
        displayArea.setText(displayArea.getText() + "\n +");
        // TODO: hide or shrink the grid?  Do something to show all the words ...
        for (String word : wordFinder.findWords(grid)) {
            word = word.toUpperCase();
            if (!wordsFound.contains(word)) {
                displayArea.setText(displayArea.getText() + " " + word);
            }
        }
    }
}