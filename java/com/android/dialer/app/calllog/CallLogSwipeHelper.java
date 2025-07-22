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
            View cardView = holder.callLogEntryView; // The card to animate
            Context context = cardView.getContext();

            // --- Key Fix: Calculate bounds based on the card's specific position ---
            // This prevents drawing over or under the date dividers.
            float cardTop = itemView.getTop() + cardView.getTop();
            float cardBottom = cardTop + cardView.getHeight();

            // Clip the canvas to the card's vertical area to guarantee no spills.
            c.save();
            c.clipRect(itemView.getLeft(), cardTop, itemView.getRight(), cardBottom);

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                boolean isSwipingRight = dX > 0;

                // 1. TRANSPARENCY: Make the card fade to reveal the color underneath.
                cardView.setAlpha(1.0f - Math.abs(dX) / (float) cardView.getWidth());

                // 2. BACKGROUND: Draw the colored, rounded rectangle within the clipped card area.
                float cornerRadius = 24 * context.getResources().getDisplayMetrics().density;
                paint.setColor(ContextCompat.getColor(context, isSwipingRight
                        ? R.color.dialer_call_green               // Green for right swipe
                        : R.color.dialer_end_call_button_color)); // Red for left swipe

                RectF background;
                if (isSwipingRight) {
                    background = new RectF(itemView.getLeft(), cardTop, itemView.getLeft() + dX, cardBottom);
                } else {
                    background = new RectF(itemView.getRight() + dX, cardTop, itemView.getRight(), cardBottom);
                }
                c.drawRoundRect(background, cornerRadius, cornerRadius, paint);

                // 3. ICON: Draw the icon with theme-aware tinting.
                Drawable icon = ContextCompat.getDrawable(context, isSwipingRight
                        ? R.drawable.quantum_ic_access_time_new_vd_theme_24
                        : R.drawable.quantum_ic_delete_vd_theme_24);

                if (icon != null) {
                    // Your requested code for theme-aware tinting
                    TypedValue typedValue = new TypedValue();
                    Resources.Theme theme = context.getTheme();
                    theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
                    int tintColor = ContextCompat.getColor(context, typedValue.resourceId);
                    icon.mutate().setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);

                    int iconSize = icon.getIntrinsicHeight();
                    int iconMargin = (int) ((cardView.getHeight() - iconSize) / 2);
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

            // Restore the canvas to remove the clip.
            c.restore();

            // --- Manual Translation ---
            // We only move the card, leaving the date divider untouched.
            cardView.setTranslationX(dX);
        } else {
            // Use default behavior for other items.
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof CallLogListItemViewHolder) {
            // --- State Restoration ---
            // We now correctly reset the state of the card view itself.
            View cardView = ((CallLogListItemViewHolder) viewHolder).callLogEntryView;
            cardView.setTranslationX(0f);
            cardView.setAlpha(1.0f);
        }
        super.clearView(recyclerView, viewHolder);
    }
}
