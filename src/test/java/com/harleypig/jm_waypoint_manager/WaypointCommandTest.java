package com.harleypig.jm_waypoint_manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class WaypointCommandTest {

  // --- resolveFilename ---

  @Test
  void resolveFilename_commandFilenameProvided_returnsCommandFilename() {
    assertEquals("myfile", WaypointCommand.resolveFilename("myfile", "configdefault"));
  }

  @Test
  void resolveFilename_commandFilenameWithJson_returnsAsIs() {
    assertEquals("myfile.json", WaypointCommand.resolveFilename("myfile.json", "configdefault"));
  }

  @Test
  void resolveFilename_nullCommand_returnsConfigDefault() {
    assertEquals("waypoints", WaypointCommand.resolveFilename(null, "waypoints"));
  }

  @Test
  void resolveFilename_emptyCommand_returnsConfigDefault() {
    assertEquals("waypoints", WaypointCommand.resolveFilename("", "waypoints"));
  }

  @Test
  void resolveFilename_nullCommandBlankConfig_returnsNull() {
    assertNull(WaypointCommand.resolveFilename(null, ""));
  }

  @Test
  void resolveFilename_emptyCommandBlankConfig_returnsNull() {
    assertNull(WaypointCommand.resolveFilename("", ""));
  }

  @Test
  void resolveFilename_commandOverridesConfig() {
    assertEquals("override", WaypointCommand.resolveFilename("override", "waypoints"));
  }

  // --- validateFilename ---

  @Test
  void validateFilename_clean_returnsNull() {
    assertNull(WaypointCommand.validateFilename("waypoints"));
  }

  @Test
  void validateFilename_withJsonExtension_returnsNull() {
    assertNull(WaypointCommand.validateFilename("waypoints.json"));
  }

  @Test
  void validateFilename_forwardSlash_returnsError() {
    assertNotNull(WaypointCommand.validateFilename("path/to/file"));
  }

  @Test
  void validateFilename_backslash_returnsError() {
    assertNotNull(WaypointCommand.validateFilename("path\\to\\file"));
  }

  @Test
  void validateFilename_dotDot_returnsNull() {
    // ".." alone has no path separator — callers control traversal via resolveFile.
    assertNull(WaypointCommand.validateFilename(".."));
  }
}
