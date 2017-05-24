package com.mehtank.androminion.util;

import java.util.HashMap;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;
import com.mehtank.androminion.R;

/**
 * Helper class for maintaining the same theme throughout the application.
 */
public class ThemeSetter {

  @SuppressWarnings("unused")
  private static final String TAG = "ThemeSetter";

  private static final Locale DefaultLocale = Locale.getDefault();

  private static final HashMap<String, Integer> THEMES_BAR = new HashMap<String, Integer>() {
    private static final long serialVersionUID = 1L;

    {
      put("androminion-dark", R.style.Theme_Androminion_Dark);
      put("androminion-light", R.style.Theme_Androminion_Light);
      put("androminion-light-darkbar", R.style.Theme_Androminion_Light_DarkActionBar);
      // sync with R.array.theme_keys and themes
    }
  };

  /**
   * Sets the theme of the passed Context to the theme chosen in preferences.
   *
   * @param ctx the theme of this Context will be changed.
   */
  public static void setTheme(Context ctx) {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
    ctx.setTheme(THEMES_BAR.get(pref.getString("theme", "androminion-light")));
  }

  public static void setLanguage(Activity act) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(act);

    Configuration config = act.getBaseContext().getResources().getConfiguration();

    String lang = settings.getString(act.getString(R.string.userlang_pref),
      "default");
    Log.d("AndrominionApplication", "lang set to " + lang);
    if ("default".equals(lang)) {
      lang = DefaultLocale.getLanguage();
    }
    if (!config.locale.getLanguage().equals(lang)) {
      Locale locale = new Locale(lang);
      Locale.setDefault(locale);
      config.locale = locale;
      act.getBaseContext()
        .getResources()
        .updateConfiguration(
          config,
          act.getBaseContext().getResources()
            .getDisplayMetrics());
    }
  }
}
