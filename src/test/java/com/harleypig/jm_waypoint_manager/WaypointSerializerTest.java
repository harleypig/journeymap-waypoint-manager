package com.harleypig.jm_waypoint_manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import journeymap.api.v2.common.waypoint.WaypointGroup;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class WaypointSerializerTest {

  private static final String MOD_ID = "jm_waypoint_manager";

  private static final String DIM = "minecraft:overworld";

  @Test
  void toJson_singleWaypoint_ungrouped() {
    IClientAPI api = mock(IClientAPI.class);

    when(api.getAllWaypointGroups()).thenReturn(new ArrayList<>());

    Waypoint wp = mock(Waypoint.class);

    when(wp.getPrimaryDimension()).thenReturn(DIM);
    when(wp.getGroupId()).thenReturn("");
    when(wp.getGuid()).thenReturn("test-guid");
    when(wp.getName()).thenReturn("Home");
    when(wp.getX()).thenReturn(10);
    when(wp.getY()).thenReturn(64);
    when(wp.getZ()).thenReturn(-20);
    when(wp.getRed()).thenReturn(255);
    when(wp.getGreen()).thenReturn(0);
    when(wp.getBlue()).thenReturn(0);
    when(wp.isEnabled()).thenReturn(true);
    when(wp.getDescription()).thenReturn(null);
    when(wp.getDimensions()).thenReturn(new TreeSet<>(List.of(DIM)));

    JsonObject root = WaypointSerializer.toJson(List.of(wp), api);

    assertTrue(root.has(DIM));

    JsonObject dimObj = root.getAsJsonObject(DIM);

    assertTrue(dimObj.has(""));

    var arr = dimObj.getAsJsonArray("");

    assertEquals(1, arr.size());

    JsonObject entry = arr.get(0).getAsJsonObject();

    assertEquals("test-guid", entry.get("guid").getAsString());
    assertEquals("Home", entry.get("name").getAsString());
    assertEquals(10, entry.get("x").getAsInt());
    assertEquals(64, entry.get("y").getAsInt());
    assertEquals(-20, entry.get("z").getAsInt());
    assertTrue(entry.get("enabled").getAsBoolean());
    assertFalse(entry.has("description"));
  }

  @Test
  void toJson_waypointWithGroup_usesGroupName() {
    WaypointGroup group = mock(WaypointGroup.class);

    when(group.getGuid()).thenReturn("group-guid");
    when(group.getName()).thenReturn("Bases");

    IClientAPI api = mock(IClientAPI.class);

    doReturn(List.of(group)).when(api).getAllWaypointGroups();

    Waypoint wp = mock(Waypoint.class);

    when(wp.getPrimaryDimension()).thenReturn(DIM);
    when(wp.getGroupId()).thenReturn("group-guid");
    when(wp.getGuid()).thenReturn("wp-guid");
    when(wp.getName()).thenReturn("Base 1");
    when(wp.getX()).thenReturn(0);
    when(wp.getY()).thenReturn(64);
    when(wp.getZ()).thenReturn(0);
    when(wp.getRed()).thenReturn(0);
    when(wp.getGreen()).thenReturn(255);
    when(wp.getBlue()).thenReturn(0);
    when(wp.isEnabled()).thenReturn(true);
    when(wp.getDescription()).thenReturn(null);
    when(wp.getDimensions()).thenReturn(new TreeSet<>(List.of(DIM)));

    JsonObject root = WaypointSerializer.toJson(List.of(wp), api);

    JsonObject dimObj = root.getAsJsonObject(DIM);

    assertTrue(dimObj.has("Bases"));
    assertEquals(1, dimObj.getAsJsonArray("Bases").size());
  }

  @Test
  void fromJson_newWaypoint_isAdded() {
    IClientAPI api = mock(IClientAPI.class);

    when(api.getWaypoints(MOD_ID)).thenReturn(new ArrayList<>());
    when(api.getAllWaypoints()).thenReturn(new ArrayList<>());

    Waypoint created = mock(Waypoint.class);

    when(created.getGuid()).thenReturn("new-guid");

    String json =
        """
        {
          "minecraft:overworld": {
            "": [
              {
                "name": "Spawn",
                "x": 0, "y": 64, "z": 0,
                "r": 255, "g": 0, "b": 0,
                "enabled": true,
                "primaryDimension": "minecraft:overworld",
                "dimensions": ["minecraft:overworld"]
              }
            ]
          }
        }
        """;

    JsonObject root = WaypointSerializer.fromJsonString(json);

    try (MockedStatic<WaypointFactory> factory = mockStatic(WaypointFactory.class)) {
      factory
          .when(
              () ->
                  WaypointFactory.createWaypoint(
                      eq(MOD_ID), any(BlockPos.class), eq("Spawn"), eq(DIM), eq(true)))
          .thenReturn(created);

      int[] result = WaypointSerializer.fromJson(root, api, MOD_ID);

      assertEquals(1, result[0]);
      assertEquals(0, result[1]);
      verify(api).addWaypoint(MOD_ID, created);
    }
  }

  @Test
  void fromJson_guidDuplicate_isSkipped() {
    Waypoint existing = mock(Waypoint.class);

    when(existing.getGuid()).thenReturn("dup-guid");
    when(existing.getName()).thenReturn("Spawn");
    when(existing.getX()).thenReturn(0);
    when(existing.getY()).thenReturn(64);
    when(existing.getZ()).thenReturn(0);
    when(existing.getPrimaryDimension()).thenReturn(DIM);

    IClientAPI api = mock(IClientAPI.class);

    doReturn(List.of(existing)).when(api).getWaypoints(MOD_ID);
    doReturn(List.of(existing)).when(api).getAllWaypoints();

    String json =
        """
        {
          "minecraft:overworld": {
            "": [
              {
                "guid": "dup-guid",
                "name": "Spawn",
                "x": 0, "y": 64, "z": 0,
                "r": 255, "g": 0, "b": 0,
                "enabled": true,
                "primaryDimension": "minecraft:overworld",
                "dimensions": ["minecraft:overworld"]
              }
            ]
          }
        }
        """;

    JsonObject root = WaypointSerializer.fromJsonString(json);

    int[] result = WaypointSerializer.fromJson(root, api, MOD_ID);

    assertEquals(0, result[0]);
    assertEquals(1, result[1]);
    verify(api, never()).addWaypoint(any(), any());
  }

  @Test
  void fromJson_positionalDuplicate_isSkipped() {
    Waypoint existing = mock(Waypoint.class);

    when(existing.getGuid()).thenReturn("other-guid");
    when(existing.getName()).thenReturn("Spawn");
    when(existing.getX()).thenReturn(0);
    when(existing.getY()).thenReturn(64);
    when(existing.getZ()).thenReturn(0);
    when(existing.getPrimaryDimension()).thenReturn(DIM);

    IClientAPI api = mock(IClientAPI.class);

    // Not owned by this mod, but exists positionally
    when(api.getWaypoints(MOD_ID)).thenReturn(new ArrayList<>());
    doReturn(List.of(existing)).when(api).getAllWaypoints();

    String json =
        """
        {
          "minecraft:overworld": {
            "": [
              {
                "name": "Spawn",
                "x": 0, "y": 64, "z": 0,
                "r": 255, "g": 0, "b": 0,
                "enabled": true,
                "primaryDimension": "minecraft:overworld",
                "dimensions": ["minecraft:overworld"]
              }
            ]
          }
        }
        """;

    JsonObject root = WaypointSerializer.fromJsonString(json);

    int[] result = WaypointSerializer.fromJson(root, api, MOD_ID);

    assertEquals(0, result[0]);
    assertEquals(1, result[1]);
  }

  @Test
  void fromJson_unknownGroup_isCreated() {
    IClientAPI api = mock(IClientAPI.class);

    when(api.getWaypoints(MOD_ID)).thenReturn(new ArrayList<>());
    when(api.getAllWaypoints()).thenReturn(new ArrayList<>());
    when(api.getWaypointGroupByName(MOD_ID, "Bases")).thenReturn(null);

    WaypointGroup newGroup = mock(WaypointGroup.class);

    Waypoint created = mock(Waypoint.class);

    when(created.getGuid()).thenReturn("new-guid");

    String json =
        """
        {
          "minecraft:overworld": {
            "Bases": [
              {
                "name": "Base",
                "x": 100, "y": 64, "z": 100,
                "r": 0, "g": 255, "b": 0,
                "enabled": true,
                "primaryDimension": "minecraft:overworld",
                "dimensions": ["minecraft:overworld"]
              }
            ]
          }
        }
        """;

    JsonObject root = WaypointSerializer.fromJsonString(json);

    try (MockedStatic<WaypointFactory> factory = mockStatic(WaypointFactory.class)) {
      factory.when(() -> WaypointFactory.createWaypointGroup(MOD_ID, "Bases")).thenReturn(newGroup);

      factory
          .when(
              () ->
                  WaypointFactory.createWaypoint(
                      eq(MOD_ID), any(BlockPos.class), eq("Base"), eq(DIM), eq(true)))
          .thenReturn(created);

      int[] result = WaypointSerializer.fromJson(root, api, MOD_ID);

      assertEquals(1, result[0]);
      verify(newGroup).setPersistent(true);
      verify(api).addWaypointGroup(newGroup);
      verify(newGroup).addWaypoint(created);
    }
  }

  @Test
  void fromJson_existingGroup_isReused() {
    WaypointGroup existingGroup = mock(WaypointGroup.class);

    IClientAPI api = mock(IClientAPI.class);

    when(api.getWaypoints(MOD_ID)).thenReturn(new ArrayList<>());
    when(api.getAllWaypoints()).thenReturn(new ArrayList<>());
    when(api.getWaypointGroupByName(MOD_ID, "Bases")).thenReturn(existingGroup);

    Waypoint created = mock(Waypoint.class);

    when(created.getGuid()).thenReturn("new-guid");

    String json =
        """
        {
          "minecraft:overworld": {
            "Bases": [
              {
                "name": "Base",
                "x": 100, "y": 64, "z": 100,
                "r": 0, "g": 255, "b": 0,
                "enabled": true,
                "primaryDimension": "minecraft:overworld",
                "dimensions": ["minecraft:overworld"]
              }
            ]
          }
        }
        """;

    JsonObject root = WaypointSerializer.fromJsonString(json);

    try (MockedStatic<WaypointFactory> factory = mockStatic(WaypointFactory.class)) {
      factory
          .when(
              () ->
                  WaypointFactory.createWaypoint(
                      eq(MOD_ID), any(BlockPos.class), eq("Base"), eq(DIM), eq(true)))
          .thenReturn(created);

      int[] result = WaypointSerializer.fromJson(root, api, MOD_ID);

      assertEquals(1, result[0]);
      verify(api, never()).addWaypointGroup(any());
      verify(existingGroup).addWaypoint(created);
    }
  }
}
