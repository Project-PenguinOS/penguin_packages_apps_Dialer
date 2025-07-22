/*
 * Copyright (C) 2023 The LineageOS Project
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

package com.android.dialer.app.calllog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.dialer.R;
import com.android.dialer.widget.SwipeAndDragHelper;

public class CallLogSwipeHelper extends SwipeAndDragHelper {

    private final Drawable swipeBackground;
    private final Paint paint = new Paint();

    public CallLogSwipeHelper(Context context, ActionCompletionContract contract) {
        super(contract);
        swipeBackground = ContextCompat.getDrawable(context, R.drawable.call_log_card_background_middle);
        paint.setColor(ContextCompat.getColor(context, R.color.dialer_end_call_button_color));
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
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            CallLogListItemViewHolder holder = (CallLogListItemViewHolder) viewHolder;
            if (holder.background == null) {
                holder.background = holder.callLogEntryView.getBackground();
            }
            if (dX > 0) {
                holder.callLogEntryView.setBackgroundColor(holder.itemView.getContext().getResources()
                        .getColor(R.color.dialer_call_green));
            } else if (dX < 0) {
                holder.callLogEntryView.setBackgroundColor(holder.itemView.getContext().getResources()
                        .getColor(R.color.dialer_end_call_button_color));
            }
            holder.callLogEntryView.setTranslationX(dX);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}
