package com.mehtank.androminion.util;

import android.widget.Checkable;

public interface CheckableEx extends Checkable {

  void setChecked(boolean arg0, String indicator);

  void setChecked(boolean arg0, int order, String indicator);
}
