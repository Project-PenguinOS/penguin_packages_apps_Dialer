package com.android.dialer.util;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public final class GlassPanel {

  private final WindowManager windowManager;
  private final View root;
  private final FrameLayout container;
  private final WindowManager.LayoutParams params;
  private final boolean matchWidth;
  private final int marginStart;
  private final int marginEnd;
  private final int marginBottom;
  private final boolean alignEnd;
  private final float translationX;
  private boolean added;

  private GlassPanel(Activity activity, View root, View view) {
    this.windowManager = activity.getWindowManager();
    this.root = root;
    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) view.getLayoutParams();
    matchWidth = lp.width == ViewGroup.LayoutParams.MATCH_PARENT;
    marginStart = lp.getMarginStart();
    marginEnd = lp.getMarginEnd();
    marginBottom = lp.bottomMargin;
    alignEnd = lp.getRules()[RelativeLayout.ALIGN_PARENT_END] != 0;
    translationX = view.getTranslationX();

    ((ViewGroup) view.getParent()).removeView(view);
    view.setTranslationX(0);
    container = new FrameLayout(activity);
    container.addView(
        view,
        new FrameLayout.LayoutParams(
            matchWidth ? ViewGroup.LayoutParams.MATCH_PARENT : lp.width, lp.height));

    params =
        new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT);
    params.setFitInsetsTypes(0);
    params.gravity = Gravity.BOTTOM | (alignEnd ? Gravity.END : Gravity.START);
    params.setTitle("DialerGlassPanel");

    View decor = activity.getWindow().getDecorView();
    root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> update());
    Runnable add =
        () -> {
          params.token = decor.getWindowToken();
          update();
          windowManager.addView(container, params);
          added = true;
        };
    decor.addOnAttachStateChangeListener(
        new View.OnAttachStateChangeListener() {
          @Override
          public void onViewAttachedToWindow(View v) {
            if (!added) {
              add.run();
            }
          }

          @Override
          public void onViewDetachedFromWindow(View v) {
            if (added) {
              windowManager.removeViewImmediate(container);
              added = false;
            }
          }
        });
    if (decor.isAttachedToWindow()) {
      add.run();
    }
  }

  public static GlassPanel host(Activity activity, View root, View view) {
    return new GlassPanel(activity, root, view);
  }

  private void update() {
    int shift = Math.round(alignEnd ? -translationX : translationX);
    int width =
        matchWidth
            ? Math.max(0, root.getWidth() - marginStart - marginEnd)
            : WindowManager.LayoutParams.WRAP_CONTENT;
    int x = (alignEnd ? marginEnd : marginStart) + shift;
    int y = marginBottom + root.getPaddingBottom();
    if ((matchWidth && root.getWidth() == 0)
        || (params.width == width && params.x == x && params.y == y)) {
      return;
    }
    params.width = width;
    params.x = x;
    params.y = y;
    if (added) {
      windowManager.updateViewLayout(container, params);
    }
  }
}
