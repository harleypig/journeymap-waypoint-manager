# Waypoint JSON Format Reference

This document describes the file format used by JourneyMap Waypoint
Manager. You can create or edit these files with any text editor and
import them with `/wm import`.

## File Location

```text
<instance>/jm_waypoint_manager/<name>.json
```

The `.json` extension is optional in commands.

## Structure

Files use a three-level nested object:

```text
dimension  →  group  →  waypoint[]
```

```json
{
  "minecraft:overworld": {
    "": [
      { "name": "Spawn", "x": 0, "y": 64, "z": 0, ... }
    ],
    "Bases": [
      { "name": "Main Base", "x": 1024, "y": 68, "z": -512, ... }
    ]
  },
  "minecraft:the_nether": {
    "": [
      { "name": "Portal", "x": 128, "y": 64, "z": -64, ... }
    ]
  }
}
```

- **Outer keys** are Minecraft dimension IDs
  (e.g., `minecraft:overworld`).
- **Inner keys** are waypoint group names. The empty string `""` means
  the ungrouped default group.
- **Each group** contains an array of waypoint objects.

## Waypoint Fields

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `name` | string | yes | Display name shown in JourneyMap |
| `x` | int | yes | Block X coordinate |
| `y` | int | yes | Block Y coordinate |
| `z` | int | yes | Block Z coordinate |
| `primaryDimension` | string | yes | Dimension this waypoint belongs to |
| `dimensions` | string\[\] | yes | Dimensions the waypoint appears in |
| `r` | int 0–255 | no | Red color channel (default: 255) |
| `g` | int 0–255 | no | Green color channel (default: 0) |
| `b` | int 0–255 | no | Blue color channel (default: 0) |
| `enabled` | bool | no | Whether the waypoint is visible (default: true) |
| `description` | string | no | Optional note shown in JourneyMap |
| `guid` | string | no | Unique ID; **omit when creating files by hand** |

## The `guid` Field

Exports include a `guid` for round-trip duplicate detection: if you
export, then re-import the same file later, any waypoint whose GUID
already exists in JourneyMap is skipped automatically.

**When creating a file by hand, leave `guid` out entirely.** JourneyMap
will assign a new GUID on import. If you include a GUID that already
belongs to an existing waypoint, the waypoint will be skipped as a
duplicate.

## The `dimensions` Field

`primaryDimension` is the dimension the waypoint "belongs to".
`dimensions` controls which dimensions the waypoint icon appears in.

For the typical case, set both to the same single value:

```json
"primaryDimension": "minecraft:overworld",
"dimensions": ["minecraft:overworld"]
```

You can list multiple dimensions if you want the waypoint visible from
more than one (for example, a portal that you want to see from both
the Overworld and the Nether map).

## Common Dimension IDs

| Dimension | ID |
|-----------|----|
| Overworld | `minecraft:overworld` |
| Nether | `minecraft:the_nether` |
| End | `minecraft:the_end` |

Modded dimensions follow the same pattern: `<modid>:<dimensionname>`.

## Duplicate Detection on Import

The importer skips a waypoint if either of these conditions is true:

- **GUID match** — the entry has a `guid` and that ID already exists
  among waypoints owned by this mod.
- **Position match** — a waypoint with the same name, X, Y, Z, and
  primary dimension already exists anywhere in JourneyMap, regardless
  of which mod or player created it.

Re-importing the same file is always safe; duplicates are counted and
reported but never added twice.

## Minimal Example

The smallest valid import file:

```json
{
  "minecraft:overworld": {
    "": [
      {
        "name": "Home",
        "x": 0,
        "y": 64,
        "z": 0,
        "primaryDimension": "minecraft:overworld",
        "dimensions": ["minecraft:overworld"]
      }
    ]
  }
}
```

## Full Example

```json
{
  "minecraft:overworld": {
    "": [
      {
        "name": "Spawn",
        "x": 0,
        "y": 64,
        "z": 0,
        "r": 255,
        "g": 255,
        "b": 255,
        "enabled": true,
        "description": "World spawn point",
        "primaryDimension": "minecraft:overworld",
        "dimensions": ["minecraft:overworld"]
      }
    ],
    "Bases": [
      {
        "name": "Main Base",
        "x": 1024,
        "y": 68,
        "z": -512,
        "r": 0,
        "g": 255,
        "b": 0,
        "enabled": true,
        "primaryDimension": "minecraft:overworld",
        "dimensions": ["minecraft:overworld"]
      }
    ]
  },
  "minecraft:the_nether": {
    "": [
      {
        "name": "Nether Hub",
        "x": 128,
        "y": 64,
        "z": -64,
        "r": 255,
        "g": 128,
        "b": 0,
        "primaryDimension": "minecraft:the_nether",
        "dimensions": ["minecraft:the_nether"]
      }
    ]
  }
}
```
