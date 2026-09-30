package com.android.dialer.universaldialer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Person;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.android.dialer.common.LogUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class UniversalCallNotificationListener extends NotificationListenerService {

  static final class TrackedCall {
    final String key;
    final String appPackage;
    final boolean incoming;
    final long startMillis;
    CharSequence name;
    @Nullable String number;
    boolean video;
    boolean connected;
    long connectedMillis;
    boolean declinedHere;
    @Nullable PendingIntent answerIntent;
    @Nullable PendingIntent declineIntent;

    TrackedCall(String key, String appPackage, boolean incoming, long startMillis) {
      this.key = key;
      this.appPackage = appPackage;
      this.incoming = incoming;
      this.startMillis = startMillis;
    }
  }

  private static final Map<String, TrackedCall> calls = new HashMap<>();

  private static final Set<String> telecomCallPackages = new HashSet<>();

  static synchronized void onTelecomVoipCall(String appPackage, boolean active) {
    if (active) {
      telecomCallPackages.add(appPackage);
    } else {
      telecomCallPackages.remove(appPackage);
    }
  }

  private static final long HIDE_SPAN_MS = 10_000;

  private static final long SNOOZED_CHECK_MS = 1_000;

  private static final Set<String> hiddenKeys = new HashSet<>();

  private static final Set<String> hiddenTelecomPackages = new HashSet<>();

  private static final Set<String> snoozedKeys = new HashSet<>();

  @Nullable private static UniversalCallNotificationListener connected;

  private final Handler handler = new Handler(Looper.getMainLooper());

  public static void onTelecomCallShown(String appPackage, boolean shown) {
    UniversalCallNotificationListener listener;
    synchronized (UniversalCallNotificationListener.class) {
      if (shown) {
        hiddenTelecomPackages.add(appPackage);
      } else {
        hiddenTelecomPackages.remove(appPackage);
      }
      listener = connected;
    }
    if (shown && listener != null) {
      listener.handler.post(() -> listener.hideCallNotificationsOf(appPackage));
    }
  }

  static synchronized void stopHiding(String key) {
    hiddenKeys.remove(key);
  }

  @Override
  public void onListenerConnected() {
    synchronized (UniversalCallNotificationListener.class) {
      connected = this;
    }
  }

  @Override
  public void onListenerDisconnected() {
    synchronized (UniversalCallNotificationListener.class) {
      if (connected == this) {
        connected = null;
      }
    }
  }

  private static boolean isCallStyle(Notification notification) {
    return Notification.CallStyle.class
        .getName()
        .equals(notification.extras.getString(Notification.EXTRA_TEMPLATE));
  }

  private void hideCallNotificationsOf(String appPackage) {
    StatusBarNotification[] active;
    try {
      active = getActiveNotifications();
    } catch (RuntimeException e) {
      return;
    }
    if (active == null) {
      return;
    }
    for (StatusBarNotification sbn : active) {
      if (sbn.getPackageName().equals(appPackage) && isCallStyle(sbn.getNotification())) {
        hide(sbn.getKey());
      }
    }
  }

  private void hide(String key) {
    try {
      snoozeNotification(key, HIDE_SPAN_MS);
    } catch (RuntimeException e) {
      LogUtil.e("UniversalCallNotificationListener.hide", "snooze failed", e);
      return;
    }
    boolean startChecking;
    synchronized (UniversalCallNotificationListener.class) {
      startChecking = snoozedKeys.isEmpty();
      snoozedKeys.add(key);
    }
    if (startChecking) {
      handler.postDelayed(this::checkSnoozed, SNOOZED_CHECK_MS);
    }
  }

  private void checkSnoozed() {
    Set<String> present = new HashSet<>();
    try {
      StatusBarNotification[] snoozed = getSnoozedNotifications();
      if (snoozed != null) {
        for (StatusBarNotification sbn : snoozed) {
          present.add(sbn.getKey());
        }
      }
      StatusBarNotification[] active = getActiveNotifications();
      if (active != null) {
        for (StatusBarNotification sbn : active) {
          present.add(sbn.getKey());
        }
      }
    } catch (RuntimeException e) {
      return;
    }
    Set<String> gone = new HashSet<>();
    boolean more;
    synchronized (UniversalCallNotificationListener.class) {
      for (String key : snoozedKeys) {
        if (!present.contains(key)) {
          gone.add(key);
        }
      }
      snoozedKeys.removeAll(gone);
      more = !snoozedKeys.isEmpty();
    }
    for (String key : gone) {
      endCall(key);
    }
    if (more) {
      handler.postDelayed(this::checkSnoozed, SNOOZED_CHECK_MS);
    }
  }

  @Nullable
  static synchronized TrackedCall get(String key) {
    return calls.get(key);
  }

  @Override
  public void onNotificationPosted(StatusBarNotification sbn) {
    if (!UniversalDialer.isEnabled(this)) {
      return;
    }
    Notification notification = sbn.getNotification();
    Bundle extras = notification.extras;
    if (!Notification.CallStyle.class.getName().equals(extras.getString(Notification.EXTRA_TEMPLATE))) {
      return;
    }
    String appPackage = sbn.getPackageName();
    synchronized (UniversalCallNotificationListener.class) {
      if (appPackage.equals(getPackageName())) {
        return;
      }
      if (telecomCallPackages.contains(appPackage)) {
        if (hiddenTelecomPackages.contains(appPackage)) {
          handler.post(() -> hide(sbn.getKey()));
        }
        return;
      }
    }
    if (UniversalDialer.isPhoneApp(this, appPackage)) {
      return;
    }

    int type = extras.getInt(Notification.EXTRA_CALL_TYPE, 0);
    TrackedCall call;
    boolean isNew;
    synchronized (UniversalCallNotificationListener.class) {
      call = calls.get(sbn.getKey());
      isNew = call == null;
      if (isNew) {
        call =
            new TrackedCall(
                sbn.getKey(),
                appPackage,
                type == Notification.CallStyle.CALL_TYPE_INCOMING,
                System.currentTimeMillis());
        calls.put(sbn.getKey(), call);
      }
    }

    Person person = extras.getParcelable(Notification.EXTRA_CALL_PERSON, Person.class);
    CharSequence name = person != null && !TextUtils.isEmpty(person.getName())
        ? person.getName()
        : extras.getCharSequence(Notification.EXTRA_TITLE);
    if (!TextUtils.isEmpty(name)) {
      call.name = name;
    }
    if (call.number == null) {
      call.number = numberFor(this, person, call.name);
    }
    call.video = extras.getBoolean(Notification.EXTRA_CALL_IS_VIDEO, call.video);
    call.answerIntent = extras.getParcelable(Notification.EXTRA_ANSWER_INTENT, PendingIntent.class);
    call.declineIntent =
        extras.getParcelable(Notification.EXTRA_DECLINE_INTENT, PendingIntent.class);

    if (!call.connected && extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)) {
      call.connected = true;
      call.connectedMillis = notification.when > 0 ? notification.when : System.currentTimeMillis();
      IncomingVoipCallNotifier.dismiss(this, call);
    }

    if (isNew && call.incoming && UniversalDialer.shouldHandleVoip(this, appPackage)) {
      LogUtil.i("UniversalCallNotificationListener", "showing incoming %s call", appPackage);
      synchronized (UniversalCallNotificationListener.class) {
        hiddenKeys.add(call.key);
      }
      IncomingVoipCallNotifier.show(this, call);
    }
    boolean keepHidden;
    synchronized (UniversalCallNotificationListener.class) {
      keepHidden = hiddenKeys.contains(call.key);
    }
    if (keepHidden) {
      hide(call.key);
    }
  }

  @Override
  public void onNotificationRemoved(
      StatusBarNotification sbn, RankingMap rankingMap, int reason) {
    if (reason == REASON_SNOOZED) {
      return;
    }
    synchronized (UniversalCallNotificationListener.class) {
      snoozedKeys.remove(sbn.getKey());
    }
    endCall(sbn.getKey());
  }

  private void endCall(String key) {
    TrackedCall call;
    synchronized (UniversalCallNotificationListener.class) {
      call = calls.remove(key);
      hiddenKeys.remove(key);
    }
    if (call == null) {
      return;
    }
    IncomingVoipCallNotifier.dismiss(this, call);
    Context context = getApplicationContext();
    new Thread(() -> writeCallLog(context, call), "UniversalDialer.writeCallLog").start();
  }

  static synchronized void markDeclined(String key) {
    TrackedCall call = calls.get(key);
    if (call != null) {
      call.declinedHere = true;
    }
  }

  private static void writeCallLog(Context context, TrackedCall call) {
    int type;
    if (!call.incoming) {
      type = Calls.OUTGOING_TYPE;
    } else if (call.connected) {
      type = Calls.INCOMING_TYPE;
    } else {
      type = call.declinedHere ? Calls.REJECTED_TYPE : Calls.MISSED_TYPE;
    }
    long durationSeconds =
        call.connected ? Math.max(0, (System.currentTimeMillis() - call.connectedMillis) / 1000) : 0;
    String name = call.name == null ? "" : call.name.toString();

    ContentValues values = new ContentValues();
    values.put(Calls.NUMBER, !TextUtils.isEmpty(call.number) ? call.number : name);
    values.put(Calls.CACHED_NAME, name);
    values.put(Calls.NUMBER_PRESENTATION, Calls.PRESENTATION_ALLOWED);
    values.put(Calls.TYPE, type);
    values.put(Calls.DATE, call.startMillis);
    values.put(Calls.DURATION, durationSeconds);
    values.put(Calls.FEATURES, call.video ? Calls.FEATURES_VIDEO : 0);
    values.put(
        Calls.PHONE_ACCOUNT_COMPONENT_NAME,
        UniversalDialer.notificationAccountComponent(call.appPackage));
    values.put(Calls.PHONE_ACCOUNT_ID, call.appPackage);
    values.put(Calls.NEW, type == Calls.MISSED_TYPE ? 1 : 0);
    values.put(Calls.IS_READ, type == Calls.MISSED_TYPE ? 0 : 1);
    try {
      context.getContentResolver().insert(Calls.CONTENT_URI, values);
    } catch (RuntimeException e) {
      LogUtil.e("UniversalCallNotificationListener.writeCallLog", "insert failed", e);
    }
  }

  @Nullable
  private static String numberFor(Context context, @Nullable Person person, CharSequence name) {
    if (person != null && person.getUri() != null && person.getUri().startsWith("tel:")) {
      return Uri.parse(person.getUri()).getSchemeSpecificPart();
    }
    if (TextUtils.isEmpty(name)) {
      return null;
    }
    try (Cursor c =
        context.getContentResolver()
            .query(
                Phone.CONTENT_URI,
                new String[] {Phone.NUMBER},
                Phone.DISPLAY_NAME + "=?",
                new String[] {name.toString()},
                Phone.IS_SUPER_PRIMARY + " DESC, " + Phone.IS_PRIMARY + " DESC")) {
      return c != null && c.moveToFirst() ? c.getString(0) : null;
    } catch (RuntimeException e) {
      return null;
    }
  }
}
