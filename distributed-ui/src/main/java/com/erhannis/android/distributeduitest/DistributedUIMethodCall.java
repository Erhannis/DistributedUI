package com.erhannis.android.distributeduitest;

/**
 * Created by erhannis on 9/16/17.
 */

public class DistributedUIMethodCall {
  public String method;
  public Object[] args;

  public DistributedUIMethodCall() {
  }

  public DistributedUIMethodCall(String method, Object[] args) {
    this.method = method;
    this.args = args;
  }
}
