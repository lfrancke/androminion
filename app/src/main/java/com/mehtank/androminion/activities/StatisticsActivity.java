package com.mehtank.androminion.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import com.mehtank.androminion.R;
import com.mehtank.androminion.util.Achievements;
import com.mehtank.androminion.util.ThemeSetter;
import com.mehtank.androminion.util.compat.StatisticsFragmentsAdapter;

/**
 * This activity just shows two tabs: statistics and achievements.
 */
public class StatisticsActivity extends AppCompatActivity {

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.activity_statistics, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      NavUtils.navigateUpFromSameTask(this);
    } else if (id == R.id.resetstatistics_menu) {
      buildResetDialog(this).show();
    } else {
      return super.onOptionsItemSelected(item);
    }
    return true;
  }

  @Override
  public void onResume() {
    super.onResume();
    ThemeSetter.setTheme(this);
    ThemeSetter.setLanguage(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    ThemeSetter.setTheme(this);
    ThemeSetter.setLanguage(this);
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_combinedstats);

    ViewPager pager = (ViewPager) findViewById(R.id.statistics_pager);
    pager.setAdapter(new StatisticsFragmentsAdapter(getFragmentManager(), this));

    setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    ActionBar actionBar = getSupportActionBar();
    assert actionBar != null;
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowTitleEnabled(true);

/*    mTabsAdapter = new TabsAdapter(this, mViewPager);

    ActionBar.Tab statsTab = actionBar.newTab().setText(R.string.win_loss_menu)
                               .setIcon(android.R.drawable.ic_menu_myplaces);
    mTabsAdapter.addTab(statsTab, WinlossFragment.class, null);

    ActionBar.Tab achievementsTab = actionBar.newTab()
                                      .setText(R.string.achievements_menu)
                                      .setIcon(android.R.drawable.ic_menu_agenda);
    mTabsAdapter.addTab(achievementsTab, AchievementsFragment.class, null);
    */
  }

  private AlertDialog buildResetDialog(final Context context) {
    final boolean[] choices = {true, true};
    class ChoiceListenerClass implements
      DialogInterface.OnMultiChoiceClickListener, OnClickListener {

      private boolean resetStats = choices[0];
      private boolean resetAchievements = choices[1];
      private AlertDialog mDialog;

      @Override
      public void onClick(DialogInterface dialog, int which,
                           boolean isChecked) {
        if (which == 0) {
          resetStats = isChecked;
        } else if (which == 1) {
          resetAchievements = isChecked;
        }
        Button resetButton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (resetStats || resetAchievements) {
          resetButton.setEnabled(true);
        } else {
          resetButton.setEnabled(false);
        }
      }

      @Override
      public void onClick(DialogInterface dialog, int i) {
        Achievements achievements = new Achievements(context);
        if (resetStats) {
          achievements.resetStats();
        }
        if (resetAchievements) {
          achievements.resetAchievements();
        }
      }

      public void setDialog(AlertDialog dialog) {
        mDialog = dialog;
      }
    }
    ChoiceListenerClass choiceListener = new ChoiceListenerClass();
    AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                    .setTitle(R.string.reset)
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .setMultiChoiceItems(R.array.reset_choices, choices, choiceListener)
                                    .setPositiveButton(R.string.reset, choiceListener);
    AlertDialog dialog = builder.create();
    choiceListener.setDialog(dialog);
    return dialog;
  }
}
