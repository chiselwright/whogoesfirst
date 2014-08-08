package net.chizography.droid.whogoesfirst;

import java.util.HashSet;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
public class CirclesDrawingView extends View {
    private static final String TAG = "CirclesDrawingView";

    /** Main bitmap */
    private Bitmap mBitmap = null;

    private Rect mMeasuredRect;

    /** Stores data about single circle */
    private static class CircleArea {
        int radius;
        int centerX;
        int centerY;
		boolean needs_wiping = false;

        CircleArea(int centerX, int centerY, int radius) {
            this.radius = radius;
            this.centerX = centerX;
            this.centerY = centerY;
        }

        @Override
        public String toString() {
            return "Circle[" + centerX + ", " + centerY + ", " + radius + "]";
        }
    }

    /** Paint to draw circles */
    private Paint mCirclePaint, mErasePaint;
    private static final int CIRCLES_LIMIT = 8;

    /** All available circles */
    private HashSet<CircleArea> mCircles = new HashSet<CircleArea>(CIRCLES_LIMIT);
    private SparseArray<CircleArea> mCirclePointer = new SparseArray<CircleArea>(CIRCLES_LIMIT);
    
    //private CountDownTimer countdownTimer;

    /**
     * Default constructor
     *
     * @param ct {@link android.content.Context}
     */
    public CirclesDrawingView(final Context ct) {
        super(ct);

        init(ct);
    }

    public CirclesDrawingView(final Context ct, final AttributeSet attrs) {
        super(ct, attrs);

        init(ct);
    }

    public CirclesDrawingView(final Context ct, final AttributeSet attrs, final int defStyle) {
        super(ct, attrs, defStyle);

        init(ct);
    }

