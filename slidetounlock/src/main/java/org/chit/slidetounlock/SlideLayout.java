package org.chit.slidetounlock;

/*
 * Created by rom on 14.12.16.
 */

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.IdRes;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.LinkedHashSet;
import java.util.Set;

public class SlideLayout
        extends FrameLayout
        implements
            View.OnTouchListener,
            ISlidingData {

    public static final String TAG = SlideLayout.class.getSimpleName();

    private float mThreshold = 1.0f;

    private Set<ISlideChangeListener> mChangeListeners = new LinkedHashSet<>();
    private Set<ISlideListener> mSlideListeners = new LinkedHashSet<>();

    private boolean mStarted;

    private int mStartX;
    private int mStartY;

    private ISlider mSlider;
    private IRenderer mRenderer;

    private Dimen mParentStartDimen = new Dimen();
    private Rect mChildStartRect = new Rect();

    @IdRes
    private int mChildId;
    private View mChild;

    private long mLockEventsTill = 0;

    private float mLastPercentage;

    // --- init

    public SlideLayout(Context context) {
        super(context);
        constructInit();
    }

    public SlideLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        constructInit();
    }

    public SlideLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        constructInit();
    }

    public void reset() {
        long diff = mLockEventsTill - System.currentTimeMillis();
        if (diff < 0) {
            doReset();
        }
        else {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    doReset();
                }
            }, diff + 500);
        }
    }

    private void doReset() {
        mLockEventsTill = System.currentTimeMillis() + mRenderer.onSlideReset(this, getChild());
    }

    private void constructInit() {

    }
    // --- lifecycle

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setOnTouchListener(this);
        if (mChildId == 0 && getChildCount() > 0) {
            mChild = getChildAt(0);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setOnTouchListener(null);
    }

    // --- getters/setters

    public void setChildId(@IdRes int id) {
        mChildId = id;
        mChild = null;
    }

    public void setThreshold(float threshold) {
        mThreshold = threshold;
    }

    public View getChild() {
        if (null == mChild) {
            mChild = findViewById(mChildId);
        }
        return mChild;
    }

    public void setSlider(ISlider slider) {
        mSlider = slider;
    }

    public void setRenderer(IRenderer renderer) {
        mRenderer = renderer;
    }

    // --- listeners

    public void addSlideListener(ISlideListener listener) {
        mSlideListeners.add(listener);
    }

    public void removeSlideListener(ISlideListener listener) {
        mSlideListeners.remove(listener);
    }

    public void addSlideChangeListener(ISlideChangeListener listener) {
        mChangeListeners.add(listener);
    }

    public void removeSlideChangeListener(ISlideChangeListener listener) {
        mChangeListeners.remove(listener);
    }

    // --- ISlidingData impl.

    @Override
    public int getStartX() {
        return mStartX;
    }

    @Override
    public int getStartY() {
        return mStartY;
    }

    @Override
    public Rect getChildStartRect() {
        return mChildStartRect;
    }

    @Override
    public Dimen getParentDimen() {
        return mParentStartDimen;
    }


    // --- OnTouchListener impl.

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (System.currentTimeMillis() < mLockEventsTill) {
            return false;
        }

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mStarted) {
                    return false;
                }
                mStarted = canStart(motionEvent);
                if (mStarted) {
                    publishOnSlideStart();
                }
                return mStarted;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(motionEvent);
                return true;
            case MotionEvent.ACTION_UP:
                handleFinishing(false);
                break;
            case MotionEvent.ACTION_CANCEL:
                handleFinishing(false);
                break;
        }
        return false;
    }

    private boolean canStart(MotionEvent motionEvent) {

        mStartX = (int) motionEvent.getX();
        mStartY = (int) motionEvent.getY();

        mParentStartDimen.width = getWidth();
        mParentStartDimen.height = getHeight();

        mChildStartRect.left = getChild().getLeft();
        mChildStartRect.right = getChild().getRight();
        mChildStartRect.top = getChild().getTop();
        mChildStartRect.bottom = getChild().getBottom();

        if (!mSlider.allowStart(this)) {
            return false;
        }

        return true;
    }

    private void handleActionMove(MotionEvent motionEvent) {
        if (!mStarted) {
            return;
        }

        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();

        float percentage = mSlider.getPercentage(this, x, y);
        if (percentage < 0) {
            percentage = 0;
        }
        else if (percentage > 1.0f) {
            percentage = 1.0f;
        }
        mLastPercentage = percentage;

        Point transformedXY = mSlider.getTransformedPosition(this, percentage, x, y);

        mRenderer.renderChanges(this, getChild(), percentage, transformedXY);

        publishOnSlideChanged(percentage);

        if (percentage >= mThreshold) {
            handleFinishing(true);
        }

    }

    private void handleFinishing(boolean done) {
        if (!mStarted) {
            return;
        }
        mStarted = false;
        if (done) {
            mLockEventsTill = System.currentTimeMillis() + mRenderer.onSlideDone(this, getChild());
        }
        else {
            mLockEventsTill = System.currentTimeMillis() + mRenderer.onSlideCancelled(this, getChild(), mLastPercentage);
        }

        publishOnSlideFinished(done);
    }

    private void publishOnSlideStart() {
        for (ISlideChangeListener listener : mChangeListeners) {
            listener.onSlideStart(this);
        }
    }

    @Override
    public void publishOnSlideChanged(float percentage) {
        for (ISlideChangeListener listener : mChangeListeners) {
            listener.onSlideChanged(this, percentage / mThreshold);
        }
    }

    private void publishOnSlideFinished(boolean done) {
        for (ISlideChangeListener listener : mChangeListeners) {
            listener.onSlideFinished(this, done);
        }
        for (ISlideListener listener : mSlideListeners) {
            listener.onSlideDone(this, done);
        }
    }

}
