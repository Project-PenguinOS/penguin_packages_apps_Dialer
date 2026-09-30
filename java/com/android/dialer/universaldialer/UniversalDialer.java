package com.android.dialer.universaldialer;

import android.app.KeyguardManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.telecom.Call;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.android.dialer.R;

public final class UniversalDialer {

  private static final String TELEPHONY_PACKAGE = "com.android.phone";

  private static final String QUERY_INCLUDE_VOIP_CALLS = "include_voip_calls";

  private static final long FOREGROUND_LOOKBACK_MS = 60 * 60 * 1000;

  private UniversalDialer() {}

  public static boolean isEnabled(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    return prefs.getBoolean(
        context.getString(R.string.universal_dialer_key),
        context.getResources().getBoolean(R.bool.universal_dialer_default));
  }

  public static Uri callLogUri(Context context, Uri base) {
    if (!isEnabled(context)) {
      return base;
    }
    return base.buildUpon().appendQueryParameter(QUERY_INCLUDE_VOIP_CALLS, "true").build();
  }

  public static boolean isVoipAccount(@Nullable String componentName) {
    if (TextUtils.isEmpty(componentName)) {
      return false;
    }
    ComponentName component = ComponentName.unflattenFromString(componentName);
    return component != null && !TELEPHONY_PACKAGE.equals(component.getPackageName());
  }

  @Nullable
  public static CharSequence appLabel(Context context, @Nullable String componentName) {
    ComponentName component =
        componentName == null ? null : ComponentName.unflattenFromString(componentName);
    if (component == null) {
      return null;
    }
    PackageManager pm = context.getPackageManager();
    try {
      return pm.getApplicationLabel(pm.getApplicationInfo(component.getPackageName(), 0));
    } catch (PackageManager.NameNotFoundException e) {
      return null;
    }
  }

  @Nullable
  public static CharSequence accountLabel(Context context, @Nullable PhoneAccountHandle handle) {
    if (handle == null) {
      return null;
    }
    try {
      PhoneAccount account = context.getSystemService(TelecomManager.class).getPhoneAccount(handle);
      if (account != null && !TextUtils.isEmpty(account.getLabel())) {
        return account.getLabel();
      }
    } catch (SecurityException e) {
    }
    return appLabel(context, handle.getComponentName().flattenToString());
  }

  public static void onCallShown(Call call, boolean shown) {
    String owner = selfManagedOwner(call);
    if (owner != null) {
      UniversalCallNotificationListener.onTelecomCallShown(owner, shown);
    }
  }

  public static void onCallAdded(Call call) {
    String owner = selfManagedOwner(call);
    if (owner != null) {
      UniversalCallNotificationListener.onTelecomVoipCall(owner, true);
    }
  }

  public static void onCallRemoved(Call call) {
    String owner = selfManagedOwner(call);
    if (owner != null) {
      UniversalCallNotificationListener.onTelecomVoipCall(owner, false);
    }
  }

  @Nullable
  private static String selfManagedOwner(Call call) {
    PhoneAccountHandle handle = call.getDetails().getAccountHandle();
    return isSelfManaged(call) && handle != null ? handle.getComponentName().getPackageName() : null;
  }

  public static boolean hasNotificationAccess(Context context) {
    return context.getSystemService(android.app.NotificationManager.class)
        .isNotificationListenerAccessGranted(
            new ComponentName(context, UniversalCallNotificationListener.class));
  }

  public static boolean isSelfManaged(Call call) {
    Call.Details details = call.getDetails();
    return details.hasProperty(Call.Details.PROPERTY_SELF_MANAGED)
        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            && details.hasProperty(Call.Details.PROPERTY_IS_TRANSACTIONAL));
  }

  public static boolean isPhoneApp(Context context, String packageName) {
    if (TELEPHONY_PACKAGE.equals(packageName) || "com.android.server.telecom".equals(packageName)) {
      return true;
    }
    Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:")).setPackage(packageName);
    return !context.getPackageManager().queryIntentActivities(dial, 0).isEmpty();
  }

  public static boolean shouldHandle(Context context, Call call) {
    if (!isSelfManaged(call)) {
      return true;
    }
    if (!isEnabled(context)) {
      return false;
    }
    PhoneAccountHandle handle = call.getDetails().getAccountHandle();
    String owner = handle == null ? null : handle.getComponentName().getPackageName();
    return owner == null || shouldHandleVoip(context, owner);
  }

  public static boolean shouldHandleVoip(Context context, String ownerPackage) {
    PowerManager power = context.getSystemService(PowerManager.class);
    KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
    if (!power.isInteractive() || keyguard.isKeyguardLocked()) {
      return true;
    }
    return !ownerPackage.equals(foregroundPackage(context));
  }

  public static final String NOTIFICATION_ACCOUNT_CLASS = "com.android.dialer.universaldialer.Call";

  public static boolean isNotificationEntry(@Nullable String componentName) {
    ComponentName component =
        componentName == null ? null : ComponentName.unflattenFromString(componentName);
    return component != null && NOTIFICATION_ACCOUNT_CLASS.equals(component.getClassName());
  }

  public static String notificationAccountComponent(String appPackage) {
    return new ComponentName(appPackage, NOTIFICATION_ACCOUNT_CLASS).flattenToString();
  }

  public static boolean canDetectForegroundApp(Context context) {
    return foregroundPackage(context) != null;
  }

  @Nullable
  static String foregroundPackage(Context context) {
    UsageStatsManager usage = context.getSystemService(UsageStatsManager.class);
    long now = System.currentTimeMillis();
    UsageEvents events;
    try {
      events = usage.queryEvents(now - FOREGROUND_LOOKBACK_MS, now);
    } catch (SecurityException e) {
      return null;
    }
    String last = null;
    UsageEvents.Event event = new UsageEvents.Event();
    while (events.hasNextEvent()) {
      events.getNextEvent(event);
      if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
        last = event.getPackageName();
      }
    }
    return last;
  }
}
