package com.android.dialer.app.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.dialer.R;

public class LicensesFragment extends PreferenceFragmentCompat {

  private static final String APACHE_2 = "Apache License 2.0";

  private static final String[][] COMPONENTS = {
    {"AOSP & LineageOS Dialer", APACHE_2},
    {"AndroidX & Material Components", APACHE_2},
    {"Apache Commons IO", APACHE_2},
    {"Apache James Mime4J", APACHE_2},
    {"BlurView", APACHE_2},
    {"Dagger", APACHE_2},
    {"Framework7 Icons", "MIT License"},
    {"Glide", "BSD, part MIT and Apache 2.0"},
    {"Guava", APACHE_2},
    {"libphonenumber", APACHE_2},
    {"Protocol Buffers", "BSD 3-Clause License"},
    {"Volley", APACHE_2},
  };

  @RawRes
  private static final int[] TEXTS = {
    R.raw.license_apache_2_0,
    R.raw.license_apache_2_0,
    R.raw.license_apache_2_0,
    R.raw.license_apache_2_0,
    R.raw.license_apache_2_0,
    R.raw.license_apache_2_0,
    R.raw.license_framework7_icons,
    R.raw.license_glide,
    R.raw.license_apache_2_0,
    R.raw.license_apache_2_0,
    R.raw.license_protobuf,
    R.raw.license_apache_2_0,
  };

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    Context context = getPreferenceManager().getContext();
    PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

    Preference note = new Preference(context);
    note.setLayoutResource(R.layout.preference_footnote);
    note.setSummary(R.string.licenses_footer);
    note.setSelectable(false);
    screen.addPreference(note);

    PreferenceCategory components = new PreferenceCategory(context);
    components.setIconSpaceReserved(false);
    screen.addPreference(components);

    for (int i = 0; i < COMPONENTS.length; i++) {
      Preference preference = new CardPreference(context);
      preference.setTitle(COMPONENTS[i][0]);
      preference.setSummary(COMPONENTS[i][1]);
      preference.setIconSpaceReserved(false);
      preference.setFragment(LicenseTextFragment.class.getName());
      preference.getExtras().putInt(LicenseTextFragment.ARG_TEXT, TEXTS[i]);
      components.addPreference(preference);
    }

    setPreferenceScreen(screen);
  }
}
