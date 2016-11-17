package com.group3.synthesizerapp.verticalSeekBar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.group3.synthesizerapp.envelope.EnvelopeGraphListener;

/**
 * Created by Marvin on 11/5/2016.
 */

public class CustomSeekBar extends View {
    //points for first Three Circles

    private Paint mPaint1;
    private Paint mPaint2;
    private Paint mPaint3;
    private Rect mRect;
    private RectF mRectF;
    private RectF mInnerRectF;

    private float valueAtTouch;
    private float xyAtTouch;
    private float sensitivity;

    private boolean mActAsSwitch;

    private CustomSeekBarListener listener;
    private float barValue;

    public CustomSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setActAsSwitch(boolean actAsSwitch)
    {
        mActAsSwitch = actAsSwitch;
    }

    public void setValue(float value)
    {
        barValue = value;
        invalidate();
    }

    public float getValue()
    {
        return barValue;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        getDrawingRect(mRect);
        mRectF.set(mRect);
        // Apply padding.
        mRectF.left += getPaddingLeft();
        mRectF.right -= getPaddingRight();
        mRectF.top += getPaddingTop();
        mRectF.bottom -= getPaddingBottom();
        mInnerRectF.set(mRectF);



        canvas.translate(0,mRectF.height());
        canvas.scale(1,-1);


        float border = mRectF.width()*.25f;
        //canvas.drawRect(mRectF,mPaint1);
        //canvas.drawRect(border,border*.5f,border*3,(mRectF.height()-border)*barValue+border*.5f,mPaint2);
        if(!mActAsSwitch)
        {
            canvas.drawRect(border, border, mRectF.width() - border, mRectF.height() - border, mPaint1);
            canvas.drawRect(border * 1.5f, border * 1.5f, border * 2.5f, (mRectF.height() - border * 3.f) * barValue + border * 1.5f, mPaint2);
        }
        else
        {
            canvas.drawRect(border, border, mRectF.width() - border, mRectF.height() - border, mPaint1);
            if(barValue == 0.0f)
                canvas.drawRect(border * 1.5f, border * 1.5f, border * 2.5f, .5f*(mRectF.height() - border * 3.f)  + border * 1.5f, mPaint3);
            else
                canvas.drawRect(border * 1.5f, .5f*(mRectF.height() - border * 3.f)  + border * 1.5f, border * 2.5f, (mRectF.height() - border * 3.f)  + border * 1.5f, mPaint2);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                // Just record the current finger position.
                if(!mActAsSwitch) {
                    xyAtTouch = event.getX() - event.getY();
                    valueAtTouch = barValue;
                }
                else
                {
                    if(barValue < 1.0f)
                    {
                        barValue = 1.0f;
                    }else if(barValue == 1.0f){
                        barValue = 0.0f;
                    }
                    if (listener != null) {
                        listener.OnCustomSeekBarChanged(this,getValue());
                    }
                    invalidate();
                }
                getDrawingRect(mRect);
                break;
            }

            case MotionEvent.ACTION_MOVE:
            {
                if(!mActAsSwitch) {
                    float xyDelta = event.getX() - event.getY() - xyAtTouch;
                    barValue = valueAtTouch + sensitivity * xyDelta;
                    barValue = Math.min(barValue, 1.0f);
                    barValue = Math.max(barValue, 0.0f);
                }

                // Notify listener and redraw.
                if (listener != null) {
                    listener.OnCustomSeekBarChanged(this,getValue());
                }
                invalidate();
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    public void setListener(CustomSeekBarListener l)
    {
        listener = l;
    }

    private void init() {


        mRectF = new RectF();
        mRect = new Rect();
        mInnerRectF = new RectF();

        mPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint1.setColor(Color.WHITE);

        mPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint2.setColor(0xFFFF4081);

        mPaint3 = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint3.setColor(0xc8181e22);

        barValue = 0.00f;
        float density = getResources().getDisplayMetrics().density;
        sensitivity = .005f / density;  // TODO: should be configurable
        mActAsSwitch = false;


    }
}
