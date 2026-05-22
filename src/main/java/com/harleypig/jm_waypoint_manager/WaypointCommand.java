package com.harleypig.jm_waypoint_manager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.common.waypoint.Waypoint;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class WaypointCommand {

  private static final String FILENAME_ARG = "filename";
  private static final String DEBUG_LITERAL = "debug";

  public static void onRegisterCommands(RegisterCommandsEvent event) {
    register(event.getDispatcher());
  }

  private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    StringArgumentType filenameArg = StringArgumentType.string();
    dispatcher.register(
        Commands.literal("wm")
            .then(
                Commands.literal("export")
                    .executes(ctx -> doExport(ctx, null, false))
                    .then(
                        Commands.literal(DEBUG_LITERAL)
                            .executes(ctx -> doExport(ctx, null, true))
                            .then(
                                Commands.argument(FILENAME_ARG, filenameArg)
                                    .executes(
                                        ctx ->
                                            doExport(
                                                ctx,
                                                StringArgumentType.getString(ctx, FILENAME_ARG),
                                                true))))
                    .then(
                        Commands.argument(FILENAME_ARG, filenameArg)
                            .executes(
                                ctx ->
                                    doExport(
                                        ctx,
                                        StringArgumentType.getString(ctx, FILENAME_ARG),
                                        false))
                            .then(
                                Commands.literal(DEBUG_LITERAL)
                                    .executes(
                                        ctx ->
                                            doExport(
                                                ctx,
                                                StringArgumentType.getString(ctx, FILENAME_ARG),
                                                true)))))
            .then(
                Commands.literal("import")
                    .executes(ctx -> doImport(ctx, null, false))
                    .then(
                        Commands.literal(DEBUG_LITERAL)
                            .executes(ctx -> doImport(ctx, null, true))
                            .then(
                                Commands.argument(FILENAME_ARG, filenameArg)
                                    .executes(
                                        ctx ->
                                            doImport(
                                                ctx,
                                                StringArgumentType.getString(ctx, FILENAME_ARG),
                                                true))))
                    .then(
                        Commands.argument(FILENAME_ARG, filenameArg)
                            .executes(
                                ctx ->
                                    doImport(
                                        ctx,
                                        StringArgumentType.getString(ctx, FILENAME_ARG),
                                        false))
                            .then(
                                Commands.literal(DEBUG_LITERAL)
                                    .executes(
                                        ctx ->
                                            doImport(
                                                ctx,
                                                StringArgumentType.getString(ctx, FILENAME_ARG),
                                                true))))));
  }

  @SuppressWarnings("PMD.GuardLogStatement")
  @SuppressFBWarnings(
      value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "CRLF_INJECTION_LOGS"},
      justification =
          "getParent() is non-null: resolveFile returns a 3-component path;"
              + " CRLF stripped from filename in resolveFile() before path construction;"
              + " debug logging is already guarded by if (debugMode)")
  private static int doExport(
      CommandContext<CommandSourceStack> ctx, @Nullable String filename, boolean debug) {
    if (!JmWaypointManagerPlugin.isAvailable()) {
      ctx.getSource().sendFailure(Component.literal("JourneyMap is not loaded."));
      return 0;
    }

    String effectiveFilename =
        resolveFilename(filename, WaypointManagerConfig.DEFAULT_FILENAME.get());
    if (effectiveFilename == null) {
      ctx.getSource()
          .sendFailure(
              Component.literal(
                  "No filename specified. Provide one or set defaultFilename in config."));
      return 0;
    }

    String filenameError = validateFilename(effectiveFilename);
    if (filenameError != null) {
      ctx.getSource().sendFailure(Component.literal(filenameError));
      return 0;
    }

    IClientAPI api = JmWaypointManagerPlugin.getApi();
    List<? extends Waypoint> waypoints = api.getAllWaypoints();

    if (waypoints.isEmpty()) {
      ctx.getSource().sendSuccess(() -> Component.literal("No waypoints to export."), false);
      return 1;
    }

    boolean debugMode = debug || WaypointManagerConfig.DEBUG_MODE.get();

    if (debugMode) {
      JmWaypointManager.LOGGER.info("Export debug — {} waypoint(s):", waypoints.size());
      for (Waypoint wp : waypoints) {
        JmWaypointManager.LOGGER.info(
            "  > {} ({}, {}, {}) in {}",
            wp.getName(),
            wp.getX(),
            wp.getY(),
            wp.getZ(),
            wp.getPrimaryDimension());
      }
    }

    JsonObject json = WaypointSerializer.toJson(waypoints, api);
    Path file = resolveFile(effectiveFilename);

    try {
      Files.createDirectories(file.getParent());
      Files.writeString(file, WaypointSerializer.toJsonString(json), StandardCharsets.UTF_8);
    } catch (IOException e) {
      JmWaypointManager.LOGGER.error("Failed to write waypoint export: {}", file, e);
      ctx.getSource().sendFailure(Component.literal("Export failed: " + e.getMessage()));
      return 0;
    }

    int count = waypoints.size();
    String suffix = debugMode ? " (detail in log)" : "";
    ctx.getSource()
        .sendSuccess(
            () -> Component.literal("Exported " + count + " waypoint(s) to " + file + suffix),
            false);
    return count;
  }

  @SuppressFBWarnings(
      value = {"CRLF_INJECTION_LOGS", "CLI_CONSTANT_LIST_INDEX"},
      justification =
          "CRLF stripped from filename in resolveFile() before path construction;"
              + " result[0]/result[1] are immediately aliased to named variables;"
              + " debug log uses {} parameterized form to avoid format injection")
  private static int doImport(
      CommandContext<CommandSourceStack> ctx, @Nullable String filename, boolean debug) {
    if (!JmWaypointManagerPlugin.isAvailable()) {
      ctx.getSource().sendFailure(Component.literal("JourneyMap is not loaded."));
      return 0;
    }

    String effectiveFilename =
        resolveFilename(filename, WaypointManagerConfig.DEFAULT_FILENAME.get());
    if (effectiveFilename == null) {
      ctx.getSource()
          .sendFailure(
              Component.literal(
                  "No filename specified. Provide one or set defaultFilename in config."));
      return 0;
    }

    String filenameError = validateFilename(effectiveFilename);
    if (filenameError != null) {
      ctx.getSource().sendFailure(Component.literal(filenameError));
      return 0;
    }

    Path file = resolveFile(effectiveFilename);
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
    } catch (JsonParseException | IllegalStateException e) {
      ctx.getSource()
          .sendFailure(
              Component.literal("Invalid JSON in " + file.getFileName() + ": " + e.getMessage()));
      return 0;
    }

    boolean debugMode = debug || WaypointManagerConfig.DEBUG_MODE.get();
    Consumer<String> debugLog = debugMode ? WaypointCommand::logDebug : ignored -> {};

    int[] result =
        WaypointSerializer.fromJson(
            json, JmWaypointManagerPlugin.getApi(), JmWaypointManager.MODID, debugLog);
    int added = result[0];
    int skipped = result[1];

    String suffix = debugMode ? " (detail in log)" : "";
    ctx.getSource()
        .sendSuccess(
            () ->
                Component.literal(
                    "Imported "
                        + added
                        + " waypoint(s), skipped "
                        + skipped
                        + " duplicate(s)."
                        + suffix),
            false);
    return added;
  }

  @Nullable
  static String resolveFilename(@Nullable String commandFilename, String configDefault) {
    if (commandFilename != null && !commandFilename.isEmpty()) {
      return commandFilename;
    }
    return !configDefault.isEmpty() ? configDefault : null;
  }

  @Nullable
  static String validateFilename(String filename) {
    if (filename.contains("/") || filename.contains("\\")) {
      return "Filename must not contain path separators (/ or \\).";
    }
    return null;
  }

  // Debug messages come from our own serializer, not raw user input; {} form prevents
  // format-string injection. CRLF_INJECTION_LOGS suppressed on this named method so
  // SpotBugs can see the annotation (lambdas are compiled as separate synthetic methods).
  @SuppressFBWarnings(
      value = "CRLF_INJECTION_LOGS",
      justification =
          "debug messages are constructed by WaypointSerializer from parsed data,"
              + " not passed through raw from user input; {} prevents format injection")
  private static void logDebug(String msg) {
    JmWaypointManager.LOGGER.info("{}", msg);
  }

  // Strips CRLF to prevent log injection, then ensures .json extension.
  private static Path resolveFile(String filename) {
    String safe = filename.replaceAll("[\r\n]", "_");
    if (safe.endsWith(".json")) {
      safe = safe.substring(0, safe.length() - 5);
    }
    return FMLPaths.GAMEDIR.get().resolve(JmWaypointManager.MODID).resolve(safe + ".json");
  }
}
