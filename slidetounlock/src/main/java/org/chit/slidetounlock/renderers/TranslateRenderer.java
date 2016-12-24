package org.chit.slidetounlock.renderers;

/*
 * Created by rom on 16.12.16.
 */

import android.animation.ValueAnimator;
import android.graphics.Point;
import android.view.View;

import org.chit.slidetounlock.IRenderer;
import org.chit.slidetounlock.ISlidingData;

public class TranslateRenderer implements IRenderer {

    public static final int DEF_DURATION = 300;

    @Override
    public void renderChanges(ISlidingData slidingData, View child, float percentage, Point transformed) {
        int translateX = transformed.x - slidingData.getStartX();
        int translateY = transformed.y - slidingData.getStartY();
        child.setTranslationX(translateX);
        child.setTranslationY(translateY);
    }

    @Override
    public int onSlideReset(ISlidingData slidingData, View child) {
        child.setTranslationX(0);
        child.setTranslationY(0);
        child.setAlpha(1);
        slidingData.publishOnSlideChanged(0);
        return 0;
    }

    @Override
    public int onSlideDone(ISlidingData slidingData, View child) {
        child.animate()
                .alpha(0)
                .setDuration(DEF_DURATION)
                .start();
        return DEF_DURATION;
    }

    @Override
    public int onSlideCancelled(final ISlidingData slidingData, final View child, final float lastPercentage) {
        ValueAnimator animator = new ValueAnimator();
        animator.setDuration(DEF_DURATION);
        animator.setFloatValues(1, 0);
        final float startX = child.getTranslationX();
        final float startY = child.getTranslationY();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = 1 - valueAnimator.getAnimatedFraction();
                child.setTranslationX(startX * val);
                child.setTranslationY(startY * val);
                slidingData.publishOnSlideChanged(val * lastPercentage);
            }
        });
        animator.start();
        return DEF_DURATION;
    }
}
