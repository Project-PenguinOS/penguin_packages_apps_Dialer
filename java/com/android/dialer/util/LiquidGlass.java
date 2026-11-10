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
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.FrameLayout;
import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class LiquidGlass {

  private static final String SETTING = "penguin_liquid_glass";
  private static final float BLUR_RADIUS_DP = 4f;
  private static final float FALLBACK_BLUR_RADIUS = 2f;
  private static final float BEZEL_DP = 32f;
  private static final float THICKNESS_DP = 30f;
  private static final float DISPERSION = 0.08f;
  private static final float SPECULAR = 0.35f;

  private static final String BEND_SHADER =
      "uniform shader content;\n"
          + "uniform float2 size;\n"
          + "uniform float radius;\n"
          + "uniform float bezel;\n"
          + "uniform float thickness;\n"
          + "uniform float dispersion;\n"
          + "uniform float specular;\n"
          + "float surface(float t) {\n"
          + "  return pow(1.0 - pow(1.0 - t, 4.0), 0.25);\n"
          + "}\n"
          + "float bend(float t) {\n"
          + "  float c = clamp(t, 0.001, 0.999);\n"
          + "  float dt = 0.015625;\n"
          + "  float slope = (surface(min(1.0, c + dt)) - surface(max(0.0, c - dt))) / (2.0 * dt);\n"
          + "  float incident = atan(slope);\n"
          + "  float refracted = asin(sin(incident) / 1.5);\n"
          + "  return min(1.0, surface(c) * tan(incident - refracted) / 0.459);\n"
          + "}\n"
          + "half4 main(float2 xy) {\n"
          + "  float2 half_size = size * 0.5;\n"
          + "  float2 p = xy - half_size;\n"
          + "  float2 q = abs(p) - (half_size - float2(radius));\n"
          + "  float2 s = float2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);\n"
          + "  float inside = 0.0;\n"
          + "  float2 n = float2(0.0);\n"
          + "  if (q.x > 0.0 && q.y > 0.0) {\n"
          + "    float len = max(length(q), 0.0001);\n"
          + "    inside = radius - len;\n"
          + "    n = q / len * s;\n"
          + "  } else if (q.x > q.y) {\n"
          + "    inside = radius - q.x;\n"
          + "    n = float2(s.x, 0.0);\n"
          + "  } else {\n"
          + "    inside = radius - q.y;\n"
          + "    n = float2(0.0, s.y);\n"
          + "  }\n"
          + "  float2 d = float2(0.0);\n"
          + "  float light = 0.0;\n"
          + "  if (inside >= 0.0 && inside < bezel) {\n"
          + "    float t = inside / bezel;\n"
          + "    d = -n * bend(t) * thickness;\n"
          + "    float facing = dot(n, float2(-0.6, -0.8));\n"
          + "    light = pow(1.0 - t, 6.0) * (max(facing, 0.0) + max(-facing, 0.0) * 0.45) * specular;\n"
          + "  }\n"
          + "  half4 r = content.eval(xy + d);\n"
          + "  half4 g = content.eval(xy + d * (1.0 - dispersion));\n"
          + "  half4 b = content.eval(xy + d * (1.0 - 2.0 * dispersion));\n"
          + "  half3 rgb = half3(r.r, g.g, b.b);\n"
          + "  half luma = dot(rgb, half3(0.2126, 0.7152, 0.0722));\n"
          + "  rgb = clamp(mix(half3(luma), rgb, half(1.4)), half3(0.0), half3(1.0));\n"
          + "  rgb += (half3(1.0) - rgb) * half(clamp(light, 0.0, 1.0));\n"
          + "  return half4(rgb, g.a);\n"
          + "}\n";

  private static Method getViewRootImpl;
  private static Method createBlurDrawable;
  private static Method setBlurRadius;
  private static Method setCornerRadius;
  private static Method setColor;
  private static Boolean available;
  private static final Set<View> applied = Collections.newSetFromMap(new WeakHashMap<>());
  private static final Set<View> lensed = Collections.newSetFromMap(new WeakHashMap<>());

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

  public static boolean bends() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
  }

  public static boolean install(BlurView view, BlurTarget target, Drawable frameClear) {
    if (!bends() && apply(view)) {
      return true;
    }
    lens(view, target, frameClear);
    return false;
  }

  private static void lens(BlurView view, BlurTarget target, Drawable frameClear) {
    if (!lensed.add(view)) {
      return;
    }
    view.setBackground(null);
    view.setForeground(new RimDrawable(view.getResources().getDisplayMetrics().density));
    view.setOutlineProvider(
        new ViewOutlineProvider() {
          @Override
          public void getOutline(View v, Outline outline) {
            outline.setRoundRect(
                0, 0, v.getWidth(), v.getHeight(), Math.min(v.getWidth(), v.getHeight()) / 2f);
          }
        });
    view.setClipToOutline(true);
    BlurView backdrop = new BlurView(view.getContext());
    view.addView(backdrop, 0, new FrameLayout.LayoutParams(0, 0));
    backdrop.setupWith(target, 1f, false).setFrameClearDrawable(frameClear).setBlurRadius(FALLBACK_BLUR_RADIUS);
    view.addOnLayoutChangeListener(
        (v, l, t, r, b, ol, ot, or, ob) -> {
          int width = r - l;
          int height = b - t;
          ViewGroup.LayoutParams lp = backdrop.getLayoutParams();
          if (lp.width != width || lp.height != height) {
            lp.width = width;
            lp.height = height;
            backdrop.post(
                () -> {
                  backdrop.setLayoutParams(lp);
                  bend(backdrop, width, height);
                });
          }
        });
  }

  private static void bend(View backdrop, int width, int height) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || width <= 0 || height <= 0) {
      return;
    }
    try {
      float density = backdrop.getResources().getDisplayMetrics().density;
      float half = Math.min(width, height) / 2f;
      RuntimeShader shader = new RuntimeShader(BEND_SHADER);
      shader.setFloatUniform("size", width, height);
      shader.setFloatUniform("radius", half);
      shader.setFloatUniform("bezel", Math.min(BEZEL_DP * density, half));
      shader.setFloatUniform("thickness", THICKNESS_DP * density);
      shader.setFloatUniform("dispersion", DISPERSION);
      shader.setFloatUniform("specular", SPECULAR);
      backdrop.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "content"));
    } catch (RuntimeException e) {
      backdrop.setRenderEffect(null);
    }
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
