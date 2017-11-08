package com.erhannis.android.distributedui;

/**
 *
 * An activity hosting a remotable fragment should use this class, for the fragment to call back to it.
 *
 * I rather wish I could use fragment-specific interfaces, but with the DistUI framework interposing,
 * it's looking kinda infeasible.
 *
 * Created by erhannis on 9/16/17.
 */

public interface DistributedUiActivity extends DistributedUiCommunicator {
  public boolean implementsInterface(Class iface);
  public Object onMessage(String method, Object... args);
}
