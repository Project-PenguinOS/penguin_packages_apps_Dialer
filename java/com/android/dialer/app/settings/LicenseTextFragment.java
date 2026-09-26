package com.android.dialer.app.settings;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.dialer.R;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LicenseTextFragment extends Fragment {

  static final String ARG_TEXT = "license_text";

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    int padding = Math.round(TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics()));

    TextView text = new TextView(requireContext());
    text.setPadding(padding, padding, padding, padding);
    text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    text.setTextColor(requireContext().getColor(R.color.dialer_label_primary));
    text.setTextIsSelectable(true);
    text.setText(reflow(readText(requireArguments().getInt(ARG_TEXT))));

    ScrollView scroll = new ScrollView(requireContext());
    scroll.setBackgroundColor(requireContext().getColor(R.color.dialer_grouped_background));
    scroll.addView(text);
    return scroll;
  }

  private static String reflow(String text) {
    StringBuilder out = new StringBuilder(text.length());
    for (String line : text.split("\\r?\\n", -1)) {
      String trimmed = line.trim();
      boolean startsBlock = trimmed.isEmpty()
          || trimmed.matches("^(\\d+\\.|\\(\\w\\)|[-*•]).*")
          || trimmed.matches("^[-=_]{3,}$");
      int length = out.length();
      boolean previousEnded = length == 0 || out.charAt(length - 1) == '\n';
      if (startsBlock || previousEnded) {
        if (!previousEnded) {
          out.append('\n');
        }
        out.append(trimmed);
        out.append(trimmed.isEmpty() || trimmed.matches("^[-=_]{3,}$") ? "\n" : "");
      } else {
        out.append(' ').append(trimmed);
      }
    }
    return out.toString().replaceAll("\n{3,}", "\n\n").trim();
  }

  private String readText(int rawRes) {
    try (InputStream in = getResources().openRawResource(rawRes)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      return "";
    }
  }
}
