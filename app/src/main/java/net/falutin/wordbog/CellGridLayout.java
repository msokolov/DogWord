package net.falutin.wordbog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Lays out grid cells dynamically and supports grid path selection
 * Created by sokolov on 2/28/2015.
 */
public class CellGridLayout extends RelativeLayout implements Char2d {

    private int cellSize;
    private int dim;
    // private Drawable selectedBackground, normalBackground;
    private byte[] cellPath = new byte[16];
    private byte pathLength = 0;

    public CellGridLayout (Context context, AttributeSet attrs) {
        super (context, attrs);
        // selectedBackground = getResources().getDrawable(R.drawable.selected_tile_bg);
        // normalBackground = getResources().getDrawable(R.drawable.tile_bg);
        //Log.d(BogWord.TAG, "selectedBackground color=" + selectedBackground);
        //Log.d(BogWord.TAG, "normalBackground color=" + normalBackground);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int size = Math.min(r - l, b - t);
        // fill the grid with random ASCII (uppercase) letters
        int childCount = getChildCount();
        dim = (int) Math.sqrt(childCount);
        cellSize = size / dim;
        for (int i = childCount-1; i >= 0; i--) {
            final TextView cell = getCell(i);
            int row = i / dim;
            int col = i % dim;
            // cell.setPadding(0, cellSize/6, 0, 0);
            cell.layout(col * cellSize + cellSize/6, row * cellSize + cellSize/6,
                    (col+1) * cellSize - cellSize/6, (row+1) * cellSize - cellSize/6);
        }
    }

    public void randomize() {
        for (int i = getChildCount()-1; i >= 0; i--) {
            getCell(i).setText(getRandomChar());
        }
    }

    @Override
    public int width() {
        return dim;
    }

    @Override
    public int height() {
        return dim;
    }

    @Override
    public char get(int row, int col) {
        return getCell(row, col).getText().charAt(0);
    }

    private TextView getCell (int row, int col) {
        return (TextView) getChildAt(row * dim + col);
    }

    private TextView getCell (int idx) {
        return (TextView) getChildAt(idx);
    }

    private CharSequence getRandomChar() {
        // TODO: weight distribution by english letter frequencies
        char [] c = Character.toChars('A' + (int)(Math.random() * 26));
        return new String(c);
    }

    public int getSelectedCellIndex(MotionEvent event) {
        int row = (int) event.getY() / cellSize;
        int col = (int) event.getX() / cellSize;
        double dr = (event.getY() - (row + 0.5) * cellSize);
        double dc = (event.getX() - (col + 0.5) * cellSize);
        if (dr*dr + dc*dc > cellSize*cellSize/4) {
            // the "hot" area is the circle of radius cellSize/2 inscribed within each cell
            return -1;
        }
        return row * dim + col;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int cellIndex = getSelectedCellIndex(event);
        if (cellIndex < 0) {
            return false;
        }
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initPath (cellIndex);
                return true;

            case MotionEvent.ACTION_MOVE:
                addPath(cellIndex);
                return true;
        }
        return false;
    }

    private void initPath(int cellIndex) {
        cellPath[0] = (byte) cellIndex;
        pathLength = 1;
        clearSelection();
        selectCell(cellIndex);
    }

    private void addPath(int cellIndex) {
        for (int i = pathLength-1; i >= 0; i--) {
            // this cell is already selected
            if (cellPath[i] == cellIndex) {
                // TODO: if i == pathLength - 2, then un-select the last cell
                return;
            }
        }
        cellPath[pathLength++] = (byte) cellIndex;
        selectCell(cellIndex);
    }

    public String finishPath() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < pathLength; i++) {
            buf.append(getCellText(cellPath[i]));
        }
        return buf.toString();
    }

    public void clearSelection () {
        for (int i = getChildCount() - 1; i >= 0; i--) {

            getChildAt(i).setBackgroundResource(R.drawable.tile_bg);
        }
    }

    public void selectCell (int cellIndex) {
        View child = getChildAt(cellIndex);
        if (child != null) {
            //child.setPressed(true);
            child.setBackgroundResource(R.drawable.selected_tile_bg);
            // Log.d(BogWord.TAG, "selectCell " + cellIndex);
        } else {
            Log.w(DogWord.TAG, "cell index out of bounds in selectCell: " + cellIndex);
        }
    }

    public void connectCells (int fromCellIndex, int toCellIndex) {
        // TODO: draw a line connecting two cells
    }

    public CharSequence getCellText(int cellIndex) {
        TextView cell = getCell(cellIndex);
        if (cell != null) {
            return cell.getText();
        }
        Log.w(DogWord.TAG, "cell index out of bounds in getCellText: " + cellIndex);
        return "";
    }
}
