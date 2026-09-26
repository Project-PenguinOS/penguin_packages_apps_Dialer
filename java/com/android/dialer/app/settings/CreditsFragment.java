package com.android.dialer.app.settings;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.dialer.R;

public class CreditsFragment extends PreferenceFragmentCompat {

  private static final String[] TEAM = {
    "jendermine", "redducc", "@Venkat3620", "@pranayadmn", "candy paneer", "@Primakre", "@Whoraj",
  };

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    Context context = getPreferenceManager().getContext();
    PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

    Preference header = new Preference(context);
    header.setLayoutResource(R.layout.credits_header);
    header.setTitle(R.string.credits_app_name);
    header.setSummary(versionText(context));
    header.setSelectable(false);
    screen.addPreference(header);

    PreferenceCategory team = new PreferenceCategory(context);
    team.setTitle(R.string.credits_team_category);
    team.setIconSpaceReserved(false);
    screen.addPreference(team);
    for (String member : TEAM) {
      team.addPreference(memberPreference(context, member));
    }

    setPreferenceScreen(screen);
  }

  private static Preference memberPreference(Context context, String member) {
    Preference preference = new Preference(context);
    preference.setTitle(member);
    preference.setIconSpaceReserved(false);
    preference.setSingleLineTitle(true);
    if (member.equals("candy paneer")) {
      preference.setSummary(R.string.credits_kitsune);
    }
    if (member.startsWith("@")) {
      preference.setIntent(
          new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/" + member.substring(1))));
    }
    return preference;
  }

  private static CharSequence versionText(Context context) {
    try {
      PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      return context.getString(
          R.string.credits_version, info.versionName, info.getLongVersionCode());
    } catch (PackageManager.NameNotFoundException e) {
      return null;
    }
  }
}
