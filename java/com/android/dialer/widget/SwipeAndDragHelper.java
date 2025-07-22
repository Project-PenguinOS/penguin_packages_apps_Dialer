package com.android.dialer.widget;

import android.graphics.Canvas;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.android.dialer.R;
import com.android.dialer.app.calllog.CallLogListItemViewHolder;

public class SwipeAndDragHelper extends ItemTouchHelper.Callback {

    private static final String TAG = "SwipeAndDragHelper";
    private ActionCompletionContract contract;

    public SwipeAndDragHelper(ActionCompletionContract contract) {
        this.contract = contract;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = 0;
        int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        contract.onViewSwiped(viewHolder.getAdapterPosition());
        contract.onRestoreInstanceState(viewHolder);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            CallLogListItemViewHolder holder = (CallLogListItemViewHolder) viewHolder;
            if (holder.background == null) {
                holder.background = holder.callLogEntryView.getBackground();
            }
            holder.callLogEntryView.setBackgroundColor(holder.itemView.getContext().getResources()
                    .getColor(R.color.dialer_call_green));
            holder.callLogEntryView.setTranslationX(dX);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        CallLogListItemViewHolder holder = (CallLogListItemViewHolder) viewHolder;
        holder.callLogEntryView.setTranslationX(0f);
        if (holder.background != null) {
            holder.callLogEntryView.setBackground(holder.background);
            holder.background = null;
        }
        contract.onRestoreInstanceState(viewHolder);
    }

    public interface ActionCompletionContract {
        void onViewSwiped(int position);
        void onRestoreInstanceState(RecyclerView.ViewHolder viewHolder);
    }
}