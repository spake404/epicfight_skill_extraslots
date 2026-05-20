# Epic Fight Skill Extra Slots

Epic Fight Skill Extra Slots is a Minecraft Forge addon for Epic Fight that adds extra Passive, Mover, and Identity skill slots.

Version 2.1.0 optimizes the Epic Fight: Skill Tree integration by replacing per-tick full skill tree scans with dirty-state updates and low-frequency fallback verification. When Skill Tree is installed, extra slots are unlocked through a dedicated Extra Slots skill tree and Soul Stone items instead of being controlled only by the current slot count config.

## Features

- Adds extra Epic Fight skill slots for Passive, Mover, and Identity skills.
- Supports configurable maximum extra slot limits.
- Adds an optional `extraslot` Skill Tree page when Epic Fight: Skill Tree is installed.
- Generates Extra Passive, Extra Mover, and Extra Identity slot unlock nodes from the configured maximum slot limits.
- Uses stored Soul Stones to unlock extra slots in Skill Tree.
- Keeps learned extra slot unlock skills as hidden marker skills so they do not occupy normal Epic Fight skill slots.
- Closing an unlocked extra slot through Skill Tree unequip disables the slot and does not refund the consumed Soul Stone.
- Hides disabled extra slots from Epic Fight skill screens, skill book slot selection, and Better Skill Menu when present.
- Provides commands for operators or creative-mode players to adjust slot counts.
- Provides a Java API for other mods to modify slot counts through code.

## Requirements

- Minecraft 1.20.1
- Minecraft Forge 47+
- Epic Fight 20.0.0+
- Java 17

## Optional Compatibility

### Epic Fight: Skill Tree

Epic Fight: Skill Tree is optional.

When it is installed:

- The mod adds a new Skill Tree page named `Extra Slots`.
- Extra slot counts are unlocked through Skill Tree nodes.
- The current Passive, Mover, and Identity slot count controls are hidden from the config screen.
- Maximum extra slot limits still come from this mod's config.
- Soul Stones are used as the unlock resource for the corresponding slot type.

When it is not installed:

- Extra slot counts are controlled by this mod's config and commands.

### Better Skill Menu

Better Skill Menu is optional.

When it is installed, this mod hides disabled extra slots and extra slot unlock marker skills from Better Skill Menu. It also includes compatibility handling for newer Epic Fight versions where Better Skill Menu may reference the removed `RANGED` weapon category field.

## Soul Stones

Version 2.0 adds three Soul Stone items:

- Passive Skillslot Soul Stone
- Mover Skillslot Soul Stone
- Identity Skillslot Soul Stone

Right-clicking a Soul Stone stores it in the player's persistent data. Stored Soul Stones are displayed in the Skill Tree UI and are consumed when unlocking the matching extra slot type.

Soul Stone counts behave like a stored progression resource, not like direct inventory checks. Once a Soul Stone is stored, it can be used by the Extra Slots Skill Tree even if the item is no longer in the inventory.

## Crafting

Soul Stone recipes are defined as datapack recipes under:

```text
data/epicfight_skill_extraslots/recipes/
```

This makes them easy to override or modify in a modpack.

The included recipes require Epic Fight: Skill Tree because they use `epicskills:ability_stone`.

## Commands

These commands require OP level 2 or creative mode.

```mcfunction
/extraslots add <targets> Passive <amount>
/extraslots add <targets> Mover <amount>
/extraslots add <targets> Identity <amount>

/extraslots remove <targets> Passive <amount>
/extraslots remove <targets> Mover <amount>
/extraslots remove <targets> Identity <amount>
```

Examples:

```mcfunction
/extraslots add @s Passive 1
/extraslots add @a Mover 2
/extraslots remove PlayerName Identity 1
```

If a slot type is already at its minimum or maximum value, the command reports that no further change can be applied.

## Configuration

The mod adds common configuration options for:

- Extra Passive slot count
- Extra Mover slot count
- Extra Identity slot count
- Maximum extra Passive slot limit
- Maximum extra Mover slot limit
- Maximum extra Identity slot limit

When Epic Fight: Skill Tree is installed, the current extra slot count options are controlled by the Extra Slots Skill Tree instead of the config screen. The maximum extra slot limit options remain configurable.

Maximum extra slot limits apply after re-entering the world.

## Slot Behavior

Increasing slots can apply during gameplay.

Decreasing slots hides disabled slots and clears skills installed in disabled slots. The registered slot list is fully refreshed after re-entering the world.

When an extra slot is unlocked through Skill Tree, its unlock skill is moved into a hidden marker slot. This keeps the unlock state available for progression checks without occupying the player's normal Epic Fight skill slots.

## Developer API

Other mods can use `ExtraSlotsApi` to modify slot counts through code.

Available operations:

```java
ExtraSlotsApi.add(players, group, amount);
ExtraSlotsApi.remove(players, group, amount);
ExtraSlotsApi.set(players, group, count);
ExtraSlotsApi.get(group);
```

Slot groups:

```java
ExtraSlotsApi.SlotGroup.PASSIVE
ExtraSlotsApi.SlotGroup.MOVER
ExtraSlotsApi.SlotGroup.IDENTITY
```

## License

MIT
