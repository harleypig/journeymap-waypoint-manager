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

## Phase 1 — Scaffold the Project

- [ ] **Clone the NeoForge MDK** as a sibling repo:

  ```bash
  git clone https://github.com/neoforged/MDK ../MDK
  ```

  Then check out the correct branch/tag for 1.21.1 — inspect
  `git -C ../MDK branch -a` and `git -C ../MDK tag` to find it.

- [ ] **Copy scaffold files** from `../MDK` into this repo. Files to copy:
  - `build.gradle`, `settings.gradle`, `gradle.properties`
  - `gradle/` directory, `gradlew`, `gradlew.bat`
  - `src/main/java/com/example/examplemod/` (rename to our package below)
  - `src/main/resources/META-INF/mods.toml`
  - `src/main/resources/pack.mcmeta`
  - `.gitignore` (MDK's Gradle/IntelliJ ignore rules are comprehensive)
  Do NOT copy `.git/`, `README.md`, or `LICENSE` from the MDK.

- [ ] **Rename the example mod** to match this project:
  - Mod ID: `jm_waypoint_manager` (snake_case, no hyphens)
  - Mod name: `JM Waypoint Manager`
  - Package: `com.harleypig.jm_waypoint_manager`
  - Files to update: `build.gradle`, `settings.gradle`,
    `gradle.properties`, `src/main/resources/META-INF/mods.toml`,
    the main Java class, and all package directories.

- [ ] **Run the initial Gradle setup** to verify the scaffold compiles:

  ```bash
  ./gradlew build
  ```

  This will download NeoForge and all dependencies on first run (~5–15
  minutes). A `BUILD SUCCESSFUL` confirms the scaffold is working.

---

## Phase 2 — Add the JourneyMap API Dependency

- [ ] **Find the correct JourneyMap API artifact** for JM 6.0.0-beta /
  NeoForge 1.21.1.
  - Check the JourneyMap API GitHub releases:
    <https://github.com/TeamJM/journeymap-api>
  - Check the JourneyMap CurseForge or Modrinth page for the API jar.
  - The API jar is separate from the main mod jar. You depend on the
    API at compile time but the user installs the full mod at runtime.

- [ ] **Add the API to `build.gradle`** under `dependencies`:

  ```groovy
  compileOnly "info.journeymap:journeymap-api-neoforge:<version>"
  ```

  The exact group ID, artifact ID, and version need to be confirmed
  from the step above — do not guess.

- [ ] Add the JourneyMap repository to `repositories` in `build.gradle`
  if the API is not on Maven Central (it likely isn't). The JourneyMap
  team publishes to their own Maven; find the URL in their repo README.

- [ ] Run `./gradlew build` again to confirm the API resolves.

---

## Phase 3 — Understand the JourneyMap API Surface

- [ ] Read the JourneyMap API source/docs to find:
  - How to obtain the `IClientAPI` instance at runtime.
  - How to list all waypoints (across all dimensions).
  - How to create a new waypoint programmatically.
  - How to delete a waypoint.
  - What fields a waypoint exposes (name, x, y, z, dimension, color,
    enabled, group/label).
  - Whether the API has events for waypoint changes (nice-to-have).

- [ ] Check whether the 6.0.0-beta API is stable enough to depend on, or
  whether there are breaking changes between beta builds. If the API is
  unstable, pin to a specific beta build number in `build.gradle`.

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
