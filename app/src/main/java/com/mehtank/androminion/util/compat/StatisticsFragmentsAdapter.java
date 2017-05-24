package com.mehtank.androminion.util.compat;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;
import com.mehtank.androminion.R;
import com.mehtank.androminion.fragments.AboutFragment;
import com.mehtank.androminion.fragments.AchievementsFragment;
import com.mehtank.androminion.fragments.ConnectionsFragment;
import com.mehtank.androminion.fragments.CreditsFragment;
import com.mehtank.androminion.fragments.WhatsnewFragment;
import com.mehtank.androminion.fragments.WinlossFragment;

public class StatisticsFragmentsAdapter extends FragmentPagerAdapter {

  private final Context context;

  public StatisticsFragmentsAdapter(FragmentManager fm, Context context) {
    super(fm);
    this.context = context;
  }

  @Override
  public Fragment getItem(int position) {
    switch(position) {
      case 0:
        return new WinlossFragment();
      case 1:
        return new AchievementsFragment();
    }
    return null;
  }

  @Override
  public int getCount() {
    return 2;
  }

  @Override
  public CharSequence getPageTitle(int position) {
    switch (position) {
      case 0:
        return context.getResources().getString(R.string.win_loss_menu);
      case 1:
        return context.getResources().getString(R.string.achievements_menu);
      default:
        return "";
    }
  }
  
}
