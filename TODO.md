# JourneyMap Waypoint Manager — Getting Started

Target: NeoForge 1.21.1 + JourneyMap 6.0.0-beta

## Background

JourneyMap 6.0.0 (the 1.21.1 NeoForge version) stores all waypoints in a
binary NBT file (`WaypointData.dat`) rather than the individual JSON files
described in older documentation. Direct file parsing is not viable. All
waypoint access must go through the JourneyMap API.

---

## Phase 0 — Prerequisites ✓ COMPLETE

See `docs/DEVELOPER.md` for full environment setup instructions (Java 21 JDK,
editor options, Gradle, and the build/test workflow for headless WSL2).

- [x] Java 21 JDK installed and active (`java -version` prints `21.x.x`)
- [x] Editor configured (VS Code Remote-WSL or terminal editor — see
  `DEVELOPER.md`)
- [x] `git` available in the WSL2 shell (`git --version`)
- [x] `pre-commit` installed and hooks activated (`pre-commit install`)

---

## Phase 1 — Scaffold the Project ✓ COMPLETE

- [x] **Clone the NeoForge MDK** — used `NeoForgeMDKs/MDK-1.21.1-NeoGradle`
  cloned to `../MDK-1.21.1-NeoGradle`.

- [x] **Copy scaffold files** from the MDK into this repo verbatim,
  then applied google-java-format as a separate formatting commit.

- [x] **Rename the example mod** to match this project:
  - Mod ID: `jm_waypoint_manager`
  - Mod name: `JourneyMap Waypoint Manager` (display name)
  - Package: `com.harleypig.jm_waypoint_manager`
  - Added `neo_version_range=[21.1.0,21.2.0)` so the jar loads on any
    NeoForge 21.1.x, not just the exact build it was compiled against.

- [x] **Run the initial Gradle setup** — `BUILD SUCCESSFUL`,
  produces `build/libs/jm_waypoint_manager-0.1.0.jar`.

---

## Phase 2 — Add the JourneyMap API Dependency ✓ COMPLETE

- [x] **Find the correct JourneyMap API artifact**: `journeymap-api-neoforge`
  version `2.0.0-1.21.1-SNAPSHOT` from `https://maven.blamejared.com`.
  Source: `TeamJM/journeymap-api` branch `1.21.1_2.0_SNAPSHOTS`,
  `docs/howto.md`.

- [x] **Added to `gradle.properties`**: `journeymap_api_version=2.0.0-1.21.1-SNAPSHOT`

- [x] **Added to `build.gradle`**:
  - Two JourneyMap Maven repos (primary: `maven.blamejared.com`,
    legacy: `jm.gserv.me`)
  - `configurations.all { resolutionStrategy.cacheChangingModulesFor 0, 'seconds' }`
    so SNAPSHOT updates are fetched on every build
  - `compileOnly group: 'info.journeymap', name: 'journeymap-api-neoforge', version: journeymap_api_version, changing: true`

- [x] **Added soft JourneyMap dependency** to `neoforge.mods.toml` so the
  mod loads after JourneyMap when both are present but does not require it.

- [x] `./gradlew compileJava` — BUILD SUCCESSFUL, API on classpath.

---

## Phase 3 — Understand the JourneyMap API Surface ✓ COMPLETE

All findings from branch `TeamJM/journeymap-api@1.21.1_2.0_SNAPSHOTS`.

**Plugin initialization** (`journeymap.api.v2.client`):

- Annotate plugin class with `@JourneyMapPlugin(apiVersion = "2.0.0")`,
  implement `IClientPlugin`. JourneyMap discovers it via annotation
  scanning on NeoForge (no extra registration needed).
- `initialize(IClientAPI jmAPI)` is called at mod-load time. Store the
  reference — it is the only way to access the API.

**Waypoint CRUD** (`journeymap.api.v2.client.IClientAPI`):

- **List all**: `jmClientApi.getAllWaypoints()` — all dimensions, all
  mods; returns copies (mutating them has no effect in-game).
- **List by dimension**: `getAllWaypoints(ResourceKey<Level> dim)`
- **Create**: `WaypointFactory.createWaypoint(modId, blockPos, name,
  dimension, persistent)` then set fields, then
  `jmClientApi.addWaypoint(modId, waypoint)`. Use `persistent = true`
  so JM saves it to disk.
- **Delete**: `jmClientApi.removeWaypoint(modId, waypoint)` — only
  removes waypoints owned by `modId`. Waypoints created by this mod use
  `"jm_waypoint_manager"` as the owner; user waypoints use
  `"journeymap"` and cannot be removed via API.
