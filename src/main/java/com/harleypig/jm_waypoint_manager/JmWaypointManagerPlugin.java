package com.harleypig.jm_waypoint_manager;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;

@JourneyMapPlugin(apiVersion = "2.0.0")
public class JmWaypointManagerPlugin implements IClientPlugin {

  private static IClientAPI api;

  @Override
  public void initialize(IClientAPI jmClientApi) {
    api = jmClientApi;

    JmWaypointManager.LOGGER.info("JourneyMap API initialized for {}", JmWaypointManager.MODID);
  }

  @Override
  public String getModId() {
    return JmWaypointManager.MODID;
  }

  public static IClientAPI getApi() {
    return api;
  }

  public static boolean isAvailable() {
    return api != null;
  }
}
