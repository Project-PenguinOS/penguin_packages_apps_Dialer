package com.android.dialer.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
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
  private static final float BLUR_RADIUS_DP = 4f;

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

  public static boolean apply(View view) {
    if (applied.contains(view)) {
      return true;
    }
    if (!isEnabled(view.getContext())) {
      return false;
    }
    applied.add(view);
    if (view.isAttachedToWindow()) {
      attach(view);
    } else {
      view.addOnAttachStateChangeListener(
          new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
              v.removeOnAttachStateChangeListener(this);
              attach(v);
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

  private static void attach(View view) {
    try {
      Object viewRoot = getViewRootImpl.invoke(view);
      if (viewRoot == null) {
        return;
      }
      Drawable blur = (Drawable) createBlurDrawable.invoke(viewRoot);
      float density = view.getResources().getDisplayMetrics().density;
      setBlurRadius.invoke(blur, Math.round(BLUR_RADIUS_DP * density));
      setColor.invoke(blur, 0);
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
      view.setBackground(new LayerDrawable(new Drawable[] {blur, new RimDrawable(density)}));
      view.setElevation(0);
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

  private static int white(float alpha) {
    return (Math.round(alpha * 255f) << 24) | 0xFFFFFF;
  }

  private static final class RimDrawable extends Drawable {
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF bounds = new RectF();

    RimDrawable(float density) {
      rim.setStyle(Paint.Style.STROKE);
      rim.setStrokeWidth(1.4f * density);
    }

    @Override
    protected void onBoundsChange(android.graphics.Rect rect) {
      bounds.set(rect);
      float radius = bounds.height() / 2f;
      path.reset();
      path.addRoundRect(bounds, radius, radius, Path.Direction.CW);
      rim.setShader(
          new LinearGradient(
              bounds.left,
              bounds.top,
              bounds.right,
              bounds.bottom,
              new int[] {white(0.55f), white(0.08f), 0, white(0.05f), white(0.30f)},
              new float[] {0f, 0.22f, 0.5f, 0.78f, 1f},
              Shader.TileMode.CLAMP));
      sheen.setShader(
          new LinearGradient(
              0f,
              bounds.top,
              0f,
              bounds.top + bounds.height() * 0.3f,
              white(0.05f),
              0,
              Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas canvas) {
      int count = canvas.save();
      canvas.clipPath(path);
      canvas.drawPath(path, rim);
      canvas.drawRect(bounds, sheen);
      canvas.restoreToCount(count);
    }

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(ColorFilter colorFilter) {}

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }
}
