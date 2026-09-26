package com.android.dialer.universaldialer;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AppCalls {

  public static final class Option {
    public final String appPackage;
    public final boolean video;
    final long dataId;
    final String mimeType;

    Option(String appPackage, boolean video, long dataId, String mimeType) {
      this.appPackage = appPackage;
      this.video = video;
      this.dataId = dataId;
      this.mimeType = mimeType;
    }

    public Intent intent() {
      return new Intent(Intent.ACTION_VIEW)
          .setDataAndType(ContentUris.withAppendedId(Data.CONTENT_URI, dataId), mimeType)
          .setPackage(appPackage)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
  }

  private static final String[][] CALL_MIME_TYPES = {
    {"vnd.android.cursor.item/vnd.com.whatsapp.voip.call", "com.whatsapp", "0"},
    {"vnd.android.cursor.item/vnd.com.whatsapp.video.call", "com.whatsapp", "1"},
    {"vnd.android.cursor.item/vnd.com.whatsapp.w4b.voip.call", "com.whatsapp.w4b", "0"},
    {"vnd.android.cursor.item/vnd.com.whatsapp.w4b.video.call", "com.whatsapp.w4b", "1"},
    {"vnd.android.cursor.item/vnd.org.telegram.messenger.android.call", "org.telegram.messenger", "0"},
    {"vnd.android.cursor.item/vnd.org.telegram.messenger.android.call.video",
        "org.telegram.messenger", "1"},
    {"vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.call", "org.thoughtcrime.securesms", "0"},
    {"vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.videocall",
        "org.thoughtcrime.securesms", "1"},
  };

  private AppCalls() {}

  @WorkerThread
  public static List<Option> forNumber(Context context, @Nullable String number) {
    List<Option> options = new ArrayList<>();
    if (TextUtils.isEmpty(number)) {
      return options;
    }
    List<Long> contactIds = new ArrayList<>();
    Uri lookup = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
    try (Cursor c =
        context.getContentResolver()
            .query(lookup, new String[] {PhoneLookup.CONTACT_ID}, null, null, null)) {
      while (c != null && c.moveToNext()) {
        contactIds.add(c.getLong(0));
      }
    } catch (RuntimeException e) {
      return options;
    }
    for (long contactId : contactIds) {
      addOptions(context, contactId, options);
    }
    return options;
  }

  @WorkerThread
  private static void addOptions(Context context, long contactId, List<Option> out) {
    String[] mimeTypes = new String[CALL_MIME_TYPES.length];
    for (int i = 0; i < mimeTypes.length; i++) {
      mimeTypes[i] = CALL_MIME_TYPES[i][0];
    }
    String placeholders = TextUtils.join(",", java.util.Collections.nCopies(mimeTypes.length, "?"));
    String[] args = Arrays.copyOf(new String[] {String.valueOf(contactId)}, 1 + mimeTypes.length);
    System.arraycopy(mimeTypes, 0, args, 1, mimeTypes.length);
    try (Cursor c =
        context.getContentResolver()
            .query(
                Data.CONTENT_URI,
                new String[] {Data._ID, Data.MIMETYPE},
                Data.CONTACT_ID + "=? AND " + Data.MIMETYPE + " IN (" + placeholders + ")",
                args,
                null)) {
      while (c != null && c.moveToNext()) {
        String mimeType = c.getString(1);
        for (String[] known : CALL_MIME_TYPES) {
          if (known[0].equals(mimeType)) {
            out.add(new Option(known[1], "1".equals(known[2]), c.getLong(0), mimeType));
          }
        }
      }
    } catch (RuntimeException e) {
    }
  }

  @WorkerThread
  @Nullable
  public static Intent callBackIntent(Context context, String appPackage, @Nullable String number) {
    for (Option option : forNumber(context, number)) {
      if (option.appPackage.equals(appPackage) && !option.video) {
        return option.intent();
      }
    }
    String digits = number == null ? "" : number.replaceAll("[^0-9]", "");
    if (appPackage.startsWith("com.whatsapp") && digits.length() >= 7) {
      return new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + digits))
          .setPackage(appPackage)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    return context.getPackageManager().getLaunchIntentForPackage(appPackage);
  }
}
