package com.mehtank.androminion.ui;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import com.mehtank.androminion.R;

public class CardAnimator {

  private static final String TAG = "CardAnimator";

  private static final List<AnimationSet> RUNNING_ANIMS = new ArrayList<>();
  private static final List<CardView> CARD_VIEWS = new ArrayList<>();

  private ViewGroup rootView;
  private int left;
  private int top;
  private int height;

  public void init(View anchor) {
    rootView = (ViewGroup) anchor.getRootView();
    int[] location = new int[2];
    anchor.getLocationOnScreen(location);
    left = location[0];
    top = location[1];
    height = anchor.getHeight();
  }

  public void showCard(CardView c, ShowCardType type) {
    AnimationSet anims = new AnimationSet(true);
    anims.setInterpolator(new LinearInterpolator());
    int cardWidth = (int) c.getResources().getDimension(R.dimen.cardWidth);
    TranslateAnimation trans;
    AlphaAnimation alpha = null;
    switch (type) {
      case OBTAINED:
        alpha = new AlphaAnimation(0, 1);
        trans = new TranslateAnimation(
                                        Animation.ABSOLUTE, left,
                                        Animation.ABSOLUTE, left,
                                        Animation.ABSOLUTE, top - height,
                                        Animation.ABSOLUTE, top);
        anims.setInterpolator(new DecelerateInterpolator());
        break;
      case TRASHED:
        alpha = new AlphaAnimation(1, 0);
        trans = new TranslateAnimation(
                                        Animation.ABSOLUTE, left,
                                        Animation.ABSOLUTE, left,
                                        Animation.ABSOLUTE, top,
                                        Animation.ABSOLUTE, top + height);
        anims.setInterpolator(new AccelerateInterpolator());
        break;
      default: //  REVEALED
        // alpha = new AlphaAnimation(1, 0.5f);
        trans = new TranslateAnimation(
                                        Animation.ABSOLUTE, left,
                                        Animation.ABSOLUTE, left + cardWidth,
                                        Animation.ABSOLUTE, top,
                                        Animation.ABSOLUTE, top);
    }
    if (alpha != null) {
      alpha.setDuration(1000);
    }
    trans.setDuration(1000);
    if (alpha != null) {
      anims.addAnimation(alpha);
    }
    anims.addAnimation(trans);
    trans = new TranslateAnimation(0, 0, 0, 0);
    trans.setDuration(2000); //
    anims.addAnimation(trans);

    anims.setAnimationListener(new CVAnimListener(c));

    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
    c.setLayoutParams(lp);
    rootView.addView(c);
    c.startAnimation(anims);

    CARD_VIEWS.add(c);
    RUNNING_ANIMS.add(anims);
  }

  public enum ShowCardType {OBTAINED, TRASHED, REVEALED}

  private class CVAnimListener implements AnimationListener {

    CardView v;

    private CVAnimListener(CardView v) {
      this.v = v;
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
      v.setVisibility(View.GONE);
      RUNNING_ANIMS.remove(animation);
      if (RUNNING_ANIMS.isEmpty()) {
        for (CardView c : CARD_VIEWS) {
          rootView.removeView(c);
        }
        CARD_VIEWS.clear();
      }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }
  }
}
