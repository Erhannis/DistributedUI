package com.erhannis.android.distributeduitest.starnetwork;

import java.util.ArrayList;
import java.util.LinkedList;

import java8.util.function.Consumer;

/**
 * Suppose you have a number of callbacks waiting for certain conditions to be fulfilled.
 * Add them to this class.
 * When trigger() is called, all callbacks added will be called back (synchronously) via the callerback passed in in the constructor.
 * All subsequent callbacks added will likewise be called back, synchronously in addCallback().
 *
 * Note that all its methods are synchronized.
 *
 * Created by erhannis on 11/7/17.
 */

public class DelayedCallerback<T> {
  protected final Consumer<Consumer<T>> mCallerback;
  protected final LinkedList<Consumer<T>> mCallbacks = new LinkedList<>();
  protected boolean mTriggered = false;

  public DelayedCallerback(Consumer<Consumer<T>> callerback) {
    this.mCallerback = callerback;
  }

  public synchronized void addCallback(Consumer<T> callback) {
    mCallbacks.add(callback);
    checkTriggered();
  }

  public synchronized void trigger() {
    mTriggered = true;
    checkTriggered();
  }

  protected void checkTriggered() {
    if (mTriggered) {
      while (!mCallbacks.isEmpty()) {
        Consumer<T> c = mCallbacks.remove();
        try {
          mCallerback.accept(c);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
}
