package com.android.dialer.universaldialer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.Context;
import android.content.Intent;

import com.android.dialer.R;
import com.android.dialer.notification.NotificationChannelId;
import com.android.dialer.notification.NotificationChannelManager;

final class IncomingVoipCallNotifier {

  private static final String TAG = "universal_voip_call";

  private IncomingVoipCallNotifier() {}

  static void show(Context context, UniversalCallNotificationListener.TrackedCall call) {
    NotificationChannelManager.initChannels(context);
    CharSequence name = call.name == null ? "" : call.name;
    CharSequence subtitle = UniversalIncomingCallActivity.subtitle(context, call);

    PendingIntent fullScreen =
        PendingIntent.getActivity(
            context,
            requestCode(call, 0),
            UniversalIncomingCallActivity.intent(context, call.key, null),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    PendingIntent answer =
        PendingIntent.getActivity(
            context,
            requestCode(call, 1),
            UniversalIncomingCallActivity.intent(
                context, call.key, UniversalIncomingCallActivity.ACTION_ANSWER),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    PendingIntent decline =
        PendingIntent.getActivity(
            context,
            requestCode(call, 2),
            UniversalIncomingCallActivity.intent(
                context, call.key, UniversalIncomingCallActivity.ACTION_DECLINE),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

    Person caller = new Person.Builder().setName(name).setImportant(true).build();
    Notification notification =
        new Notification.Builder(context, NotificationChannelId.INCOMING_CALL)
            .setSmallIcon(R.drawable.quantum_ic_call_vd_theme_24)
            .setContentTitle(name)
            .setContentText(subtitle)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setStyle(Notification.CallStyle.forIncomingCall(caller, decline, answer))
            .build();
    context.getSystemService(NotificationManager.class).notify(TAG, id(call), notification);
  }

  static void dismiss(Context context, UniversalCallNotificationListener.TrackedCall call) {
    context.getSystemService(NotificationManager.class).cancel(TAG, id(call));
    UniversalIncomingCallActivity.finishFor(call.key);
  }

  private static int id(UniversalCallNotificationListener.TrackedCall call) {
    return call.key.hashCode();
  }

  private static int requestCode(UniversalCallNotificationListener.TrackedCall call, int action) {
    return call.key.hashCode() * 3 + action;
  }
}
