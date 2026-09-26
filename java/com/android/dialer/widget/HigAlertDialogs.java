package com.android.dialer.widget;

import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.dialer.R;

public final class HigAlertDialogs {

  private HigAlertDialogs() {}

  public static AlertDialog showConfirmation(AlertDialog.Builder builder, boolean destructive) {
    AlertDialog dialog = builder.create();
    dialog.setOnShowListener(d -> style(dialog, destructive));
    dialog.show();
    return dialog;
  }

  private static void style(AlertDialog dialog, boolean destructive) {
    TextView title = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
    if (title != null) {
      title.setGravity(Gravity.CENTER_HORIZONTAL);
      title.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
    }
    TextView message = dialog.findViewById(android.R.id.message);
    if (message != null) {
      message.setGravity(Gravity.CENTER_HORIZONTAL);
      message.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
    }

    Button action = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
    if (action != null && destructive) {
      action.setTextColor(dialog.getContext().getColor(R.color.dialer_system_red));
    }
    Button cancel = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
    if (cancel != null) {
      cancel.setTypeface(Typeface.DEFAULT_BOLD);
    }
  }
}
