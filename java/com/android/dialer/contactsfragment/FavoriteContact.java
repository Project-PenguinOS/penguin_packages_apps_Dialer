package com.android.dialer.contactsfragment;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class FavoriteContact {

  final long contactId;
  final String name;
  final Uri lookupUri;
  final long photoId;
  @Nullable final Uri photoUri;

  private FavoriteContact(
      long contactId, String name, Uri lookupUri, long photoId, @Nullable Uri photoUri) {
    this.contactId = contactId;
    this.name = name;
    this.lookupUri = lookupUri;
    this.photoId = photoId;
    this.photoUri = photoUri;
  }

  @WorkerThread
  static List<FavoriteContact> load(Context context, boolean withPhoneNumbersOnly) {
    String[] projection = {
      Contacts._ID,
      Contacts.LOOKUP_KEY,
      Contacts.DISPLAY_NAME_PRIMARY,
      Contacts.PHOTO_ID,
      Contacts.PHOTO_THUMBNAIL_URI,
    };
    String selection =
        Contacts.STARRED
            + "=1"
            + (withPhoneNumbersOnly ? " AND " + Contacts.HAS_PHONE_NUMBER + "=1" : "");
    List<FavoriteContact> favorites = new ArrayList<>();
    try (Cursor cursor =
        context
            .getContentResolver()
            .query(Contacts.CONTENT_URI, projection, selection, null, Contacts.SORT_KEY_PRIMARY)) {
      while (cursor != null && cursor.moveToNext()) {
        String name = cursor.getString(2);
        if (TextUtils.isEmpty(name)) {
          continue;
        }
        String photoUri = cursor.getString(4);
        favorites.add(
            new FavoriteContact(
                cursor.getLong(0),
                name,
                Contacts.getLookupUri(cursor.getLong(0), cursor.getString(1)),
                cursor.getLong(3),
                photoUri == null ? null : Uri.parse(photoUri)));
      }
    } catch (RuntimeException e) {
      return Collections.emptyList();
    }
    return favorites;
  }
}
