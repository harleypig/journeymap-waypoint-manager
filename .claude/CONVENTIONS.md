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

## JSON Format

- The mod reads and writes only its own JSON format. Never parse
  JourneyMap's internal `WaypointData.dat` (NBT binary); use the API
  exclusively.
- Structure: `dimension → group → waypoint[]`. Group key `""` means
  the ungrouped default group.
- External tools can generate import files as plain JSON without any
  mod tooling.

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
- NeoForge has a `GameTest` framework (`./gradlew runGameTestServer`)
  for in-game integration tests, but it requires a running game
  instance and is not appropriate for testing business logic. JUnit 5
  + Mockito is the right fit for this mod.

## Pre-commit Workflow

- **Fix config** (`.pre-commit-config-fix.yaml`) — applies all
  auto-fixes: Google Java Format, trailing whitespace, end-of-file
  newlines, and mixed line endings.
- **Check config** (`.pre-commit-config.yaml`) — read-only checks:
  static analysis, tests, markdown, and formatting verification.
- **Workflow before committing locally:**
  1. `pre-commit run --config .pre-commit-config-fix.yaml --all-files`
  2. Re-stage modified files and commit (check config runs via hook).
- **CI enforcement:** runs the fix config, then `git diff --exit-code`
  to fail if any files needed auto-fixing, then runs the check config
  for static analysis, tests, and the remaining hooks.
