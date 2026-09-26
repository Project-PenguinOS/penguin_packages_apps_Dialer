package com.android.dialer.contactsfragment;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.dialer.R;

public final class AlphabetIndexView extends View {

  public interface OnLetterSelectedListener {
    void onLetterSelected(String letter);
  }

  private static final String[] LETTERS = {
    "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
    "T", "U", "V", "W", "X", "Y", "Z", "#",
  };

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final float letterHeight;
  @Nullable private OnLetterSelectedListener listener;
  private int lastIndex = -1;

  public AlphabetIndexView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    float density = getResources().getDisplayMetrics().density;
    letterHeight = 16 * density;
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
    paint.setTypeface(Typeface.create(Typeface.DEFAULT, 600, false));
    paint.setColor(context.getColor(R.color.dialer_label_secondary));
    setContentDescription(null);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  public void setOnLetterSelectedListener(@Nullable OnLetterSelectedListener listener) {
    this.listener = listener;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = Math.round(20 * getResources().getDisplayMetrics().density);
    int height = Math.round(letterHeight * LETTERS.length) + getPaddingTop() + getPaddingBottom();
    setMeasuredDimension(
        resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
  }

  private float top() {
    return (getHeight() - letterHeight * LETTERS.length) / 2f;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    float x = getWidth() / 2f;
    float top = top();
    Paint.FontMetrics metrics = paint.getFontMetrics();
    float baselineOffset = (letterHeight - metrics.ascent - metrics.descent) / 2f;
    for (int i = 0; i < LETTERS.length; i++) {
      canvas.drawText(LETTERS[i], x, top + i * letterHeight + baselineOffset, paint);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        getParent().requestDisallowInterceptTouchEvent(true);
      case MotionEvent.ACTION_MOVE:
        int index = (int) ((event.getY() - top()) / letterHeight);
        index = Math.max(0, Math.min(LETTERS.length - 1, index));
        if (index != lastIndex) {
          lastIndex = index;
          performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
          if (listener != null) {
            listener.onLetterSelected(LETTERS[index]);
          }
        }
        return true;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        lastIndex = -1;
        getParent().requestDisallowInterceptTouchEvent(false);
        return true;
      default:
        return super.onTouchEvent(event);
    }
  }
}