- **Modify**: copies returned by `getAllWaypoints()` are read-only for
  in-game state. To update, call setters on the copy then
  `removeWaypoint` + `addWaypoint`.

**Waypoint fields** (`journeymap.api.v2.common.waypoint.Waypoint`):

| Field | Getter | Setter |
| --- | --- | --- |
| Unique ID | `getGuid()` | — (assigned at creation) |
| Display name | `getName()` | `setName(String)` |
| Description | `getDescription()` | `setDescription(String)` |
| Position | `getX/Y/Z()`, `getBlockPos()` | `setX/Y/Z()`, `setPos()`, `setBlockPos()` |
| Color (packed) | `getColor()` | `setColor(int)` |
| Color (channels) | `getRed/Green/Blue()` | `setRed/Green/Blue(int)` |
| Primary dimension | `getPrimaryDimension()` | `setPrimaryDimension(ResourceKey<Level>)` |
| All dimensions | `getDimensions()` → `TreeSet<String>` | `setDimensions(Collection<String>)` |
| Enabled | `isEnabled()` | `setEnabled(boolean)` |
| Persistent | `isPersistent()` | `setPersistent(boolean)` |
| Group ID | `getGroupId()` | — (set via group API) |
| Custom data | `getCustomData(key)` | `setCustomData(key, value)` |

**Events** (`journeymap.api.v2.common.event.CommonEventRegistry`):

- `WAYPOINT_EVENT` — fires on CREATE, UPDATE, DELETED, READ. Subscribe
  in `initialize()`: `CommonEventRegistry.WAYPOINT_EVENT.subscribe(modId, consumer)`.
  CREATE and UPDATE are cancellable.
- The old `ClientEventRegistry.WAYPOINT_EVENT` is `@Deprecated` —
  use `CommonEventRegistry` instead.

**API stability**: The 2.0.0-SNAPSHOT API is stable enough to build
against. `createClientWaypoint` is deprecated in favour of
`createWaypoint`; `ClientEventRegistry.WAYPOINT_EVENT` deprecated in
favour of `CommonEventRegistry.WAYPOINT_EVENT`. No need to pin a
specific beta build — SNAPSHOT is intentional for this series.

---

## Phase 4 — Implement MVP ✓ COMPLETE

MVP scope: two commands that work in-game, one export format (JSON).

- [x] **`/wm export [filename]`** — implemented; writes
  `config/jm_waypoint_manager/<filename>.json` (default `waypoints`).
  JSON structure: `dimension → group → waypoint[]`. Includes guid for
  round-trip duplicate detection.

- [x] **`/wm import <filename>`** — implemented; reads the same
  format, skips duplicates by GUID (primary) and name+pos+dim
  (fallback for user-owned waypoints).

- [x] Register the command using NeoForge's `RegisterCommandsEvent`
  on the game bus from the client-only `@Mod` constructor.

- [x] Test with a known set of waypoints — mod initializes in-game,
  plugin discovered, API initialized. Full round-trip test (export →
  delete → import) blocked pending file-structure design decisions;
  see open questions below.

---

## Phase 5 — Package and Install ✓ PARTIALLY COMPLETE

- [x] `./gradlew build` produces a jar in `build/libs/`.
- [x] Copy the jar to the `mods/` folder of the target CurseForge
  instance (United Colonies).
- [x] Confirm the mod loads in-game — mod appears in the in-game mod
  list; JourneyMap discovers the plugin (`@JourneyMapPlugin` found,
  API initialized).
- [ ] Full round-trip test (export → delete waypoints → import →
  verify) — blocked on open design questions below.

---

## Open Design Questions

Answers needed before completing Phase 5 testing and implementing
Phase 8 (config, logging, debug).

**File structure** — where should the mod put its files?

Proposed layout:

```text
<instance>/
  jm_waypoint_manager/          ← mod working dir (FMLPaths.GAMEDIR)
    waypoints/                  ← export/import files
    logs/                       ← mod-specific log (if dedicated log added)
  config/
    jm_waypoint_manager.toml    ← NeoForge config (must live here)
```

Currently waypoints go to `config/jm_waypoint_manager/`. Decision
needed: move to `<instance>/jm_waypoint_manager/waypoints/`?

Decision: Yes, follow the directory structure above. No path is required or
needed for the filename, since it will be placed in the waypoints/ dir above.
Including path (either slash) should be an error explaining why.

