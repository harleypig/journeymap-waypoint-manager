package com.harleypig.jm_waypoint_manager;

import net.neoforged.neoforge.common.ModConfigSpec;

public class WaypointManagerConfig {

  private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

  public static final ModConfigSpec.ConfigValue<String> DEFAULT_FILENAME =
      BUILDER
          .comment(
              "Default filename used when /wm export or /wm import is run without a filename.",
              "  myfile or myfile.json  =>  use that file (no path separators allowed)",
              "  (blank)               =>  filename is required in every command",
              "Default 'waypoints' means /wm export writes to jm_waypoint_manager/waypoints.json.")
          .define("defaultFilename", "waypoints");

  public static final ModConfigSpec.BooleanValue DEBUG_MODE =
      BUILDER
          .comment(
              "Write per-waypoint detail to the log file during import and export.",
              "The 'debug' keyword on a command overrides this for that invocation.")
          .define("debugMode", false);

  static final ModConfigSpec SPEC = BUILDER.build();
}
