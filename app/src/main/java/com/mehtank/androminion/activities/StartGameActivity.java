package com.mehtank.androminion.activities;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import com.mehtank.androminion.R;
import com.mehtank.androminion.fragments.StartGameFragment;
import com.mehtank.androminion.fragments.StartGameFragment.OnStartGameListener;
import com.mehtank.androminion.util.ThemeSetter;

/**
 * This activity shows the start game screen where players can be selected.
 * The actual content is in StartGameFragment.
 */
public class StartGameActivity extends AppCompatActivity implements OnStartGameListener {

  @Override
  public void onStartGameClick(ArrayList<String> values) {
    Intent data = new Intent();
    data.putStringArrayListExtra("command", values);
    setResult(RESULT_OK, data);
    finish();
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

    setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    ActionBar actionBar = getSupportActionBar();
    assert actionBar != null;
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowTitleEnabled(true);

    if (savedInstanceState == null) {
      Fragment startGameFragment = new StartGameFragment();

      if (getIntent().hasExtra("cards")) {
        startGameFragment.setArguments(getIntent().getExtras());
      }

      getSupportFragmentManager().beginTransaction().add(android.R.id.content, startGameFragment).commit();
    }
  }
}
