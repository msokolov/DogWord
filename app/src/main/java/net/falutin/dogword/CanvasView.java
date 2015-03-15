package net.falutin.dogword;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom view for drawing lines
 */
public class CanvasView extends View {

    // the amount to fudge wide line endings so there's no gap where they join
    private static final float CAP_EPS = 0.013f;

    private Paint paint;
    private int dim;
    private int cellSize;
    private byte [] cellPath;

    private final static String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    public CanvasView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        paint = new Paint();
        final int gestureColorRes = attributeSet.getAttributeResourceValue(ANDROID_NS, "gestureColor", android.R.color.white);
        paint.setColor(getResources().getColor(gestureColorRes));
        float gestureStrokeWidth = attributeSet.getAttributeFloatValue(ANDROID_NS, "gestureStrokeWidth", 2f);
        paint.setStrokeWidth(gestureStrokeWidth);
        // sadly this won't work https://code.google.com/p/android/issues/detail?id=24873
        // but maybe someday?
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setDimensions (int dim, int cellSize, byte[] cellPath) {
        this.dim = dim;
        this.cellSize = cellSize;
        this.cellPath = cellPath;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (cellPath == null || dim == 0) {
            // not initialized yet?
            return;
        }
        for (int i = 0; i < cellPath.length - 1; i++) {
            int iCell = cellPath[i], iCellNext = cellPath[i+1];
            if (iCell < 0 || iCellNext < 0) {
                break;
            }
            int dh = (iCellNext % dim) - (iCell % dim);
            float eps_horz = dh == 0 ? 0 : (dh > 0 ? CAP_EPS : -CAP_EPS);
            int dv = (iCellNext / dim) - (iCell / dim);
            float eps_vert = dv == 0 ? 0 : (dv > 0 ? CAP_EPS : -CAP_EPS);
            canvas.drawLine(
                    (iCell % dim + 0.3f - eps_horz) * cellSize,
                    (iCell / dim + 0.3f - eps_vert) * cellSize,
                    (iCellNext % dim + 0.3f + eps_horz) * cellSize,
                    (iCellNext / dim + 0.3f + eps_vert) * cellSize,
                    paint);
        }
    }

}