**Import vs export default filename** — commands already accept
explicit filenames. Should the config default be one shared value
(`waypoints`) or separate defaults for each command?
Recommendation: one shared default.

Decision: default filenames for import and export should be the same and set
in the configuration file automatically when initialized. If one or both of
those settings are set to blank or removed, the relevant command should
require a filename from the chat cli. In either case, the chat cli can
override the configuration if a filename is included.

**Logging** — SLF4J already routes to `logs/latest.log`. Do we also
want a dedicated `jm_waypoint_manager/logs/jm_waypoint_manager.log`
(requires custom Log4j2 appender)?

Decision: Leave it for the moment, but keep it in mind should we ever need
more details.

**Group management** — import/export already preserves group
membership (round-trips the `dimension → group → waypoints[]`
structure). Is the ask for management *commands* (`/wm group list`,
`/wm group create`, etc.) or something else?

Answer: We want group CRUD as well, since importing someone else's waypoints
will almost certainly include their own groups.

---

## Phase 8 — Config, Logging, and Debug (Planned)

Blocked on open design questions above.

- [ ] **Config** (`config/jm_waypoint_manager.toml`) — NeoForge
  `ModConfigSpec`. Options: default filename, debug mode, log level.

- [ ] **Debug flag in commands** — `/wm import <filename> debug`
  (optional literal arg) overrides the config debug setting for that
  one invocation; prints per-waypoint detail to chat.

- [ ] **Dedicated log file** (if decided) — custom Log4j2 appender.

- [ ] **File path migration** — move waypoint files to
  `<instance>/jm_waypoint_manager/waypoints/` if that layout is
  approved.

---

## Phase 6 — Code Quality Hooks

- [x] **Spotless / google-java-format** — covered by the existing
  `macisamuele/language-formatters-pre-commit-hooks`
  (`pretty-format-java`) hook wired in Phase 1. No Gradle plugin
  needed.

- [x] **SpotBugs** — static bug analysis via `com.github.spotbugs`
  Gradle plugin (v6.5.4, SpotBugs tool 4.9.8). Wired as a local
  pre-commit hook (`./gradlew spotbugsMain`). All findings treated as
  errors; suppress only with documented `@SuppressFBWarnings`. Plugins
  active: find-sec-bugs (security patterns) and fb-contrib/sb-contrib
  (extended correctness/performance patterns).

- [ ] **OWASP Dependency-Check** — scans dependencies for known CVEs
  via `org.owasp:dependency-check-gradle`. Run as a manual-stage
  pre-commit hook (too slow for every commit). Worth running before
  releases and periodically.

- [ ] **PMD** — structural static analysis complementary to SpotBugs.
  Covers cyclomatic complexity, overly long methods, dead code, and
  copy-paste detection — issue classes SpotBugs and fb-contrib largely
  skip. Straightforward Gradle plugin integration. Recommended next
  addition.

- [ ] **Error Prone** — Google's compile-time bug checker; runs as a
  javac plugin so findings appear during `compileJava`, not a separate
  step. Highest signal-to-noise of any Java static analyzer; catches
  real bugs SpotBugs misses because it has full type information at
  compile time. Main risk: NeoGradle manages the compiler invocation
  in non-standard ways, so the Java agent attachment required by Error
  Prone may need extra Gradle wiring. Investigate integration before
  committing. Consider NullAway (an Error Prone plugin) alongside it,
  since we have started annotating with `@Nullable`.

---

## Phase 7 — GitHub Actions CI

Set up automated CI so every push and PR is verified without manual
intervention.

- [ ] Create `.github/workflows/build.yml`:
  - Trigger on `push` and `pull_request` to `main`/`master`.
  - Steps: checkout → set up Java 21 → `./gradlew build`
  - Cache Gradle wrapper and dependencies for faster runs.

- [ ] Add a `pre-commit` CI job:
  - Use `pre-commit/action` or invoke `pre-commit run --all-files`
    directly.
  - Run only the check config (`.pre-commit-config.yaml`), never
    the fix config.

- [ ] Add a SpotBugs/Spotless check job once Phase 6 is complete.

- [ ] Consider a release workflow: on tag push matching `v*`, run
  `./gradlew build` and upload the output jar as a GitHub release
  asset via `gh release create`.

---

## Notes

- The export JSON format is your own design, not JourneyMap's internal
  format. This is intentional: it stays stable across JM updates.
- External tools (scripts, spreadsheets) can generate import files as
  plain JSON without any mod tooling.
- The `WaypointData.dat` NBT files should never be parsed directly by
  this mod; use the API exclusively.
