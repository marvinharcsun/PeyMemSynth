package com.group3.synthesizerapp.envelope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by marvin on 10/6/16.
 */



public class EnvelopeGraph extends View {

    //points for first Three Circles

    private  Paint mPaint;
    private Rect mRect;
    private RectF mRectF;

    private RectF mInnerRectF;

    private float mScale;


    private float mXa, mYa, mSa, mXd, mSd, mYs, mXr, mSr;


    float w,h = 0.f;
    private float mXAtTouch;
    private float mYAtTouch;


    private Path path;

    float [] points;

    boolean aXYSelected;
    boolean dXYSelected;
    boolean rXSelected;

    boolean aSXYSelected;
    boolean dSXYSelected;
    boolean rSXYSelected;

    private EnvelopeGraphListener listener;

    private int mSampleRate;
    float [] shapeValues = {.01f, .02f, .03f, .04f, .05f, .06f, .07f, .08f, .09f,
                        .10f, .11f, .12f, .13f, .14f, .15f, .16f, .17f, .18f, .19f,
                        .20f, .21f, .22f, .23f, .24f, .25f, .26f, .27f, .28f, .29f,
                        .30f, .31f, .32f, .33f, .34f, .35f, .36f, .37f, .38f, .39f,
                        .40f, .41f, .42f, .43f, .44f, .45f, .46f, .47f, .48f, .49f,
                        .50f, .51f, .52f, .53f, .54f, .55f, .56f, .57f, .58f, .59f,
                        .60f, .61f, .62f, .63f, .64f, .65f, .66f, .67f, .68f, .69f,
                        .70f, .71f, .72f, .73f, .74f, .75f, .76f, .77f, .78f, .79f,
                        .80f, .81f, .82f, .83f, .84f, .85f, .86f, .87f, .88f, .89f,
                        .90f, .91f, .92f, .93f, .94f, .95f, .96f, .97f, .98f, .99f,
                        1.0f, 2.0f, 3.0f , 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f,
                        11.0f, 12.0f, 13.0f , 14.0f, 15.0f, 16.0f, 17.0f, 18.0f, 19.0f, 20.0f,
                        21.0f, 22.0f, 23.0f , 24.0f, 25.0f, 26.0f, 27.0f, 28.0f, 29.0f, 30.0f,
                        31.0f, 32.0f, 33.0f , 34.0f, 35.0f, 36.0f, 37.0f, 38.0f, 39.0f, 40.0f,
                        41.0f, 42.0f, 43.0f , 44.0f, 45.0f, 46.0f, 47.0f, 48.0f, 49.0f, 50.0f,
                        51.0f, 52.0f, 53.0f , 54.0f, 55.0f, 56.0f, 57.0f, 58.0f, 59.0f, 60.0f,
                        61.0f, 62.0f, 63.0f , 64.0f, 65.0f, 66.0f, 67.0f, 68.0f, 69.0f, 70.0f,
                        71.0f, 72.0f, 73.0f , 74.0f, 75.0f, 76.0f, 77.0f, 78.0f, 79.0f, 80.0f,
                        81.0f, 82.0f, 83.0f , 84.0f, 85.0f, 86.0f, 87.0f, 88.0f, 89.0f, 90.0f,
                        91.0f, 92.0f, 93.0f , 94.0f, 95.0f, 96.0f, 97.0f, 98.0f, 99.0f, 100.0f};

    public EnvelopeGraph(Context context, AttributeSet attrs) {
        super(context, attrs);




        aXYSelected = false;
        aXYSelected = false;
        dXYSelected = false;
        rXSelected = false;
        aSXYSelected = false;
        dSXYSelected = false;
        rSXYSelected = false;

        path = new Path();
        mSampleRate = 16000;

        setWillNotDraw(false);

        init();

    }

