package com.android.dialer.contactsfragment;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Profile;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

final class MyCard {

  final String name;
  final Uri lookupUri;
  final long photoId;
  @Nullable final Uri photoUri;

  private MyCard(String name, Uri lookupUri, long photoId, @Nullable Uri photoUri) {
    this.name = name;
    this.lookupUri = lookupUri;
    this.photoId = photoId;
    this.photoUri = photoUri;
  }

  @WorkerThread
  @Nullable
  static MyCard load(Context context) {
    String[] projection = {
      Profile._ID, Profile.LOOKUP_KEY, Profile.DISPLAY_NAME, Profile.PHOTO_ID, Profile.PHOTO_URI,
    };
    try (Cursor cursor =
        context.getContentResolver().query(Profile.CONTENT_URI, projection, null, null, null)) {
      if (cursor == null || !cursor.moveToFirst() || TextUtils.isEmpty(cursor.getString(2))) {
        return null;
      }
      String photoUri = cursor.getString(4);
      return new MyCard(
          cursor.getString(2),
          Contacts.getLookupUri(cursor.getLong(0), cursor.getString(1)),
          cursor.getLong(3),
          photoUri == null ? null : Uri.parse(photoUri));
    } catch (RuntimeException e) {
      return null;
    }
  }
}
