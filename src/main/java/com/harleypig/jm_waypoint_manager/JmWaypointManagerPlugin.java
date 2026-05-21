package com.harleypig.jm_waypoint_manager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;

@JourneyMapPlugin(apiVersion = "2.0.0")
public class JmWaypointManagerPlugin implements IClientPlugin {

  private static IClientAPI api;

  @SuppressFBWarnings(
      value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
      justification =
          "IClientPlugin.initialize() is an instance method by JourneyMap's API contract;"
              + " storing the reference statically is the required plugin pattern")
  @Override
  public void initialize(IClientAPI jmClientApi) {
    api = jmClientApi;

    JmWaypointManager.LOGGER.info("JourneyMap API initialized for {}", JmWaypointManager.MODID);
  }

  @Override
  public String getModId() {
    return JmWaypointManager.MODID;
  }

  @SuppressFBWarnings(
      value = "MS_EXPOSE_REP",
      justification =
          "IClientAPI is an external JourneyMap interface; callers need the live reference"
              + " to call API methods — a defensive copy is neither possible nor meaningful")
  public static IClientAPI getApi() {
    return api;
  }

  public static boolean isAvailable() {
    return api != null;
  }
}