    public void setEnvelopeValues(EnvelopeGraphListener.Segment segment, float value)
    {

        switch (segment)
        {
            case kAttackX:
                mXa = value*getMeasuredWidth();
                break;
            case kAttackY:
                mYa = value*getMeasuredHeight();
                break;
            case kAttackShape:
                mSa = value;
                break;
            case kDecayX:
                mXd = value*getMeasuredWidth();
                break;
            case kDecayShape:
                mSd = value;
                break;
            case kSustainY:
                mYs = value*getMeasuredHeight();
                break;
            case kReleaseX:
                mXr = value*getMeasuredWidth();
                break;
            case kReleaseShape:
                mSr = value;
                break;
        }
        invalidate();

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

        mScale = getMeasuredWidth() * .01f;

        mPaint.setStrokeWidth(mScale);

        canvas.translate(0,mRectF.height());
        canvas.scale(1,-1);

        if(points == null)
        {
            points = new float[(int)getMeasuredWidth()*8];
        }

        float Y1 = 0.0f;
        float Y2 = 0.0f;
        int counter = 0;
        for(float i = 0; i < mXa ; i+=mScale) {
            Y1 = (float) Math.pow(i / mXa, mSa) * mYa;
            Y2 = (float) Math.pow((i + mScale) / mXa, mSa) * mYa;

            if(Y1 >= mYa)
            {
                Y1 = mYa;
            }
            if(Y2 >= mYa)
            {
                Y2 = mYa;
            }
            points[counter] = i;
            points[counter+1] = Y1;
            points[counter+2] = i+mScale;
            points[counter+3] = Y2;
            //canvas.drawLine(i,Y1,i+mIncrement,Y2,mPaint);
            counter+=4;
        }

        canvas.drawRect(mRectF,mPaint);
        canvas.drawCircle(mXa,mYa,mScale*2,mPaint);

        for(float i = 0; i < mXd ; i += mScale)
        {
            if(mYa <= mYs)
            {
                Y1 = mYa + (float)Math.pow(i/mXd,mSd)*(mYs-mYa);
                Y2 = mYa + (float)Math.pow((i+mScale)/mXd,mSd)*(mYs-mYa);


                if(Y1 >= mYs)
                {
                    Y1 = mYs;
                }
                if(Y2 >= mYs)
                {
                    Y2 = mYs;
                }
            }
            else if(mYs < mYa)
            {
                Y1 = mYa - (float)Math.pow(i/mXd,mSd)*(mYa-mYs);
                Y2 = mYa - (float)Math.pow((i+mScale)/mXd,mSd)*(mYa-mYs);

                if(Y1 <= mYs)
                {
                    Y1 = mYs;
                }
                if(Y2 <= mYs)
                {
                    Y2 = mYs;
                }
            }
            points[counter] = i+mXa;
            points[counter+1] = Y1;
            points[counter+2] = i+mScale+mXa;
            points[counter+3] = Y2;
            counter+=4;

        }

        canvas.drawCircle(mXa+mXd,mYs,mScale*2,mPaint);

        for(float i = 0; i < mXr ; i += mScale)
        {

            Y1 = mYs - (float)Math.pow(i/mXr,mSr)*mYs;
            Y2 = mYs - (float)Math.pow((i+mScale)/mXr,mSr)*mYs;

            if(Y1 <= 0.0f)
            {
                Y1 = 0.0f;
            }
            if(Y2 <= 0.0f)
            {
                Y2 = 0.0f;
            }

            points[counter] = i+mXa+mXd;
            points[counter+1] = Y1;
            points[counter+2] = i+mXa+mXd+mScale;
            points[counter+3] = Y2;
            counter+=4;

        }
        canvas.drawCircle(mXa+mXd+mXr,0,mScale*2,mPaint);
        canvas.drawLines(points,0,counter,mPaint);
    }

    public void setSampleRate(int sampleRate)
    {
        mSampleRate = sampleRate;
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float valueX = 0.0f;
        float valueY = 0.0f;
        mScale = getMeasuredWidth() * .01f;
        float epsilon = mScale*16;
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                // Just record the current finger position.
                mXAtTouch = event.getX();
                mYAtTouch = event.getY();

                if((valueX = Math.abs(mXAtTouch-mXa)) < epsilon && (valueY = Math.abs(mYAtTouch-(mRectF.height()-mYa)))<epsilon)
                {
                    aXYSelected = true;
                }
                else if((valueX = Math.abs(mXAtTouch-mXa-mXd)) < epsilon && (valueY = Math.abs(mYAtTouch-(mRectF.height()-mYs)))<epsilon)
                {
                    dXYSelected = true;
                }
                else if((valueX = Math.abs(mXAtTouch-mXa-mXd-mXr)) < epsilon)
                {
                    rXSelected = true;
                }

                else if((valueX = Math.abs(mXAtTouch-(mXa/2)))<epsilon)
                {
                    aSXYSelected = true;
                }
                else if((valueX = Math.abs(mXAtTouch-mXa-(mXd/2)))<epsilon)
                {
                    dSXYSelected = true;
                }

                else if((valueX = Math.abs(mXAtTouch-mXa-mXd-(mXr/2)))<epsilon)
                {
                    rSXYSelected = true;
                }


                getDrawingRect(mRect);
                break;
            }

