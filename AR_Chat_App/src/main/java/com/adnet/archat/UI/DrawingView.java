package com.adnet.archat.UI;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.jar.Attributes;

/**
 * Created by TanZhegui on 9/11/2016.
 */
public class DrawingView extends View {

    public int width;
    public  int height;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    Context context;
    private Paint circlePaint;
    private Path circlePath;
    private Paint mPaint;

    private ArrayList<Point> pointArray = new ArrayList<Point>();
    private Point leftTop;
    private Point rightBottom;
    private int drawColor = Color.GREEN;

    public boolean isDrawingPossible = false;

    public DrawingListener listener;

    public interface DrawingListener{
        public void drawingEnd(ArrayList<Point> ptArray, Point leftTop, Point rightBottom, int color);
    }

    public DrawingView(Context c) {
        super(c);
        context=c;
    }

    public DrawingView(Context c, AttributeSet attributes) {
        super(c, attributes);
        context=c;
    }

    public void setDrawingColor(int color){
        drawColor = color;
        mPaint.setColor(drawColor);
    }

    public void initVariable(DrawingListener listener){
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(drawColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        circlePaint = new Paint();
        circlePath = new Path();
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(Color.BLUE);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeJoin(Paint.Join.MITER);
        circlePaint.setStrokeWidth(4f);
        this.listener = listener;
        isDrawingPossible = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(isDrawingPossible){
            canvas.drawBitmap( mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath( mPath,  mPaint);
            canvas.drawPath( circlePath,  circlePaint);
        }
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void clearDrawing(){
        mBitmap = null;
        mPath = null;
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mPath = new Path();
    }
    private void touch_start(float x, float y) {
        clearDrawing();
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
        pointArray = new ArrayList<Point>();
        pointArray.add(new Point(x, y));
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
            pointArray.add(new Point(mX, mY));

            circlePath.reset();
            circlePath.addCircle(mX, mY, 30, Path.Direction.CW);
        }
    }

    private void touch_up() {
        mPath.lineTo(mX, mY);
        pointArray.add(new Point(mX, mY));
        circlePath.reset();
        // commit the path to our offscreen
        mCanvas.drawPath(mPath,  mPaint);
        // kill this so we don't double draw
        mPath.reset();
        double left = pointArray.get(0).x, top = pointArray.get(0).y,
                right = pointArray.get(pointArray.size() - 1).x, bottom = pointArray.get(pointArray.size() - 1).y;
        for(Point pt : pointArray){
            if(left > pt.x) left = pt.x;
            if(top > pt.y) top = pt.y;
            if(right < pt.x) right = pt.x;
            if(bottom < pt.y) bottom = pt.y;
        }
        leftTop = new Point(left, top);
        rightBottom = new Point(right, bottom);
        listener.drawingEnd(pointArray, leftTop, rightBottom, drawColor);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }
}