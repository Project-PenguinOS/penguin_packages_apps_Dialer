package com.android.dialer.app.settings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.android.dialer.R;

public final class GroupedPreferenceDecoration extends RecyclerView.ItemDecoration {

  private final int margin;
  private final int groupGap;
  private final int headerGap;
  private final float radius;
  private final int separatorInset;
  private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint separatorPaint = new Paint();
  private final RectF groupBounds = new RectF();
  private final Path groupPath = new Path();
  private final float[] radii = new float[8];

  private GroupedPreferenceDecoration(Context context) {
    float density = context.getResources().getDisplayMetrics().density;
    margin = Math.round(16 * density);
    groupGap = Math.round(24 * density);
    headerGap = Math.round(4 * density);
    radius = 12 * density;
    separatorInset = Math.round(16 * density);
    cellPaint.setColor(context.getColor(R.color.dialer_grouped_cell));
    separatorPaint.setColor(context.getColor(R.color.dialer_grouped_separator));
    separatorPaint.setStrokeWidth(Math.max(1f, density / 3f));
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

  private static boolean isRow(RecyclerView parent, int position) {
    if (position < 0 || !(parent.getAdapter() instanceof PreferenceGroupAdapter)) {
      return false;
    }
    PreferenceGroupAdapter adapter = (PreferenceGroupAdapter) parent.getAdapter();
    if (position >= adapter.getItemCount()) {
      return false;
    }
    Preference preference = adapter.getItem(position);
    return preference != null && !(preference instanceof PreferenceCategory);
  }

  @Override
  public void getItemOffsets(
      @NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
      @NonNull RecyclerView.State state) {
    int position = parent.getChildAdapterPosition(view);
    if (position == RecyclerView.NO_POSITION) {
      return;
    }
    if (!isRow(parent, position)) {
      outRect.set(margin, position == 0 ? 0 : groupGap - headerGap, margin, 0);
      return;
    }
    outRect.left = margin;
    outRect.right = margin;
    if (!isRow(parent, position - 1)) {
      outRect.top = position > 0 ? headerGap : groupGap;
    }
  }

  @Override
  public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent,
      @NonNull RecyclerView.State state) {
    int count = parent.getChildCount();
    int i = 0;
    while (i < count) {
      View first = parent.getChildAt(i);
      int firstPos = parent.getChildAdapterPosition(first);
      if (!isRow(parent, firstPos)) {
        i++;
        continue;
      }
      int j = i;
      while (j + 1 < count) {
        int nextPos = parent.getChildAdapterPosition(parent.getChildAt(j + 1));
        int pos = parent.getChildAdapterPosition(parent.getChildAt(j));
        if (nextPos != pos + 1 || !isRow(parent, nextPos)) {
          break;
        }
        j++;
      }
      View last = parent.getChildAt(j);
      int lastPos = parent.getChildAdapterPosition(last);
      float top = first.getTop() + first.getTranslationY();
      float bottom = last.getBottom() + last.getTranslationY();
      groupBounds.set(margin, top, parent.getWidth() - margin, bottom);
      float topRadius = isRow(parent, firstPos - 1) ? 0 : radius;
      float bottomRadius = isRow(parent, lastPos + 1) ? 0 : radius;
      radii[0] = radii[1] = radii[2] = radii[3] = topRadius;
      radii[4] = radii[5] = radii[6] = radii[7] = bottomRadius;
      groupPath.rewind();
      groupPath.addRoundRect(groupBounds, radii, Path.Direction.CW);
      c.drawPath(groupPath, cellPaint);

      for (int k = i; k < j; k++) {
        View row = parent.getChildAt(k);
        float y = row.getBottom() + row.getTranslationY();
        c.drawLine(margin + separatorInset, y, parent.getWidth() - margin, y, separatorPaint);
      }
      i = j + 1;
    }
  }
}
