package com.harleypig.jm_waypoint_manager;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.common.waypoint.Waypoint;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class WaypointCommand {

  public static void onRegisterCommands(RegisterCommandsEvent event) {
    register(event.getDispatcher(), event.getBuildContext());
  }

  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
    dispatcher.register(
        Commands.literal("wm")
            .then(
                Commands.literal("export")
                    .executes(ctx -> doExport(ctx, "waypoints"))
                    .then(
                        Commands.argument("filename", StringArgumentType.string())
                            .executes(
                                ctx ->
                                    doExport(ctx, StringArgumentType.getString(ctx, "filename")))))
            .then(
                Commands.literal("import")
                    .then(
                        Commands.argument("filename", StringArgumentType.string())
                            .executes(
                                ctx ->
                                    doImport(
                                        ctx, StringArgumentType.getString(ctx, "filename"))))));
  }

  @SuppressFBWarnings(
      value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "CRLF_INJECTION_LOGS"},
      justification =
          "getParent() is non-null: resolveFile returns a 3-component path;"
              + " CRLF stripped from filename in resolveFile() before path construction")
  private static int doExport(CommandContext<CommandSourceStack> ctx, String filename) {
    if (!JmWaypointManagerPlugin.isAvailable()) {
      ctx.getSource().sendFailure(Component.literal("JourneyMap is not loaded."));
      return 0;
    }

    IClientAPI api = JmWaypointManagerPlugin.getApi();
    List<? extends Waypoint> waypoints = api.getAllWaypoints();

    if (waypoints.isEmpty()) {
      ctx.getSource().sendSuccess(() -> Component.literal("No waypoints to export."), false);
      return 1;
    }

    JsonObject json = WaypointSerializer.toJson(waypoints, api);
    Path file = resolveFile(filename);

    try {
      Files.createDirectories(file.getParent());
      Files.writeString(file, WaypointSerializer.toJsonString(json), StandardCharsets.UTF_8);
    } catch (IOException e) {
      JmWaypointManager.LOGGER.error("Failed to write waypoint export: {}", file, e);
      ctx.getSource().sendFailure(Component.literal("Export failed: " + e.getMessage()));
      return 0;
    }

    int count = waypoints.size();
    ctx.getSource()
        .sendSuccess(
            () -> Component.literal("Exported " + count + " waypoint(s) to " + file), false);
    return count;
  }

  @SuppressFBWarnings(
      value = "CRLF_INJECTION_LOGS",
      justification = "CRLF stripped from filename in resolveFile() before path construction")
  private static int doImport(CommandContext<CommandSourceStack> ctx, String filename) {
    if (!JmWaypointManagerPlugin.isAvailable()) {
      ctx.getSource().sendFailure(Component.literal("JourneyMap is not loaded."));
      return 0;
    }

    Path file = resolveFile(filename);
    if (!Files.exists(file)) {
      ctx.getSource().sendFailure(Component.literal("File not found: " + file));
      return 0;
    }

    String content;
    try {
      content = Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      JmWaypointManager.LOGGER.error("Failed to read waypoint import: {}", file, e);
      ctx.getSource().sendFailure(Component.literal("Import failed: " + e.getMessage()));
      return 0;
    }

    JsonObject json;
    try {
      json = WaypointSerializer.fromJsonString(content);
    } catch (Exception e) {
      ctx.getSource()
          .sendFailure(
              Component.literal("Invalid JSON in " + file.getFileName() + ": " + e.getMessage()));
      return 0;
    }

    int[] result =
        WaypointSerializer.fromJson(
            json, JmWaypointManagerPlugin.getApi(), JmWaypointManager.MODID);
    int added = result[0];
    int skipped = result[1];

    ctx.getSource()
        .sendSuccess(
            () ->
                Component.literal(
                    "Imported " + added + " waypoint(s), skipped " + skipped + " duplicate(s)."),
            false);
    return added;
  }

  // Strips path separators and CRLF characters to prevent directory traversal
  // and log injection, then appends .json.
  private static Path resolveFile(String filename) {
    String safe = filename.replaceAll("[/\\\\\r\n]", "_");
    if (safe.endsWith(".json")) {
      safe = safe.substring(0, safe.length() - 5);
    }
    return FMLPaths.CONFIGDIR.get().resolve(JmWaypointManager.MODID).resolve(safe + ".json");
  }
}
