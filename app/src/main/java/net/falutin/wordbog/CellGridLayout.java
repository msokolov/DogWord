package net.falutin.wordbog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
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
public class CellGridLayout extends RelativeLayout {

    private int cellSize = 0;
    private int dim;
    private CellGrid grid;
    private CanvasView canvasView;
    private byte[] cellPath = new byte[16];
    private byte pathLength = 0;

    public CellGridLayout (Context context, AttributeSet attrs) {
        super (context, attrs);
        for (int i = 0; i < cellPath.length; i++) {
            cellPath[i] = -1;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int size = Math.min(r - l, b - t);
        // The first N^2 children are the grid cells
        int childCount = getChildCount();
        dim = (int) Math.sqrt(childCount);
        cellSize = size / dim;
        for (int i = dim * dim - 1; i >= 0; i--) {
            final TextView cell = getCell(i);
            int row = i / dim;
            int col = i % dim;
            // cell.setPadding(0, cellSize/6, 0, 0);
            cell.layout(col * cellSize + cellSize/6, row * cellSize + cellSize/6,
                    (col+1) * cellSize - cellSize/6, (row+1) * cellSize - cellSize/6);
        }
        setGrid(grid); // copy the letters into the text cells
        canvasView.layout(l, t, r, b);
        canvasView.setDimensions(dim, cellSize, cellPath);
    }

    public void setCanvasView(CanvasView canvasView) {
        this.canvasView = canvasView;
    }

    private TextView getCell (int idx) {
        return (TextView) getChildAt(idx);
    }

    public int getSelectedCellIndex(MotionEvent event) {
        int row = (int) event.getY() / cellSize;
        int col = (int) event.getX() / cellSize;
        double dr = (event.getY() - (row + 0.5) * cellSize);
        double dc = (event.getX() - (col + 0.5) * cellSize);
        if (dr*dr + dc*dc > cellSize*cellSize/9) {
            // the "hot" area is the circle of radius cellSize/3 inscribed within each cell
            return -1;
        }
        return row * dim + col;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int cellIndex = getSelectedCellIndex(event);
        if (cellIndex < 0 || cellIndex >= dim*dim) {
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
        cellPath[1] = -1;
        pathLength = 1;
        clearSelection();
        selectCell(cellIndex);
    }

    private void addPath(int cellIndex) {
        assert(cellIndex < dim*dim);
        for (int i = pathLength-1; i >= 0; i--) {
            // this cell is already selected
            if (cellPath[i] == cellIndex) {
                // TODO: if i == pathLength - 2, then un-select the last cell?
                // not if we want implement Boggle-style rules
                return;
            }
        }
        cellPath[pathLength++] = (byte) cellIndex;
        cellPath[pathLength] = -1;
        selectCell(cellIndex);
        canvasView.invalidate(); // TODO: limit area
    }

    public String finishPath() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < pathLength; i++) {
            buf.append(grid.get(cellPath[i]));
        }
        pathLength = 0;
        cellPath[0] = -1;
        canvasView.invalidate(); // TODO: limit area
        return buf.toString();
    }

    public void clearSelection () {
        for (int i = dim * dim - 1; i >= 0; i--) {
            getChildAt(i).setBackgroundResource(R.drawable.tile_bg);
        }
    }

    public void selectCell (int cellIndex) {
        View child = getChildAt(cellIndex);
        if (child != null) {
            child.setBackgroundResource(R.drawable.selected_tile_bg);
        } else {
            Log.w(DogWord.TAG, "cell index out of bounds in selectCell: " + cellIndex);
        }
    }

    public CellGrid getGrid() {
        return grid;
    }

    public void setGrid(CellGrid grid) {
        this.grid = grid;
        if (cellSize == 0) {
            // onLayout not yet called
            return;
        }
        for (int row = 0; row < grid.height(); row++) {
            for (int col = 0; col < grid.width(); col++) {
                TextView cell = getCell(row * grid.width() + col);
                cell.setText(new String(new char[]{grid.get(row, col)}));
            }
        }
    }

}
