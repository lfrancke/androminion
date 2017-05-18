package com.mehtank.androminion.activities;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import com.mehtank.androminion.R;
import com.mehtank.androminion.util.ThemeSetter;
import com.mehtank.androminion.util.compat.AboutFragmentsAdapter;

/**
 * This activity just shows four tabs: about, connections, what's new and credits.
 *
 * Rewrite to support actionbar, tabs and swipe gestures (backwards compatible
 * to API7).
 */
public class AboutActivity extends AppCompatActivity {

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        NavUtils.navigateUpFromSameTask(this);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    ThemeSetter.setTheme(this, true);
    ThemeSetter.setLanguage(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ThemeSetter.setTheme(this, true);
    ThemeSetter.setLanguage(this);

    setContentView(R.layout.activity_about);

    ViewPager pager = (ViewPager) findViewById(R.id.about_pager);
    pager.setAdapter(new AboutFragmentsAdapter(getFragmentManager(), this));


    /*
    ActionBar bar = getSupportActionBar();
    bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
    bar.setDisplayHomeAsUpEnabled(true);
    bar.setDisplayShowTitleEnabled(true);
    bar.setTitle(R.string.aboutactivity_title);
    */
    /*
    mTabsAdapter = new TabsAdapter(this, mViewPager);

    // About tab
    ActionBar.Tab aboutTab = bar.newTab().setText(R.string.about_menu)
                               .setIcon(android.R.drawable.ic_menu_info_details);
    mTabsAdapter.addTab(aboutTab, AboutFragment.class, null);

    // connections tab
    ActionBar.Tab connectionsTab = bar.newTab()
                                     .setText(R.string.connections_menu)
                                     .setIcon(android.R.drawable.ic_menu_share);
    mTabsAdapter.addTab(connectionsTab, ConnectionsFragment.class, null);

    // What's New tab
    ActionBar.Tab whatsnewTab = bar.newTab()
                                  .setText(R.string.whatsnew_menu)
                                  .setIcon(android.R.drawable.ic_menu_view);
    mTabsAdapter.addTab(whatsnewTab, WhatsnewFragment.class, null);

    // Credits tab
    ActionBar.Tab creditsTab = bar.newTab().setText(R.string.contrib_menu)
                                 .setIcon(android.R.drawable.ic_menu_my_calendar);
    mTabsAdapter.addTab(creditsTab, CreditsFragment.class, null);
     */
  }
}
