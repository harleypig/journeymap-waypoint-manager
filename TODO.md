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

---

## Phase 1 — Scaffold the Project ✓ COMPLETE

---

## Phase 2 — Add the JourneyMap API Dependency ✓ COMPLETE

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
JSON structure: `dimension → group → waypoint[]`. Includes guid for
round-trip duplicate detection. Skips duplicates by GUID (primary) and
name+pos+dim (fallback for user-owned waypoints).

---

## Phase 5 — Package and Install

- [ ] Full round-trip test (export → delete waypoints → import →
  verify) — blocked on Phase 8 (file path migration and config).

---

## Phase 6 — Code Quality Hooks ✓ COMPLETE

Pre-commit hooks: google-java-format, SpotBugs (+ find-sec-bugs +
fb-contrib), PMD, Error Prone, OWASP Dependency-Check (manual stage).
Gradle wrapper validation. All quality tools gated behind explicit
invocation — `./gradlew build` stays clean.

---

## Phase 7 — GitHub Actions CI ✓ COMPLETE

`.github/workflows/build.yml` — two parallel jobs on push/PR to
`master`:

- **Build** — `actions/checkout`, `actions/setup-java` (Temurin 21),
  `gradle/actions/setup-gradle` (with caching), `./gradlew build`.
- **Pre-commit checks** — same Java + Gradle setup, then
  `actions/setup-python` + `pre-commit/action`. Runs
  `.pre-commit-config.yaml` (check only): google-java-format,
  SpotBugs, PMD, Error Prone, tests, wrapper validation. OWASP is
  `stages: [manual]` so it is excluded from CI.

---

## Phase 8 — Config, Logging, and Debug

- [x] **File path migration** — waypoint files moved from
  `config/jm_waypoint_manager/` to `<instance>/jm_waypoint_manager/`.
  Slash in filename is an error; `.json` auto-appended.

- [x] **Config** (`config/jm_waypoint_manager-client.toml`) — NeoForge
  `ModConfigSpec` registered in `JmWaypointManagerClient`. Options:
  `defaultFilename` (blank = explicit filename required) and
  `debugMode` (writes per-waypoint detail to log).

- [x] **Debug flag in commands** — `debug` keyword accepted before or
  after the optional filename in both `export` and `import`. Writes
  per-waypoint detail to `latest.log`; chat reply says "(detail in
  log)".

---

## Phase 9 — Group CRUD (Planned)

- [ ] **Group CRUD commands** — `/wm group list`, `/wm group create
  <name>`, `/wm group delete <name>`. Needed because importing someone
  else's waypoints may include groups that don't exist locally.

- [ ] **Duplicate detection design** — GUID match and coordinate match
  cover the common cases, but names are unreliable ("Village" appears
  in every world). Design a strategy for cross-source imports where
  neither GUID nor exact coordinates are a reliable signal. Must be
  resolved before Phase 10.

---

## Phase 10 — Live Waypoint Sync (Planned)

Duplicate detection strategy (Phase 9) must be decided before starting
this phase.

JourneyMap API v2 exposes `CommonEventRegistry.WAYPOINT_EVENT` with
CREATE/UPDATE/DELETED contexts — auto-export on waypoint change is
feasible. Needs design work before implementation.

- [ ] **Auto-export on change** — subscribe to `WaypointEvent` and
  re-export to the default file whenever a waypoint is added, updated,
  or deleted. Requires `defaultFilename` to be set.

- [ ] **Incremental update** — instead of full overwrite on each
  change, track deltas and merge into the existing file. Requires a
  merge strategy for removed waypoints.

---

## Phase 11 — Release Automation (Planned)

Deferred until the mod is feature-complete and ready to publish.

- [ ] **GitHub release workflow** — on tag push matching `v*`: run
  `./gradlew build`, create a GitHub release via `gh release create`,
  and attach the jar from `build/libs/`.

- [ ] **CurseForge upload** — automate jar upload to CurseForge as
  part of the release workflow. Research options: CurseForge API
  directly, `cf-cli`, or the `itsmeow/curseforge-upload` action.
  Will need a CurseForge API token stored as a repo secret.

- [ ] **Tag protection ruleset** — create a GitHub ruleset targeting
  `refs/tags/v*` that prevents deletion and force-pushes of release
  tags. Use the template in `private_dotfiles/github-rulesets` (create
  template there first if it doesn't exist).

- [ ] **Issue templates** — create `.github/ISSUE_TEMPLATE/` templates
  for feature requests and bug reports.

---

## Phase 12 — Companion Tooling (Planned)

- [ ] **JSON schema** — define a JSON schema for the waypoints.json
  format to enable editor autocomplete and validation of hand-crafted
  import files.

- [ ] **Format conversion script** — Python script to convert waypoint
  data from other formats (JourneyMap `.csv`, other mod exports) into
  this mod's JSON format.

---

## Notes

- The export JSON format is your own design, not JourneyMap's internal
  format. This is intentional: it stays stable across JM updates.
- External tools (scripts, spreadsheets) can generate import files as
  plain JSON without any mod tooling.
- The `WaypointData.dat` NBT files should never be parsed directly by
  this mod; use the API exclusively.
- [ ] **NeoForge GameTest** — research the GameTest framework
  (`./gradlew runGameTestServer`) for in-game integration tests.
  Useful for verifying command registration and end-to-end API
  behavior; would complement the current JUnit 5 unit tests.
