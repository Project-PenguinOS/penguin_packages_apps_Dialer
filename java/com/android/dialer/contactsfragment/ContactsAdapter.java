/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright (C) 2023-2025 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dialer.contactsfragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.recyclerview.widget.RecyclerView;

import com.android.dialer.R;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.contactphoto.ContactPhotoManager;
import com.android.dialer.contactsfragment.ContactsFragment.Header;
import com.android.dialer.contactsfragment.ContactsFragment.OnContactSelectedListener;
import com.android.dialer.lettertile.LetterTileDrawable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;

/** List adapter for the union of all contacts associated with every account on the device. */
final class ContactsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private static final int UNKNOWN_VIEW_TYPE = 0;
  private static final int ADD_CONTACT_VIEW_TYPE = 1;
  private static final int CONTACT_VIEW_TYPE = 2;
  private static final int MY_CARD_VIEW_TYPE = 3;

  /** An Enum for the different row view types shown by this adapter. */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({UNKNOWN_VIEW_TYPE, ADD_CONTACT_VIEW_TYPE, CONTACT_VIEW_TYPE, MY_CARD_VIEW_TYPE})
  @interface ContactsViewType {}

  private final ArrayMap<ContactViewHolder, Integer> holderMap = new ArrayMap<>();
  private final Context context;
  private final @Header int header;
  private final OnContactSelectedListener onContactSelectedListener;

  // List of contact sublist headers
  private String[] headers = new String[0];
  // Number of contacts that correspond to each header in {@code headers}.
  private int[] counts = new int[0];
  // Cursor with list of contacts
  private Cursor cursor;
  @Nullable private MyCard myCard;
  private List<FavoriteContact> favorites = Collections.emptyList();

  ContactsAdapter(
      Context context, @Header int header, OnContactSelectedListener onContactSelectedListener) {
    this.context = context;
    this.header = header;
    this.onContactSelectedListener = Assert.isNotNull(onContactSelectedListener);
  }

  void setMyCard(@Nullable MyCard myCard) {
    this.myCard = myCard;
    notifyDataSetChanged();
  }

  void setFavorites(List<FavoriteContact> favorites) {
    this.favorites = favorites;
    notifyDataSetChanged();
  }

  private int leadingRows() {
    return (header != Header.NONE ? 1 : 0) + (myCard != null ? 1 : 0);
  }

  private int rowsBeforeCursor() {
    return leadingRows() + favorites.size();
  }

  private boolean isFavoriteRow(int position) {
    return position >= leadingRows() && position < rowsBeforeCursor();
  }

  void updateCursor(Cursor cursor) {
    this.cursor = cursor;
    headers = cursor.getExtras().getStringArray(Contacts.EXTRA_ADDRESS_BOOK_INDEX_TITLES);
    counts = cursor.getExtras().getIntArray(Contacts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS);
    if (counts != null) {
      int sum = 0;
      for (int count : counts) {
        sum += count;
      }

      if (sum != cursor.getCount()) {
        LogUtil.e(
            "ContactsAdapter", "Count sum (%d) != cursor count (%d).", sum, cursor.getCount());
      }
    }
    notifyDataSetChanged();
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(
      ViewGroup parent, @ContactsViewType int viewType) {
    switch (viewType) {
      case ADD_CONTACT_VIEW_TYPE:
        return new AddContactViewHolder(
            LayoutInflater.from(context).inflate(R.layout.add_contact_row, parent, false));
      case MY_CARD_VIEW_TYPE:
        return new RecyclerView.ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.my_card_row, parent, false)) {};
      case CONTACT_VIEW_TYPE:
        return new ContactViewHolder(
            LayoutInflater.from(context).inflate(R.layout.contact_row, parent, false),
            onContactSelectedListener);
      case UNKNOWN_VIEW_TYPE:
      default:
        throw Assert.createIllegalStateFailException("Invalid view type: " + viewType);
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
    if (viewHolder instanceof AddContactViewHolder) {
      return;
    }
    if (getItemViewType(position) == MY_CARD_VIEW_TYPE) {
      bindMyCard(viewHolder.itemView);
      return;
    }

    ContactViewHolder contactViewHolder = (ContactViewHolder) viewHolder;
    holderMap.put(contactViewHolder, position);

    String name;
    Uri contactUri;
    long contactId;
    long photoId;
    Uri photoUri;
    if (isFavoriteRow(position)) {
      FavoriteContact favorite = favorites.get(position - leadingRows());
      name = favorite.name;
      contactUri = favorite.lookupUri;
      contactId = favorite.contactId;
      photoId = favorite.photoId;
      photoUri = favorite.photoUri;
    } else {
      cursor.moveToPosition(position - rowsBeforeCursor());
      name = getDisplayName(cursor);
      contactUri = getContactUri(cursor);
      contactId = getContactId(cursor);
      photoId = getPhotoId(cursor);
      photoUri = getPhotoUri(cursor);
    }
    String header = getHeaderString(position);

    ContactPhotoManager.getInstance(context)
        .loadDialerThumbnailOrPhoto(
            contactViewHolder.getPhoto(),
            contactUri,
            photoId,
            photoUri,
            name,
            LetterTileDrawable.TYPE_DEFAULT);

    String photoDescription = context.getString(R.string.description_quick_contact_for, name);
    contactViewHolder.getPhoto().setContentDescription(photoDescription);

    // Always show the view holder's header if it's the first item in the list. Otherwise, compare
    // it to the previous element and only show the anchored header if the row elements fall into
    // the same sublists.
    boolean showHeader = isFirstInSection(position);

contactViewHolder.bind(header, name, contactUri, contactId, showHeader);

boolean isFirstInGroup = (position == 1);
boolean isLastInGroup = (position == getItemCount() - 1);

int drawableId;
if (isFirstInGroup && isLastInGroup) {
    drawableId = R.drawable.call_log_card_background_single;
} else if (isFirstInGroup) {
    drawableId = R.drawable.call_log_card_background_top;
} else if (isLastInGroup) {
    drawableId = R.drawable.call_log_card_background_bottom;
} else {
    drawableId = R.drawable.call_log_card_background_middle;
}
contactViewHolder.itemView.findViewById(R.id.contact_row).setBackgroundResource(drawableId);


  }

  /**
   * Returns {@link #ADD_CONTACT_VIEW_TYPE} if the adapter was initialized with {@link
   * Header#ADD_CONTACT} and the position is 0. Otherwise, {@link #CONTACT_VIEW_TYPE}.
   */
  @Override
  public @ContactsViewType int getItemViewType(int position) {
    if (header != Header.NONE && position == 0) {
      return ADD_CONTACT_VIEW_TYPE;
    }
    if (myCard != null && position == leadingRows() - 1) {
      return MY_CARD_VIEW_TYPE;
    }
    return CONTACT_VIEW_TYPE;
  }

  private void bindMyCard(View row) {
    QuickContactBadge photo = row.findViewById(R.id.my_card_photo);
    ((TextView) row.findViewById(R.id.my_card_name)).setText(myCard.name);
    ContactPhotoManager.getInstance(context)
        .loadDialerThumbnailOrPhoto(
            photo, myCard.lookupUri, myCard.photoId, myCard.photoUri, myCard.name,
            LetterTileDrawable.TYPE_DEFAULT);
    Uri lookupUri = myCard.lookupUri;
    row.setOnClickListener(
        v -> context.startActivity(new Intent(Intent.ACTION_VIEW, lookupUri)));
  }

  @Override
  public void onViewRecycled(RecyclerView.ViewHolder contactViewHolder) {
    super.onViewRecycled(contactViewHolder);
    if (contactViewHolder instanceof ContactViewHolder) {
      holderMap.remove(contactViewHolder);
    }
  }

  void refreshHeaders() {
    for (ContactViewHolder holder : holderMap.keySet()) {
      int position = holderMap.get(holder);
      holder.getHeaderView().setVisibility(isFirstInSection(position) ? View.VISIBLE : View.GONE);
    }
  }

  boolean isContactRow(int position) {
    return position >= leadingRows() && position < getItemCount();
  }

  boolean isFirstInSection(int position) {
    int firstContact = leadingRows();
    return position == firstContact
        || (position > firstContact
            && !getHeaderString(position).equals(getHeaderString(position - 1)));
  }

  int getPositionForSection(String title) {
    int position = rowsBeforeCursor();
    for (int i = 0; i < headers.length; i++) {
      if (headers[i].equals(title)) {
        return position;
      }
      position += counts[i];
    }
    return -1;
  }

  String[] getSectionTitles() {
    return headers;
  }

  @Override
  public int getItemCount() {
    int count = cursor == null || cursor.isClosed() ? 0 : cursor.getCount();
    return count + rowsBeforeCursor();
  }

  private static String getDisplayName(Cursor cursor) {
    return cursor.getString(ContactsCursorLoader.CONTACT_DISPLAY_NAME);
  }

  private static long getPhotoId(Cursor cursor) {
    return cursor.getLong(ContactsCursorLoader.CONTACT_PHOTO_ID);
  }

  private static Uri getPhotoUri(Cursor cursor) {
    String photoUri = cursor.getString(ContactsCursorLoader.CONTACT_PHOTO_URI);
    return photoUri == null ? null : Uri.parse(photoUri);
  }

  private static Uri getContactUri(Cursor cursor) {
    long contactId = getContactId(cursor);
    String lookupKey = cursor.getString(ContactsCursorLoader.CONTACT_LOOKUP_KEY);
    return Contacts.getLookupUri(contactId, lookupKey);
  }

  private static long getContactId(Cursor cursor) {
    return cursor.getLong(ContactsCursorLoader.CONTACT_ID);
  }

  String getHeaderString(int position) {
    if (position < leadingRows()) {
      return "+";
    }
    if (isFavoriteRow(position)) {
      return context.getString(R.string.contacts_favourites_header);
    }
    position -= rowsBeforeCursor();

    int index = -1;
    int sum = 0;
    while (sum <= position) {
      sum += counts[++index];
    }
    return headers[index];
  }
}
