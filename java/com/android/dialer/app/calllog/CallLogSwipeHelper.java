package com.android.dialer.app.calllog;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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

    private static final float CARD_CORNER_RADIUS_DP = 32f;
    private static final float CARD_INNER_CORNER_RADIUS_DP = 1f;

    private static final float SWIPE_THRESHOLD = 0.4f;
    private static final float SWIPE_ESCAPE_VELOCITY_MULTIPLIER = 1.5f;
    private static final float MIN_SWIPE_ALPHA = 0.4f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF background = new RectF();
    private final Path backgroundPath = new Path();
    private final float[] radii = new float[8];

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
    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
        return SWIPE_THRESHOLD;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return defaultValue * SWIPE_ESCAPE_VELOCITY_MULTIPLIER;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        if (!(viewHolder instanceof CallLogListItemViewHolder)) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }

        CallLogListItemViewHolder holder = (CallLogListItemViewHolder) viewHolder;
        View itemView = holder.itemView;
        View cardView = holder.callLogEntryView;
        Context context = cardView.getContext();

        float cardLeft = itemView.getLeft() + cardView.getLeft();
        float cardTop = itemView.getTop() + cardView.getTop();
        float cardRight = cardLeft + cardView.getWidth();
        float cardBottom = cardTop + cardView.getHeight();

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0) {
            c.save();
            c.clipRect(cardLeft, cardTop, cardRight, cardBottom);

            boolean isSwipingRight = dX > 0;
            paint.setColor(ContextCompat.getColor(context, isSwipingRight
                    ? R.color.dialer_call_green
                    : R.color.dialer_end_call_button_color));

            background.set(cardLeft, cardTop, cardRight, cardBottom);
            setCornerRadii(context, holder);
            backgroundPath.rewind();
            backgroundPath.addRoundRect(background, radii, Path.Direction.CW);
            c.drawPath(backgroundPath, paint);

            drawActionIcon(c, context, cardView, itemView, cardTop, dX, isSwipingRight);

            c.restore();
        }

        float progress = Math.min(1f, Math.abs(dX) / (float) cardView.getWidth());
        cardView.setAlpha(1f - (1f - MIN_SWIPE_ALPHA) * progress);
        cardView.setTranslationX(dX);
    }

    private void setCornerRadii(Context context, CallLogListItemViewHolder holder) {
        float outer = dpToPx(context, CARD_CORNER_RADIUS_DP);
        float inner = dpToPx(context, CARD_INNER_CORNER_RADIUS_DP);

        float top = holder.isFirstInDateGroup ? outer : inner;
        float bottom = holder.isLastInDateGroup ? outer : inner;

        radii[0] = radii[1] = top;
        radii[2] = radii[3] = top;
        radii[4] = radii[5] = bottom;
        radii[6] = radii[7] = bottom;
    }

    private void drawActionIcon(Canvas c, Context context, View cardView, View itemView,
                                float cardTop, float dX, boolean isSwipingRight) {
        Drawable icon = ContextCompat.getDrawable(context, isSwipingRight
                ? R.drawable.quantum_ic_call_vd_theme_24
                : R.drawable.quantum_ic_delete_vd_theme_24);
        if (icon == null) {
            return;
        }

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        int tintColor = ContextCompat.getColor(context, typedValue.resourceId);
        icon = icon.mutate();
        icon.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);

        int iconSize = icon.getIntrinsicHeight();
        int iconMargin = (cardView.getHeight() - iconSize) / 2;
        int iconAreaWidth = iconSize + (2 * iconMargin);

        float reveal = Math.min(1f, Math.abs(dX) / (float) Math.max(1, iconAreaWidth));
        icon.setAlpha((int) (reveal * 255));

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

    private static float dpToPx(Context context, float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
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
