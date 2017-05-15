package com.mehtank.androminion.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.mehtank.androminion.R;

public class WinlossFragment extends Fragment {

  @SuppressWarnings("unused")
  private static final String TAG = "WinlossFragment";

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_winloss, container, false);
  }
}