    private void init(final Context ct) {
        // Generate bitmap used for background
        mBitmap = BitmapFactory.decodeResource(ct.getResources(), R.drawable.felt_01);

        mCirclePaint = new Paint();
        mCirclePaint.setColor(Color.MAGENTA);
        mCirclePaint.setStrokeWidth(40);
        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));
        
		mErasePaint = new Paint(mCirclePaint);
		mErasePaint.setColor(Color.WHITE);
		
        // prepare for 'touch, timer, show 'winner'
        // via:        
        this.setOnTouchListener(new OnTouchListener() {
        	public boolean onTouch(View arg0, MotionEvent evt) {/*
        		this.countdownTimer = new CountDownTimer(100000, 1000) {
                    public void onTick(long millisUntilFinished) {
            			Log.d("CHIZBUG", "Seconds remaining: " + millisUntilFinished / 1000);
                	}

                    public void onFinish() {
                    	Toast.makeText(ct, "Finished", Toast.LENGTH_SHORT).show();
                    }
                }.start();*/
                return false;
        	}
        });
    }

    @Override
    public void onDraw(final Canvas canv) {
        // background bitmap to cover all area
        canv.drawBitmap(mBitmap, null, mMeasuredRect, null);

        for (CircleArea circle : mCircles) {
			Paint p;
			if (circle.needs_wiping) {
				p = mErasePaint;
			}
			else {
				p = mCirclePaint;
			}
     	    canv.drawCircle(circle.centerX, circle.centerY, circle.radius, p);
			
			if (circle.needs_wiping){
				// make sure we don't try to delete ourself multiple times
				// if there is a backlog of events
				if(mCircles.contains(circle)){
					int idx = mCirclePointer.indexOfValue(circle);
					//mCircles.remove(circle);
					//mCirclePointer.delete(idx);
				}
			}
        }
    }

	public CircleArea scanForTouchedCircle(final MotionEvent event){
		int actionIndex;
		CircleArea touchedCircle=null;
		int xTouch,yTouch,pointerId;
		final int pointerCount = event.getPointerCount();
		
		for (actionIndex = 0; actionIndex < pointerCount; actionIndex++) {
			// Some pointer has moved, search it by pointer id
			pointerId = event.getPointerId(actionIndex);

			xTouch = (int) event.getX(actionIndex);
			yTouch = (int) event.getY(actionIndex);

			touchedCircle = mCirclePointer.get(pointerId);

			if (null != touchedCircle) {
				touchedCircle.centerX = xTouch;
				touchedCircle.centerY = yTouch;
			}
		}
		return touchedCircle;
	}

	
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        boolean handled = false;

        CircleArea touchedCircle;
        int xTouch;
        int yTouch;
        int pointerId;
        int actionIndex = event.getActionIndex();

        // get touch event coordinates and make transparent circle from it
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // it's the first pointer, so clear all existing pointers data
                clearCirclePointer();

                xTouch = (int) event.getX(0);
                yTouch = (int) event.getY(0);

                // check if we've touched inside some circle
                touchedCircle = obtainTouchedCircle(xTouch, yTouch);
                touchedCircle.centerX = xTouch;
                touchedCircle.centerY = yTouch;
                mCirclePointer.put(event.getPointerId(0), touchedCircle);

                invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                Log.w(TAG, "Pointer down");
                // It secondary pointers, so obtain their ids and check circles
                pointerId = event.getPointerId(actionIndex);

                xTouch = (int) event.getX(actionIndex);
                yTouch = (int) event.getY(actionIndex);

                // check if we've touched inside some circle
                touchedCircle = obtainTouchedCircle(xTouch, yTouch);

                mCirclePointer.put(pointerId, touchedCircle);
                touchedCircle.centerX = xTouch;
                touchedCircle.centerY = yTouch;
                invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_MOVE:
				touchedCircle = scanForTouchedCircle(event);
                invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_UP:
				// the finger that started the "gesture"
				touchedCircle = scanForTouchedCircle(event);
				touchedCircle.needs_wiping=true;
				if(mCirclePointer.size()==1){
					clearCirclePointer();
				}
				else{
					Toast.makeText(this.getContext(),"non final Up",Toast.LENGTH_SHORT).show();
				}
                //clearCirclePointer();
                invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_POINTER_UP:
				// one of the "other" fingers
                pointerId = event.getPointerId(actionIndex);
				mCirclePointer.get(pointerId).needs_wiping=true;
                mCirclePointer.remove(pointerId);
				invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_CANCEL:
                handled = true;
                break;

            default:
                // do nothing
                break;
        }

        return super.onTouchEvent(event) || handled;
    }

    /**
     * Clears all CircleArea - pointer id relations
     */
    private void clearCirclePointer() {
        Log.w(TAG, "clearCirclePointer");
        mCirclePointer.clear();
		mCircles.clear();
    }

    /**
     * Search and creates new (if needed) circle based on touch area
     *
     * @param xTouch int x of touch
     * @param yTouch int y of touch
     *
     * @return obtained {@link CircleArea}
     */
    private CircleArea obtainTouchedCircle(final int xTouch, final int yTouch) {
        CircleArea touchedCircle = getTouchedCircle(xTouch, yTouch);

        if (null == touchedCircle) {
            touchedCircle = new CircleArea(xTouch, yTouch, 120);

            if (mCircles.size() == CIRCLES_LIMIT) {
                Log.w(TAG, "Clear all circles, size is " + mCircles.size());
                // remove first circle
                mCircles.clear();
            }

            Log.w(TAG, "Added circle " + touchedCircle);
            mCircles.add(touchedCircle);
        }

        return touchedCircle;
    }

    /**
     * Determines touched circle
     *
     * @param xTouch int x touch coordinate
     * @param yTouch int y touch coordinate
     *
     * @return {@link CircleArea} touched circle or null if no circle has been touched
     */
    private CircleArea getTouchedCircle(final int xTouch, final int yTouch) {
        CircleArea touched = null;

        for (CircleArea circle : mCircles) {
            if ((circle.centerX - xTouch) * (circle.centerX - xTouch) + (circle.centerY - yTouch) * (circle.centerY - yTouch) <= circle.radius * circle.radius) {
                touched = circle;
                break;
            }
        }

        return touched;
    }

    @SuppressLint("DrawAllocation")
	@Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mMeasuredRect = new Rect(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }
}
