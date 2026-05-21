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

## Phase 4 — Implement MVP

MVP scope: two commands that work in-game, one export format (JSON array).

- [ ] **`/wm export [filename]`**
  - Calls JourneyMap API to get all waypoints for the current
    world/server context.
  - Writes a JSON array to `config/jm_waypoint_manager/<filename>.json`
    (defaults to `waypoints.json`).
  - Each entry includes: name, x, y, z, dimension, r, g, b, enabled,
    group.
  - Prints a confirmation message in chat with the file path and count.

- [ ] **`/wm import <filename>`**
  - Reads a JSON array from `config/jm_waypoint_manager/<filename>.json`.
  - Creates each waypoint via the JourneyMap API.
  - Skips duplicates (same name + coordinates) and reports how many
    were added vs. skipped.

- [ ] Register the command using NeoForge's command registration event.

- [ ] Test with a known set of waypoints: export, delete all in-game,
  import, verify they reappear.

---

## Phase 5 — Package and Install

- [ ] `./gradlew build` produces a jar in `build/libs/`.
- [ ] Copy the jar to the `mods/` folder of the target CurseForge
  instance.
- [ ] Confirm the mod loads in-game (check the mods list or log).

---

## Phase 6 — Code Quality Hooks

Expand pre-commit coverage once the Gradle scaffold (Phase 1) is in
place and Java sources exist to lint.

- [ ] **SpotBugs** — static bug analysis. Integrate via Gradle task
  (`./gradlew spotbugsMain`) and wire as a pre-commit local hook.
  Use `com.github.spotbugs` Gradle plugin. Treat all findings as
  errors; suppress only with documented justification.

- [ ] **Spotless** — opinionated formatter (wraps google-java-format).
  Add `com.diffplug.spotless` Gradle plugin, configure
  `googleJavaFormat()` in the `java` block. Wire two hooks:
  - check: `./gradlew spotlessCheck` (in `.pre-commit-config.yaml`)
  - fix: `./gradlew spotlessApply` (in `.pre-commit-config-fix.yaml`)
  Use `macisamuele/language-formatters-pre-commit-hooks` as an
  alternative if a non-Gradle hook is preferred (hook ID:
  `pretty-format-java`, latest rev via
  `gh api repos/macisamuele/language-formatters-pre-commit-hooks/tags`).

- [ ] **Dependency security scan** — check for known CVEs in
  dependencies. Candidates:
  - OWASP Dependency-Check Gradle plugin
    (`org.owasp:dependency-check-gradle`) — comprehensive NVD scan.
  - `./gradlew dependencyCheckAnalyze` as a manual-stage pre-commit
    hook (slow; not appropriate for every commit).
  Confirm the correct plugin and hook approach once the dependency
  tree is established in Phase 2.

- [ ] **Checkstyle / PMD** (optional) — evaluate after SpotBugs is
  wired. Avoid adding both unless each catches distinct classes of
  issue; overlapping linters add noise without value.

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
