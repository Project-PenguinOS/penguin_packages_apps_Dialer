package com.android.dialer.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.dialer.R;

public abstract class GroupedListDecoration extends RecyclerView.ItemDecoration {

  protected final int margin;
  protected final int groupGap;
  private final float radius;
  private final int separatorInset;
  private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint separatorPaint = new Paint();
  private final RectF groupBounds = new RectF();
  private final Path groupPath = new Path();
  private final float[] radii = new float[8];

  protected GroupedListDecoration(Context context) {
    float density = context.getResources().getDisplayMetrics().density;
    margin = Math.round(16 * density);
    groupGap = Math.round(24 * density);
    radius = 12 * density;
    separatorInset = Math.round(16 * density);
    cellPaint.setColor(context.getColor(R.color.dialer_grouped_cell));
    separatorPaint.setColor(context.getColor(R.color.dialer_grouped_separator));
    separatorPaint.setStrokeWidth(Math.max(1f, density / 3f));
  }

  protected abstract boolean isGroupedRow(@NonNull RecyclerView parent, int position);

  protected int gapBeforeGroup(@NonNull RecyclerView parent, int position) {
    return position == 0 ? 0 : groupGap;
  }

  private boolean isRow(RecyclerView parent, int position) {
    RecyclerView.Adapter<?> adapter = parent.getAdapter();
    return adapter != null
        && position >= 0
        && position < adapter.getItemCount()
        && isGroupedRow(parent, position);
  }

  @Override
  public void getItemOffsets(
      @NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
      @NonNull RecyclerView.State state) {
    int position = parent.getChildAdapterPosition(view);
    if (position == RecyclerView.NO_POSITION || !isRow(parent, position)) {
      return;
    }
    outRect.left = margin;
    outRect.right = margin;
    if (!isRow(parent, position - 1)) {
      outRect.top = gapBeforeGroup(parent, position);
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
        int pos = parent.getChildAdapterPosition(parent.getChildAt(j));
        int nextPos = parent.getChildAdapterPosition(parent.getChildAt(j + 1));
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
