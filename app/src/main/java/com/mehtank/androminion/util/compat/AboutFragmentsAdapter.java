package com.mehtank.androminion.util.compat;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;
import com.mehtank.androminion.R;
import com.mehtank.androminion.fragments.AboutFragment;
import com.mehtank.androminion.fragments.ConnectionsFragment;
import com.mehtank.androminion.fragments.CreditsFragment;
import com.mehtank.androminion.fragments.WhatsnewFragment;

public class AboutFragmentsAdapter extends FragmentPagerAdapter {

  private final Context context;
  
  public AboutFragmentsAdapter(FragmentManager fm, Context context) {
    super(fm);
    this.context = context;
  }

  @Override
  public Fragment getItem(int position) {
    switch(position) {
      case 0:
        return new AboutFragment();
      case 1:
        return new ConnectionsFragment();
      case 2:
        return new WhatsnewFragment();
      case 3:
        return new CreditsFragment();

    }
    return null;
  }

  @Override
  public int getCount() {
    return 4;
  }

  @Override
  public CharSequence getPageTitle(int position) {
    switch (position) {
      case 0:
        return context.getResources().getString(R.string.about_menu);
      case 1:
        return context.getResources().getString(R.string.connections_menu);
      case 2:
        return context.getResources().getString(R.string.whatsnew_menu);
      case 3:
        return context.getResources().getString(R.string.contrib_menu);
      default:
        return "";
    }
  }
  
}
