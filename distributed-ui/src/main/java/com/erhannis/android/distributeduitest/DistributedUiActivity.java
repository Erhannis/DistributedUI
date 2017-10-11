package com.erhannis.android.distributeduitest;

import java.util.Map;

/**
 *
 * An activity hosting a remotable fragment should use this class, for the fragment to call back to it.
 *
 * I rather wish I could use fragment-specific interfaces, but with the DistUI framework interposing,
 * it's looking kinda infeasible.
 *
 * Created by erhannis on 9/16/17.
 */

public interface DistributedUiActivity {
  public boolean implementsInterface(Class iface);
  public void sendToHub(String method, Object... args);
  public Object sendToHubAndWait(String method, Object... args);
  public void sendToSatellites(String method, Object... args);
  public Map<String, Object> sendToSatellitesAndWait(String method, Object... args);
  public void sendToSatellite(String satellite, String method, Object... args);
  public Object sendToSatelliteAndWait(String satellite, String method, Object... args);
  public Object onMessage(String method, Object... args);
}
