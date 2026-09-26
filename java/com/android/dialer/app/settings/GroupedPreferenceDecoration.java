package com.android.dialer.app.settings;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.android.dialer.R;
import com.android.dialer.widget.GroupedListDecoration;

public final class GroupedPreferenceDecoration extends GroupedListDecoration {

  private final int headerGap;

  private GroupedPreferenceDecoration(Context context) {
    super(context);
    headerGap = Math.round(4 * context.getResources().getDisplayMetrics().density);
  }

  public static void apply(PreferenceFragmentCompat fragment) {
    RecyclerView list = fragment.getListView();
    if (list == null || list.getTag(R.id.grouped_preference_decoration) != null) {
      return;
    }
    Context context = list.getContext();
    list.setTag(R.id.grouped_preference_decoration, Boolean.TRUE);
    list.addItemDecoration(new GroupedPreferenceDecoration(context));
    list.setBackgroundColor(context.getColor(R.color.dialer_grouped_background));
    list.setClipToPadding(false);
    list.setPadding(list.getPaddingLeft(), list.getPaddingTop(), list.getPaddingRight(),
        Math.round(24 * context.getResources().getDisplayMetrics().density));
    fragment.setDivider(null);
  }

  @Override
  protected boolean isGroupedRow(@NonNull RecyclerView parent, int position) {
    if (!(parent.getAdapter() instanceof PreferenceGroupAdapter)) {
      return false;
    }
    Preference preference = ((PreferenceGroupAdapter) parent.getAdapter()).getItem(position);
    return preference != null
        && !(preference instanceof PreferenceCategory)
        && preference.getLayoutResource() != R.layout.credits_header
        && preference.getLayoutResource() != R.layout.preference_footnote;
  }

  @Override
  protected int gapBeforeGroup(@NonNull RecyclerView parent, int position) {
    return position > 0 ? headerGap : groupGap;
  }

  @Override
  public void getItemOffsets(
      @NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
      @NonNull RecyclerView.State state) {
    super.getItemOffsets(outRect, view, parent, state);
    int position = parent.getChildAdapterPosition(view);
    if (position != RecyclerView.NO_POSITION && !isGroupedRow(parent, position)) {
      outRect.set(margin, position == 0 ? 0 : groupGap - headerGap, margin, 0);
    }
  }
}
