package com.android.dialer.widget;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.dialer.R;

public final class HigNavigationBar extends FrameLayout {

  public static final String EXTRA_BACK_LABEL = "com.android.dialer.widget.EXTRA_BACK_LABEL";

  private TextView title;
  private TextView backLabel;
  private View back;

  public HigNavigationBar(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    title = findViewById(R.id.hig_nav_title);
    backLabel = findViewById(R.id.hig_nav_back_label);
    back = findViewById(R.id.hig_nav_back);
  }

  public void setTitle(@Nullable CharSequence text) {
    title.setText(text);
  }

  public void setBackLabel(@Nullable CharSequence text) {
    CharSequence label =
        text == null || text.length() == 0 ? getContext().getText(R.string.hig_navigation_back) : text;
    backLabel.setText(label);
    back.setContentDescription(label);
  }

  public void setOnBackClickListener(OnClickListener listener) {
    back.setOnClickListener(listener);
  }

  @Nullable
  public static HigNavigationBar installInActionBar(AppCompatActivity activity) {
    ActionBar actionBar = activity.getSupportActionBar();
    if (actionBar == null) {
      return null;
    }
    HigNavigationBar bar = inflate(activity, null);
    actionBar.setDisplayShowTitleEnabled(false);
    actionBar.setDisplayHomeAsUpEnabled(false);
    actionBar.setDisplayShowHomeEnabled(false);
    actionBar.setDisplayShowCustomEnabled(true);
    actionBar.setCustomView(
        bar,
        new ActionBar.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    if (bar.getParent() instanceof Toolbar) {
      ((Toolbar) bar.getParent()).setContentInsetsAbsolute(0, 0);
    }
    bar.bind(activity);
    return bar;
  }

  public static HigNavigationBar installInToolbar(Activity activity, Toolbar toolbar) {
    toolbar.setNavigationIcon(null);
    toolbar.setTitle(null);
    toolbar.setContentInsetsAbsolute(0, 0);
    HigNavigationBar bar = inflate(activity, toolbar);
    toolbar.addView(
        bar,
        new Toolbar.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    bar.bind(activity);
    return bar;
  }

  private static HigNavigationBar inflate(Activity activity, @Nullable ViewGroup parent) {
    return (HigNavigationBar)
        LayoutInflater.from(activity).inflate(R.layout.hig_navigation_bar, parent, false);
  }

  private void bind(Activity activity) {
    setTitle(activity.getTitle());
    setBackLabel(activity.getIntent().getCharSequenceExtra(EXTRA_BACK_LABEL));
    setOnBackClickListener(v -> activity.onBackPressed());
  }
}
