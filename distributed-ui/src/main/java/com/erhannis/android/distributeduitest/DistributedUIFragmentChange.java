package com.erhannis.android.distributeduitest;

import android.support.v4.app.Fragment;

/**
 * Created by erhannis on 9/16/17.
 */

public class DistributedUIFragmentChange {
  public String target;
  public FragmentHandle fragment;

  public DistributedUIFragmentChange() {
  }

  public DistributedUIFragmentChange(String target, FragmentHandle fragment) {
    this.target = target;
    this.fragment = fragment;
  }
}
