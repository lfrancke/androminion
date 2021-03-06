package com.mehtank.androminion.activities;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import com.mehtank.androminion.R;
import com.mehtank.androminion.ui.Strings;
import com.mehtank.androminion.util.ThemeSetter;

/**
 * This activity shows the settings menu.
 *
 * Rewrite to support actionbar (backwards compatible to API7).
 *
 * Could be even better by supporting a modern layout on tablets in landscreen
 * mode with PreferenceFragment I guess, but seems to be a bit more complicated
 * and provides almost no use since preferences are not accessed very often.
 *
 * For example how to do it right:
 * https://github.com/commonsguy/cw-omnibus/tree/master/Prefs/FragmentsBC
 */
public class SettingsActivity extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener {

  private SharedPreferences prefs;

  @Override
  public void onResume() {
    super.onResume();
    ThemeSetter.setTheme(this);
    ThemeSetter.setLanguage(this);
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    prefs.registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    prefs.unregisterOnSharedPreferenceChangeListener(this);
  }

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
  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    if (key.equals("userlang")) {
      Strings.initContext(getApplicationContext());
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    ThemeSetter.setTheme(this);
    ThemeSetter.setLanguage(this);
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_settings);
    setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowTitleEnabled(true);

    addPreferencesFromResource(R.xml.preferences);
  }
}
