package com.android.dialer.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.dialer.R;

public class PlainListSeparatorDecoration extends RecyclerView.ItemDecoration {

  private final Paint paint = new Paint();
  private final int inset;

  public PlainListSeparatorDecoration(Context context) {
    paint.setColor(context.getColor(R.color.dialer_grouped_separator));
    paint.setStrokeWidth(Math.max(1f, context.getResources().getDisplayMetrics().density / 3f));
    inset = context.getResources().getDimensionPixelSize(R.dimen.dialer_plain_row_separator_inset);
  }

  protected boolean hasSeparatorBelow(@NonNull RecyclerView parent, int position) {
    RecyclerView.Adapter<?> adapter = parent.getAdapter();
    return adapter != null && position >= 0 && position < adapter.getItemCount() - 1;
  }

  @Override
  public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent,
      @NonNull RecyclerView.State state) {
    boolean rtl = parent.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    for (int i = 0; i < parent.getChildCount(); i++) {
      View child = parent.getChildAt(i);
      if (!hasSeparatorBelow(parent, parent.getChildAdapterPosition(child))) {
        continue;
      }
      float y = child.getBottom() + child.getTranslationY();
      float start = rtl ? child.getLeft() : child.getLeft() + inset;
      float end = rtl ? child.getRight() - inset : child.getRight();
      c.drawLine(start, y, end, y, paint);
    }
  }
}
