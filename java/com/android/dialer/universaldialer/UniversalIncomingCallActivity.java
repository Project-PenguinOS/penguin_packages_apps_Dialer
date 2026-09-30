package com.android.dialer.universaldialer;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.android.dialer.R;
import com.android.dialer.common.LogUtil;
import com.android.dialer.lettertile.LetterTileDrawable;

import java.lang.ref.WeakReference;

public final class UniversalIncomingCallActivity extends Activity {

  static final String ACTION_ANSWER = "com.android.dialer.universaldialer.ANSWER";
  static final String ACTION_DECLINE = "com.android.dialer.universaldialer.DECLINE";
  private static final String EXTRA_KEY = "notification_key";

  @Nullable private static WeakReference<UniversalIncomingCallActivity> current;
  private String key;

  static Intent intent(Context context, String key, @Nullable String action) {
    return new Intent(context, UniversalIncomingCallActivity.class)
        .setAction(action)
        .putExtra(EXTRA_KEY, key)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
  }

  static CharSequence subtitle(Context context, UniversalCallNotificationListener.TrackedCall call) {
    CharSequence app =
        UniversalDialer.appLabel(
            context, UniversalDialer.notificationAccountComponent(call.appPackage));
    return context.getString(
        call.video
            ? R.string.universal_dialer_call_label_video
            : R.string.universal_dialer_call_label_audio,
        app == null ? call.appPackage : app);
  }

  @Nullable
  static Drawable appIcon(Context context, String appPackage) {
    try {
      return context.getPackageManager().getApplicationIcon(appPackage);
    } catch (PackageManager.NameNotFoundException e) {
      return null;
    }
  }

  static Drawable avatar(Context context, UniversalCallNotificationListener.TrackedCall call) {
    Bitmap photo = toBitmap(loadIcon(context, call.callerIcon));
    if (photo != null) {
      RoundedBitmapDrawable round =
          RoundedBitmapDrawableFactory.create(context.getResources(), photo);
      round.setCircular(true);
      return round;
    }
    LetterTileDrawable tile = new LetterTileDrawable(context.getResources());
    tile.setCanonicalDialerLetterTileDetails(
        call.name == null ? "" : call.name.toString(),
        call.key,
        LetterTileDrawable.SHAPE_CIRCLE,
        LetterTileDrawable.TYPE_DEFAULT);
    return tile;
  }

  @Nullable
  static Drawable loadIcon(Context context, @Nullable Icon icon) {
    if (icon == null) {
      return null;
    }
    try {
      return icon.loadDrawable(context);
    } catch (RuntimeException e) {
      return null;
    }
  }

  @Nullable
  static Bitmap toBitmap(@Nullable Drawable drawable) {
    if (drawable == null) {
      return null;
    }
    if (drawable instanceof BitmapDrawable && ((BitmapDrawable) drawable).getBitmap() != null) {
      return ((BitmapDrawable) drawable).getBitmap();
    }
    int width = Math.max(1, drawable.getIntrinsicWidth());
    int height = Math.max(1, drawable.getIntrinsicHeight());
    if (width == 1 && height == 1) {
      width = height = 256;
    }
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, width, height);
    drawable.draw(canvas);
    return bitmap;
  }

  static void finishFor(String key) {
    UniversalIncomingCallActivity activity = current == null ? null : current.get();
    if (activity != null && key.equals(activity.key)) {
      activity.runOnUiThread(activity::finish);
    }
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setShowWhenLocked(true);
    setTurnScreenOn(true);
    handle(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    handle(intent);
  }

  @Override
  protected void onDestroy() {
    if (current != null && current.get() == this) {
      current = null;
    }
    super.onDestroy();
  }

  private void handle(Intent intent) {
    key = intent.getStringExtra(EXTRA_KEY);
    UniversalCallNotificationListener.TrackedCall call =
        key == null ? null : UniversalCallNotificationListener.get(key);
    if (call == null) {
      finish();
      return;
    }
    if (ACTION_ANSWER.equals(intent.getAction())) {
      answer(call);
      return;
    }
    if (ACTION_DECLINE.equals(intent.getAction())) {
      decline(call);
      return;
    }
    current = new WeakReference<>(this);
    setContentView(R.layout.universal_incoming_call);
    CharSequence name = call.name == null ? "" : call.name;
    ((TextView) findViewById(R.id.universal_call_name)).setText(name);
    ((TextView) findViewById(R.id.universal_call_subtitle)).setText(subtitle(this, call));
    Drawable appIcon = appIcon(this, call.appPackage);
    ((ImageView) findViewById(R.id.universal_call_app_icon)).setImageDrawable(appIcon);
    ((ImageView) findViewById(R.id.universal_call_app_badge)).setImageDrawable(appIcon);
    ((ImageView) findViewById(R.id.universal_call_avatar)).setImageDrawable(avatar(this, call));

    TextView status = findViewById(R.id.universal_call_status);
    boolean showStatus =
        !TextUtils.isEmpty(call.statusText) && !TextUtils.equals(call.statusText, name);
    status.setText(showStatus ? call.statusText : null);
    status.setVisibility(showStatus ? View.VISIBLE : View.GONE);

    TextView verification = findViewById(R.id.universal_call_verification);
    boolean verified = !TextUtils.isEmpty(call.verificationText);
    verification.setText(verified ? call.verificationText : null);
    Drawable badge = verified ? loadIcon(this, call.verificationIcon) : null;
    if (badge != null) {
      int size = Math.round(16 * getResources().getDisplayMetrics().density);
      badge.setBounds(0, 0, size, size);
    }
    verification.setCompoundDrawablesRelative(badge, null, null, null);
    verification.setVisibility(verified ? View.VISIBLE : View.GONE);
    findViewById(R.id.universal_call_answer).setOnClickListener(v -> answer(call));
    findViewById(R.id.universal_call_decline).setOnClickListener(v -> decline(call));
  }

  private void answer(UniversalCallNotificationListener.TrackedCall call) {
    send(call.answerIntent);
    IncomingVoipCallNotifier.dismiss(this, call);
    finish();
  }

  private void decline(UniversalCallNotificationListener.TrackedCall call) {
    UniversalCallNotificationListener.markDeclined(call.key);
    send(call.declineIntent);
    IncomingVoipCallNotifier.dismiss(this, call);
    finish();
  }

  private void send(@Nullable PendingIntent action) {
    if (action == null) {
      return;
    }
    ActivityOptions options = ActivityOptions.makeBasic();
    options.setPendingIntentBackgroundActivityStartMode(
        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
    try {
      action.send(this, 0, null, null, null, null, options.toBundle());
    } catch (PendingIntent.CanceledException e) {
      LogUtil.e("UniversalIncomingCallActivity.send", "app action was cancelled", e);
    }
  }
}
