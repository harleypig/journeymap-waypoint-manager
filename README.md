# JourneyMap Waypoint Manager

A NeoForge 1.21.1 mod that adds `/wm export` and `/wm import` commands
so you can back up, share, and restore your JourneyMap waypoints as
plain JSON files.

## Requirements

- NeoForge 1.21.1
- JourneyMap 6.0.0-beta or later

## Installation

Drop the mod jar into your instance's `mods/` folder alongside
JourneyMap and launch as normal.

## Usage

All commands use the `/wm` prefix and work in single-player or on a
server where you have the mod installed client-side.

### Export

```text
/wm export
/wm export <filename>
/wm export debug
/wm export <filename> debug
```

Writes all your current JourneyMap waypoints to a JSON file in the
`jm_waypoint_manager/` folder inside your Minecraft instance directory.
With no filename, the configured default (`waypoints`) is used.

The `debug` keyword logs one line per waypoint to `logs/latest.log`
instead of cluttering chat.

### Import

```text
/wm import
/wm import <filename>
/wm import debug
/wm import <filename> debug
```

Reads waypoints from a JSON file and adds them to JourneyMap. Waypoints
that already exist (matched by position or unique ID) are skipped
automatically, so re-importing the same file is always safe.

## Configuration

The mod creates `config/jm_waypoint_manager-client.toml` in your
instance directory on first launch:

| Setting | Default | Description |
|---------|---------|-------------|
| `defaultFilename` | `waypoints` | Filename used when no filename is given on the command. Set blank to require an explicit filename every time. |
| `debugMode` | `false` | Write per-waypoint detail to `logs/latest.log` during import and export. The `debug` keyword on a command overrides this for that run. |

## Files

Waypoint files live at:

```text
<instance>/jm_waypoint_manager/<name>.json
```

The `.json` extension is optional in commands — `/wm export waypoints`
and `/wm export waypoints.json` both work.

## Power Users

If you want to create waypoints by hand, convert them from another
format, or understand the full file structure, see
[docs/JSON-FORMAT.md](docs/JSON-FORMAT.md).

## License

[MIT](LICENSE)
