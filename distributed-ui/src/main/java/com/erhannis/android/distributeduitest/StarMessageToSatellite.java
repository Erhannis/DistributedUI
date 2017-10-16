package com.erhannis.android.distributeduitest;

/**
 * Created by erhannis on 9/23/17.
 */

public class StarMessageToSatellite extends StarMessage {
  public String satellite;

  public StarMessageToSatellite() {
  }

  public StarMessageToSatellite(String satellite, Object payload) {
    super(payload);
    this.satellite = satellite;
  }
}
