package com.vdom.api;

import java.util.ArrayList;

public class FrameworkEventHelper {

  private static final ArrayList<FrameworkEventListener> listeners = new ArrayList<>();

  public static void addFrameworkListener(FrameworkEventListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  public static void broadcastEvent(FrameworkEvent frameworkEvent) {
    for (FrameworkEventListener frameworkEventListener : listeners) {
      frameworkEventListener.frameworkEvent(frameworkEvent);
    }
  }
}
