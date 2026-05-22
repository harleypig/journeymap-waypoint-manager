package com.harleypig.jm_waypoint_manager;

import net.neoforged.neoforge.common.ModConfigSpec;

/** NeoForge CLIENT config spec for the jm_waypoint_manager mod. */
public class WaypointManagerConfig {

  private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

  /** Default filename for export/import when none is given on the command line. */
  public static final ModConfigSpec.ConfigValue<String> DEFAULT_FILENAME =
      BUILDER
          .comment(
              "Default filename used when /wm export or /wm import is run without a filename.",
              "  myfile or myfile.json  =>  use that file (no path separators allowed)",
              "  (blank)               =>  filename is required in every command",
              "Default 'waypoints' means /wm export writes to jm_waypoint_manager/waypoints.json.")
          .define("defaultFilename", "waypoints");

  /** When true, write per-waypoint detail to the log file during import and export. */
  public static final ModConfigSpec.BooleanValue DEBUG_MODE =
      BUILDER
          .comment(
              "Write per-waypoint detail to the log file during import and export.",
              "The 'debug' keyword on a command overrides this for that invocation.")
          .define("debugMode", false);

  static final ModConfigSpec SPEC = BUILDER.build();
}
