# Developer Setup — Headless WSL2 Ubuntu 24.04

This project is developed in a headless Linux environment (WSL2 Ubuntu 24.04).
Minecraft runs on the Windows host; the mod is built in Linux and the output
jar is copied to a CurseForge instance for testing.

---

## Java 21 JDK

NeoForge 1.21.1 requires Java 21. Install it via the Ubuntu package manager:

```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

Verify:

```bash
java -version
# Should print: openjdk version "21.x.x" ...
javac -version
# Should print: javac 21.x.x
```

If your system has multiple JDK versions installed, pin Java 21 as the active
one:

```bash
sudo update-alternatives --config java
sudo update-alternatives --config javac
```

Select the `java-21-openjdk-amd64` entry from each menu.

---

## Gradle

The NeoForge MDK ships with a Gradle wrapper (`gradlew`). You do not need to
install Gradle system-wide — the wrapper downloads the correct version on
first use.

The wrapper does need `curl` or `wget` and `unzip` available:

```bash
sudo apt install curl unzip
```

---

## pre-commit

Install pre-commit and activate the hooks for this repo:

```bash
sudo apt install pre-commit
pre-commit install
```

Run checks manually at any time:

```bash
pre-commit run --all-files
```

To apply fixers (trailing whitespace, line endings, Java formatting):

```bash
pre-commit run --config .pre-commit-config-fix.yaml --all-files
```

## Git

Git is almost certainly already present on Ubuntu 24.04. Confirm:

```bash
git --version
```

If not:

```bash
sudo apt install git
```

---

## Editor

Use whatever editor works for you. The project maintainer uses vim with
coc.nvim and ALE.

---

## Building the Mod

After the MDK is set up (see `TODO.md` Phase 1):

```bash
# First build — downloads NeoForge (~5–15 minutes, network-dependent)
./gradlew build

# Subsequent builds
./gradlew build

# Output jar
ls build/libs/*.jar
```

The jar to install in Minecraft is the one **without** `-sources` or
`-all` in the name, e.g., `jm-waypoint-manager-1.21.1-0.1.0.jar`.

---

## Testing Workflow

Testing requires a running Minecraft client (Windows side) because NeoForge
mods execute in the game engine.

1. Build the jar in WSL2: `./gradlew build`
2. Copy it to a CurseForge instance's `mods/` folder:

   ```bash
   INSTANCE="/mnt/c/Users/harleypig/projects/curseforge/minecraft/Instances/HarleyColonies"
   cp build/libs/jm-waypoint-manager-*.jar "$INSTANCE/mods/"
   ```

3. Launch Minecraft from CurseForge on the Windows side.
4. Check the in-game mod list or the log at:
   `$INSTANCE/logs/latest.log`

The log is readable from WSL2:

```bash
tail -f "$INSTANCE/logs/latest.log"
```

---

## Useful Gradle Tasks

| Task | Purpose |
|------|---------|
| `./gradlew build` | Compile and package the mod jar |
| `./gradlew compileJava` | Compile only, no packaging |
| `./gradlew clean` | Remove build outputs |
| `./gradlew dependencies` | Show the full dependency tree |
| `./gradlew --refresh-dependencies build` | Force re-resolve all dependencies |
