package com.android.dialer.util;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.provider.Settings;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class LiquidGlass {

  private static final String SETTING = "penguin_liquid_glass";
  private static final float BLUR_RADIUS_DP = 30f;

  private static Method getViewRootImpl;
  private static Method createBlurDrawable;
  private static Method setBlurRadius;
  private static Method setCornerRadius;
  private static Method setColor;
  private static Boolean available;
  private static final Set<View> applied = Collections.newSetFromMap(new WeakHashMap<>());

  private LiquidGlass() {}

  public static boolean isEnabled(Context context) {
    WindowManager wm = context.getSystemService(WindowManager.class);
    return wm != null
        && wm.isCrossWindowBlurEnabled()
        && Settings.Secure.getInt(context.getContentResolver(), SETTING, 0) != 0
        && isAvailable();
  }

  public static boolean apply(View view, Drawable overlay) {
    if (applied.contains(view)) {
      return true;
    }
    if (!isEnabled(view.getContext())) {
      return false;
    }
    applied.add(view);
    if (view.isAttachedToWindow()) {
      attach(view, overlay);
    } else {
      view.addOnAttachStateChangeListener(
          new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
              v.removeOnAttachStateChangeListener(this);
              attach(v, overlay);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {}
          });
    }
    return true;
  }

  private static synchronized boolean isAvailable() {
    if (available == null) {
      try {
        getViewRootImpl = View.class.getDeclaredMethod("getViewRootImpl");
        Class<?> viewRoot = Class.forName("android.view.ViewRootImpl");
        createBlurDrawable = viewRoot.getDeclaredMethod("createBackgroundBlurDrawable");
        Class<?> blur = createBlurDrawable.getReturnType();
        setBlurRadius = blur.getDeclaredMethod("setBlurRadius", int.class);
        setCornerRadius = blur.getDeclaredMethod("setCornerRadius", float.class);
        setColor = blur.getDeclaredMethod("setColor", int.class);
        available = true;
      } catch (ReflectiveOperationException | RuntimeException e) {
        available = false;
      }
    }
    return available;
  }

  private static void attach(View view, Drawable overlay) {
    try {
      Object viewRoot = getViewRootImpl.invoke(view);
      if (viewRoot == null) {
        return;
      }
      Drawable blur = (Drawable) createBlurDrawable.invoke(viewRoot);
      float density = view.getResources().getDisplayMetrics().density;
      setBlurRadius.invoke(blur, Math.round(BLUR_RADIUS_DP * density));
      setColor.invoke(blur, tint(view.getContext()));
      setCornerRadius.invoke(blur, view.getHeight() / 2f);
      view.addOnLayoutChangeListener(
          (v, l, t, r, b, ol, ot, or, ob) -> {
            if (b - t != ob - ot) {
              try {
                setCornerRadius.invoke(blur, (b - t) / 2f);
              } catch (ReflectiveOperationException e) {
                return;
              }
            }
          });
      view.setBackground(
          overlay == null ? blur : new LayerDrawable(new Drawable[] {blur, overlay}));
      view.setOutlineProvider(
          new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
              outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), v.getHeight() / 2f);
            }
          });
      view.setClipToOutline(true);
    } catch (ReflectiveOperationException | RuntimeException e) {
      available = false;
    }
  }

  private static int tint(Context context) {
    boolean night =
        (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES;
    return night ? 0x1AFFFFFF : 0x40FFFFFF;
  }
}
