package com.android.dialer.app.calllog;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.graphics.Path; 
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.dialer.R;
import com.android.dialer.widget.SwipeAndDragHelper;

public class CallLogSwipeHelper extends SwipeAndDragHelper {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CallLogSwipeHelper(Context context, ActionCompletionContract contract) {
        super(contract);
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof CallLogListItemViewHolder) {
            return makeMovementFlags(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }
        return 0;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        if (viewHolder instanceof CallLogListItemViewHolder) {
            CallLogListItemViewHolder holder = (CallLogListItemViewHolder) viewHolder;
            View itemView = holder.itemView;
            View cardView = holder.callLogEntryView;
            Context context = cardView.getContext();

            float cardTop = itemView.getTop() + cardView.getTop();
            float cardBottom = cardTop + cardView.getHeight();

            c.save();
            c.clipRect(itemView.getLeft(), cardTop, itemView.getRight(), cardBottom);

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0) {
                boolean isSwipingRight = dX > 0;
                paint.setColor(ContextCompat.getColor(context, isSwipingRight
                        ? R.color.dialer_call_green
                        : R.color.dialer_end_call_button_color));

                float marginPx = 1* context.getResources().getDisplayMetrics().density;
                float cornerRadius = 0* context.getResources().getDisplayMetrics().density;

                RectF background = new RectF(
                        itemView.getLeft() + marginPx,
                        cardTop,
                        itemView.getRight() - marginPx,
                        cardBottom);
                float cornerRadiusPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 32, context.getResources().getDisplayMetrics());

                int position = viewHolder.getAdapterPosition();
                float[] radii = new float[8];
                if (position != RecyclerView.NO_POSITION) {
                    if (holder.isFirstInDateGroup) {
                        radii[0] = cornerRadiusPx; // Top-left X
                        radii[1] = cornerRadiusPx; // Top-left Y
                        radii[2] = cornerRadiusPx; // Top-right X
                        radii[3] = cornerRadiusPx; // Top-right Y
                    }
                    if (holder.isLastInDateGroup) {
                        radii[4] = cornerRadiusPx; // Bottom-right X
                        radii[5] = cornerRadiusPx; // Bottom-right Y
                        radii[6] = cornerRadiusPx; // Bottom-left X
                        radii[7] = cornerRadiusPx; // Bottom-left Y
                    }
                }

                Path path = new Path();
                path.addRoundRect(background, radii, Path.Direction.CW);
                c.drawPath(path, paint);

                Drawable icon = ContextCompat.getDrawable(context, isSwipingRight
                        ? R.drawable.quantum_ic_access_time_new_vd_theme_24
                        : R.drawable.quantum_ic_delete_vd_theme_24);

                if (icon != null) {
                    TypedValue typedValue = new TypedValue();
                    Resources.Theme theme = context.getTheme();
                    theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
                    int tintColor = ContextCompat.getColor(context, typedValue.resourceId);
                    icon.mutate().setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);

                    int iconSize = icon.getIntrinsicHeight();
                    int iconMargin = (int) ((cardView.getHeight() - iconSize) / 2);
                    int iconAreaWidth = iconSize + (2 * iconMargin);

                    if (Math.abs(dX) > iconAreaWidth) {
                        int top = (int) (cardTop + iconMargin);
                        int bottom = top + iconSize;

                        if (isSwipingRight) {
                            int left = itemView.getLeft() + iconMargin;
                            icon.setBounds(left, top, left + iconSize, bottom);
                        } else {
                            int right = itemView.getRight() - iconMargin;
                            icon.setBounds(right - iconSize, top, right, bottom);
                        }
                        icon.draw(c);
                    }
                }
            }

            c.restore();

            cardView.setAlpha(1.0f - (Math.abs(dX) / (float) cardView.getWidth()) * 1.5f );
            cardView.setTranslationX(dX);

        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof CallLogListItemViewHolder) {
            View cardView = ((CallLogListItemViewHolder) viewHolder).callLogEntryView;
            cardView.setTranslationX(0f);
            cardView.setAlpha(1.0f);
        }
        super.clearView(recyclerView, viewHolder);
    }
}
