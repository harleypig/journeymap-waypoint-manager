# Conventions

## NeoForge Mod Architecture

- `@Mod(MODID)` — main mod class; runs on both logical sides.
- `@Mod(value = MODID, dist = Dist.CLIENT)` — client-only class;
  handles client-side registration (config, commands, screens).
- Config registration: `container.registerConfig(Type.CLIENT, SPEC)`
  in the client-only constructor. CLIENT configs land at
  `<instance>/config/<modid>-client.toml`.
- Event bus listeners: `NeoForge.EVENT_BUS.addListener(...)` in the
  client-only constructor.

## JourneyMap API Integration

- Plugin implements `IClientPlugin`, annotated `@JourneyMapPlugin`.
- Always guard API use with `JmWaypointManagerPlugin.isAvailable()`.
- The live API reference is stored statically in
  `JmWaypointManagerPlugin.api`; callers use `getApi()`.

## File Locations (Runtime)

- Waypoint JSON files: `<instance>/jm_waypoint_manager/<name>.json`
  (managed by this mod; NOT in `config/`).
- Mod config TOML: `<instance>/config/jm_waypoint_manager-client.toml`
  (managed by NeoForge).

## Filename Handling

- `.json` extension is always stripped then re-appended in
  `resolveFile()` so users can supply names with or without it.
- Path separators (`/`, `\`) in a filename are a hard error, not
  silently replaced — prevents directory traversal.
- CRLF characters are stripped in `resolveFile()` to prevent log
  injection.

## Testing Constraints

- `FMLPaths`, `ModLoadingContext`, and NeoForge event bus cannot be
  instantiated in unit tests — keep that code in thin wiring classes.
- JourneyMap API interfaces (`IClientAPI`, `Waypoint`, `WaypointGroup`)
  can be mocked with Mockito.
- Static factory methods (`WaypointFactory`) use `mockStatic()` in a
  try-with-resources block.
- Business logic (serialization, filename validation, resolution) must
  be extractable from NeoForge-dependent code so it can be unit-tested.
  Extract to package-private helpers when needed.
