package com.harleypig.jm_waypoint_manager;

import net.neoforged.neoforge.common.ModConfigSpec;

public class WaypointManagerConfig {

  private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

  public static final ModConfigSpec.ConfigValue<String> DEFAULT_FILENAME =
      BUILDER
          .comment(
              "Default filename for /wm export and /wm import (without the .json extension).",
              "Leave blank to require an explicit filename in every command.")
          .define("defaultFilename", "");

  public static final ModConfigSpec.BooleanValue DEBUG_MODE =
      BUILDER
          .comment(
              "Write per-waypoint detail to the log file during import and export.",
              "The 'debug' keyword on a command overrides this for that invocation.")
          .define("debugMode", false);

  static final ModConfigSpec SPEC = BUILDER.build();
}
