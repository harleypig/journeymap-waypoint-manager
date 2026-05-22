package com.harleypig.jm_waypoint_manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import journeymap.api.v2.common.waypoint.WaypointGroup;
import net.minecraft.core.BlockPos;

public class WaypointSerializer {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  /** Serializes waypoints into a JSON object grouped by primary dimension then group name. */
  public static JsonObject toJson(Iterable<? extends Waypoint> waypoints, IClientAPI api) {
    List<? extends WaypointGroup> allGroups = api.getAllWaypointGroups();
    Map<String, String> guidToName = new HashMap<>(allGroups.size() * 2);
    for (WaypointGroup group : allGroups) {
      guidToName.put(group.getGuid(), group.getName());
    }

    Map<String, Map<String, List<Waypoint>>> byDimGroup = new LinkedHashMap<>();
    for (Waypoint wp : waypoints) {
      String dim = wp.getPrimaryDimension();
      String groupName = guidToName.getOrDefault(wp.getGroupId(), "");
      byDimGroup
          .computeIfAbsent(dim, k -> new LinkedHashMap<>())
          .computeIfAbsent(groupName, k -> new ArrayList<>())
          .add(wp);
    }

    JsonObject root = new JsonObject();
    for (Map.Entry<String, Map<String, List<Waypoint>>> dimEntry : byDimGroup.entrySet()) {
      JsonObject dimObj = new JsonObject();
      for (Map.Entry<String, List<Waypoint>> groupEntry : dimEntry.getValue().entrySet()) {
        JsonArray arr = new JsonArray();
        for (Waypoint wp : groupEntry.getValue()) {
          arr.add(serializeWaypoint(wp));
        }
        dimObj.add(groupEntry.getKey(), arr);
      }
      root.add(dimEntry.getKey(), dimObj);
    }
    return root;
  }

  private static JsonObject serializeWaypoint(Waypoint wp) {
    JsonObject obj = new JsonObject();
    obj.addProperty("guid", wp.getGuid());
    obj.addProperty("name", wp.getName());
    obj.addProperty("x", wp.getX());
    obj.addProperty("y", wp.getY());
    obj.addProperty("z", wp.getZ());
    obj.addProperty("r", wp.getRed());
    obj.addProperty("g", wp.getGreen());
    obj.addProperty("b", wp.getBlue());
    obj.addProperty("enabled", wp.isEnabled());
    obj.addProperty("primaryDimension", wp.getPrimaryDimension());

    if (wp.getDescription() != null) {
      obj.addProperty("description", wp.getDescription());
    }

    JsonArray dims = new JsonArray();
    for (String dim : wp.getDimensions()) {
      dims.add(dim);
    }
    obj.add("dimensions", dims);

    return obj;
  }

  /** Formats a JSON object as a pretty-printed string. */
  public static String toJsonString(JsonObject json) {
    return GSON.toJson(json);
  }

  /** Parses a JSON string into a root object for use with {@link #fromJson}. */
  public static JsonObject fromJsonString(String json) {
    return JsonParser.parseString(json).getAsJsonObject();
  }

  /** Imports waypoints from a parsed JSON root; returns {@code int[]{added, skipped}}. */
  public static int[] fromJson(
      JsonObject root, IClientAPI api, String modId, Consumer<String> debugLog) {
    // GUID set for waypoints we own — primary duplicate check for round-trips.
    List<? extends Waypoint> ownedList = api.getWaypoints(modId);
    Set<String> ownedGuids = new HashSet<>(ownedList.size() * 2);
    for (Waypoint wp : ownedList) {
      ownedGuids.add(wp.getGuid());
    }

    // Name+pos+dim set across all waypoints — fallback for user-created
    // waypoints (owned by "journeymap", not our modId).
    List<? extends Waypoint> allList = api.getAllWaypoints();
    Set<String> allByPosition = new HashSet<>(allList.size() * 2);
    for (Waypoint wp : allList) {
      allByPosition.add(
          dupKey(wp.getName(), wp.getX(), wp.getY(), wp.getZ(), wp.getPrimaryDimension()));
    }

    int added = 0;
    int skipped = 0;

    for (Map.Entry<String, JsonElement> dimEntry : root.entrySet()) {
      String primaryDim = dimEntry.getKey();
      JsonObject dimObj = dimEntry.getValue().getAsJsonObject();

      for (Map.Entry<String, JsonElement> groupEntry : dimObj.entrySet()) {
        String groupName = groupEntry.getKey();
        WaypointGroup group = resolveGroup(api, modId, groupName);

        for (JsonElement el : groupEntry.getValue().getAsJsonArray()) {
          JsonObject obj = el.getAsJsonObject();
          String name = obj.get("name").getAsString();
          int x = obj.get("x").getAsInt();
          int y = obj.get("y").getAsInt();
          int z = obj.get("z").getAsInt();

          // GUID check first (exact round-trip match for our waypoints).
          String guid = obj.has("guid") ? obj.get("guid").getAsString() : null;
          if (guid != null && ownedGuids.contains(guid)) {
            debugLog.accept(
                "  skip (guid) " + name + " (" + x + ", " + y + ", " + z + ") in " + primaryDim);
            skipped++;
            continue;
          }

          // Positional check catches user waypoints and import-from-JM exports.
          String posKey = dupKey(name, x, y, z, primaryDim);
          if (allByPosition.contains(posKey)) {
            debugLog.accept(
                "  skip (pos)  " + name + " (" + x + ", " + y + ", " + z + ") in " + primaryDim);
            skipped++;
            continue;
          }

          Waypoint wp =
              WaypointFactory.createWaypoint(modId, new BlockPos(x, y, z), name, primaryDim, true);

          if (obj.has("r")) {
            wp.setRed(obj.get("r").getAsInt());
          }
          if (obj.has("g")) {
            wp.setGreen(obj.get("g").getAsInt());
          }
          if (obj.has("b")) {
            wp.setBlue(obj.get("b").getAsInt());
          }
          if (obj.has("enabled")) {
            wp.setEnabled(obj.get("enabled").getAsBoolean());
          }
          if (obj.has("description")) {
            wp.setDescription(obj.get("description").getAsString());
          }
          if (obj.has("dimensions")) {
            List<String> dims = new ArrayList<>();
            for (JsonElement d : obj.get("dimensions").getAsJsonArray()) {
              dims.add(d.getAsString());
            }
            wp.setDimensions(dims);
          }

          if (group != null) {
            group.addWaypoint(wp);
          }
          api.addWaypoint(modId, wp);
          ownedGuids.add(wp.getGuid());
          allByPosition.add(posKey);
          debugLog.accept(
              "  add         " + name + " (" + x + ", " + y + ", " + z + ") in " + primaryDim);
          added++;
        }
      }
    }

    return new int[] {added, skipped};
  }

  // Finds an existing group by name under modId, or creates and registers a new one.
  // Returns null for the unnamed default group (empty string key).
  @Nullable
  private static WaypointGroup resolveGroup(IClientAPI api, String modId, String name) {
    if (name.isEmpty()) {
      return null;
    }
    WaypointGroup group = api.getWaypointGroupByName(modId, name);
    if (group != null) {
      return group;
    }
    group = WaypointFactory.createWaypointGroup(modId, name);
    group.setPersistent(true);
    api.addWaypointGroup(group);
    return group;
  }

  private static String dupKey(String name, int x, int y, int z, String dim) {
    return name + '\0' + x + '\0' + y + '\0' + z + '\0' + dim;
  }
}
