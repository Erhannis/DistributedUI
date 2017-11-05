package com.erhannis.android.distributeduitest;

import java.util.Map;

/**
 * Created by erhannis on 11/4/17.
 */

public interface DistributedUiCommunicator {
  public void sendToHub(String method, Object... args);
  public Object sendToHubAndWait(String method, Object... args);
  public void sendToSatellites(String method, Object... args);
  public Map<String, Object> sendToSatellitesAndWait(String method, Object... args);
  public void sendToSatellite(String satellite, String method, Object... args);
  public Object sendToSatelliteAndWait(String satellite, String method, Object... args);
}
