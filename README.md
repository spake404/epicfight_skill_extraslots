# Epic Fight Skill Extra Slots

Epic Fight Skill Extra Slots is a Minecraft Forge addon for Epic Fight that adds configurable extra skill slots for Passive, Mover, and Identity skills.

It is designed for modpacks and servers that want more flexible character progression while keeping slot counts configurable and controllable in-game.

## Features

- Adds extra Epic Fight skill slots for Passive, Mover, and Identity skills.
- Supports configurable current slot counts.
- Supports configurable maximum slot limits.
- Allows slot counts to be increased during gameplay.
- When slots are reduced, overflow slots are hidden immediately and fully removed after restarting the game.
- Synchronizes slot changes from server to client.
- Hides disabled slots from both the skill screen and the skill book installation screen.
- Provides commands for server operators or creative-mode players.
- Provides a Java API for other mods to modify slot counts through code.

## Requirements

- Minecraft 1.20.1
- Minecraft Forge 47+
- Epic Fight 20.0.0+
- Java 17

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

If a slot type is already at its minimum or maximum value, the command will report that no further change can be applied.

## Configuration

The mod adds common configuration options for:

- Passive slot count
- Mover slot count
- Identity slot count
- Maximum Passive slot limit
- Maximum Mover slot limit
- Maximum Identity slot limit

Increasing slots can apply during gameplay.

Decreasing slots hides the disabled slots immediately, but a full restart is recommended to completely remove them from the registered slot list.

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

## Planned Items

Three Soul Stone item placeholders exist internally for future use:

- Passive Skillslot Soul Stone
- Mover Skillslot Soul Stone
- Identity Skillslot Soul Stone

They are currently not registered in-game until final textures and behavior are ready.

## License

MIT
