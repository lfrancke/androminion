package com.mehtank.androminion.util.compat;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

// http://stackoverflow.com/questions/10082163/actionbarsherlock-tabs-multi-fragments
public class TabsAdapter extends FragmentPagerAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener {

  private final Context mContext;
  private final ActionBar mActionBar;
  private final ViewPager mViewPager;
  private final List<TabInfo> mTabs = new ArrayList<>();

  public TabsAdapter(AppCompatActivity activity, ViewPager pager) {
    super(activity.getSupportFragmentManager());
    mContext = activity;
    mActionBar = activity.getSupportActionBar();
    mViewPager = pager;
    mViewPager.setAdapter(this);
    mViewPager.setOnPageChangeListener(this);
  }

  public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
    TabInfo info = new TabInfo(clss, args);
    tab.setTag(info);
    tab.setTabListener(this);
    mTabs.add(info);
    mActionBar.addTab(tab);
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return mTabs.size();
  }

  @Override
  public Fragment getItem(int position) {
    TabInfo info = mTabs.get(position);
    return Fragment.instantiate(mContext, info.clss.getName(), info.args);
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
  }

  @Override
  public void onPageSelected(int position) {
    mActionBar.setSelectedNavigationItem(position);
  }

  @Override
  public void onPageScrollStateChanged(int state) {
  }

  @Override
  public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
    Object tag = tab.getTag();
    for (int i = 0; i < mTabs.size(); i++) {
      if (mTabs.get(i) == tag) {
        mViewPager.setCurrentItem(i);
      }
    }
  }

  @Override
  public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
  }

  @Override
  public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
  }

  static final class TabInfo {

    private final Class<?> clss;
    private final Bundle args;

    TabInfo(Class<?> _class, Bundle _args) {
      clss = _class;
      args = _args;
    }
  }
}
