package com.mehtank.androminion.activities;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
    ThemeSetter.setTheme(this);
    ThemeSetter.setLanguage(this);
    super.onResume();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    ThemeSetter.setTheme(this);
    ThemeSetter.setLanguage(this);
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_about);

    ViewPager pager = (ViewPager) findViewById(R.id.about_pager);
    pager.setAdapter(new AboutFragmentsAdapter(getFragmentManager(), this));

    setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    ActionBar actionBar = getSupportActionBar();
    assert actionBar != null;
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowTitleEnabled(true);


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
