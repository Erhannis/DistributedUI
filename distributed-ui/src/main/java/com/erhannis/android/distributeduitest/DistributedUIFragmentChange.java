package com.erhannis.android.distributeduitest;

import android.support.v4.app.Fragment;

/**
 * Created by erhannis on 9/16/17.
 */

public class DistributedUIFragmentChange {
  public static enum ChangeType {
    HOST_FRAGMENT, DROP_FRAGMENT;
  }

  public ChangeType type;
  public FragmentHandle fragment;

  public DistributedUIFragmentChange() {
  }

  public DistributedUIFragmentChange(ChangeType type, FragmentHandle fragment) {
    this.type = type;
    this.fragment = fragment;
  }
}