            case MotionEvent.ACTION_MOVE: {


                mXAtTouch = event.getX();
                mYAtTouch = event.getY();

                if(aXYSelected)
                {
                    mXa = mXAtTouch - epsilon;
                    mYa = mRectF.height() - mYAtTouch ;
                    if(mXa < 0)
                    {
                        mXa = 0;
                    }

                    if (mXa > mRectF.width()-mXd-mXr)
                    {
                        mXa = mRectF.width()-mXd-mXr;
                    }
                    if(mYa > mRectF.height())
                    {
                        mYa = mRectF.height();
                    }
                    else if (mYa < 0)
                    {
                        mYa = 0;
                    }
                    if (listener != null) {
                        listener.OnEnvelopeGraphChanged(this, EnvelopeGraphListener.Segment.kAttackY, (mYa/mRectF.height()));
                    }
                    if (listener != null) {
                        listener.OnEnvelopeGraphChanged(this, EnvelopeGraphListener.Segment.kAttackX, (mXa/mRectF.width()));
                    }
                }
                else if(dXYSelected)
                {
                    mXd = mXAtTouch - epsilon;
                    mYs = mRectF.height() - mYAtTouch;

                    if(mXd < 0)
                    {
                        mXd = 0;
                    }

                    if (mXd > mRectF.width()-mXa-mXr)
                    {
                        mXd = mRectF.width()-mXa-mXr;
                    }

                    if(mYs > mRectF.height())
                    {
                        mYs = mRectF.height();
                    }
                    else if (mYs < 0 )
                    {
                        mYs = 0;
                    }
                    if (listener != null) {
                        listener.OnEnvelopeGraphChanged(this, EnvelopeGraphListener.Segment.kDecayX, (mXd/mRectF.width()));
                    }
                    if (listener != null) {
                        listener.OnEnvelopeGraphChanged(this, EnvelopeGraphListener.Segment.kSustainY, (mYs/mRectF.height()));
                    }

                }
                else if(rXSelected) {
                    mXr = mXAtTouch-epsilon;
                    if (mXr < 0)
                    {
                        mXr = 0;
                    }
                    if (mXr > mRectF.width()-mXa-mXd)
                    {
                        mXr = mRectF.width()-mXa-mXd;
                    }
                    if (listener != null) {
                        listener.OnEnvelopeGraphChanged(this, EnvelopeGraphListener.Segment.kReleaseX, (mXr/mRectF.width()));
                    }
                }
                else if(aSXYSelected) {
                    mSa = mRectF.height() - mYAtTouch - epsilon;

                    int shape = (int)Math.abs(Math.max(0,(mRectF.height()-mYAtTouch)));
                    if(shape>198) {
                        shape = 198;
                    }

                    mSa = shapeValues[shape];

                    if (listener != null) {
                        listener.OnEnvelopeGraphChanged(this, EnvelopeGraphListener.Segment.kAttackShape, mSa );
                    }

                }

                else if(dSXYSelected) {
                    mSd = mRectF.height() - mYAtTouch - epsilon;

                    int shape = (int)Math.abs(Math.max(0,(mRectF.height()-mYAtTouch)));
                    if(shape>198) {
                        shape = 198;
                    }

                    mSd = shapeValues[shape];

                    if (listener != null) {
                        listener.OnEnvelopeGraphChanged(this, EnvelopeGraphListener.Segment.kDecayShape, mSd );
                    }


                }
                else if(rSXYSelected) {
                    mSr = mRectF.height() - mYAtTouch - epsilon;

                    int shape = (int)Math.abs(Math.max(0,(mRectF.height()-mYAtTouch)));
                    if(shape>198) {
                        shape = 198;
                    }

                    mSr = shapeValues[shape];

                    if (listener != null) {
                        listener.OnEnvelopeGraphChanged(this, EnvelopeGraphListener.Segment.kReleaseShape, mSr );
                    }

                }





                invalidate();

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                aXYSelected = false;
                dXYSelected = false;
                rXSelected = false;
                aSXYSelected = false;
                dSXYSelected = false;
                rSXYSelected = false;

            /*{
                if (listenerUp_ != null) {
                    listenerUp_.onKnobChanged(this,getValue());
                }
                break;
            }
            */
                break;
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    public void setEnvelopeGraphListener(EnvelopeGraphListener l)
    {
        listener = l;
    }

    private void init() {


        mRectF = new RectF();
        mRect = new Rect();
        mInnerRectF =new RectF();
        mScale = getMeasuredWidth() * .01f;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setDither(true);
        mPaint.setColor(0xFFFF4081);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);


    }
}
