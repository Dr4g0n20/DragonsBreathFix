# DragonsBreathFix

A Minecraft 1.12.2 / Java 8 Spigot-compatible plugin intended to work around an Eaglercraft 1.12.2 client crash in `EntityAreaEffectCloud.shouldIgnoreRadius()`.

## Approach

The server continues to create and tick its normal `AreaEffectCloud` entities. This is deliberate: the server remains responsible for vanilla lingering-potion and dragon's-breath effects, including their durations, radius, damage and potion-effect application.

ProtocolLib filters outgoing 1.12.2 `SPAWN_ENTITY` packets for object type ID 3 (Area Effect Cloud), preventing clients from creating the entity that triggers the reported Eaglercraft crash.

The plugin optionally displays coloured `SPELL_MOB` potion particles around active server-side clouds. These are ordinary 1.12-era potion particles, not an area-effect-cloud entity.

## Requirements

- Minecraft server 1.12.2 (Spigot-compatible API)
- Java 8
- ProtocolLib 4.4.0 or another ProtocolLib build compatible with the server

## Build

Run:

```sh
mvn clean package
```

The output JAR is:

```
target/DragonsBreathFix.jar
```

Place that JAR in the server's `plugins` folder. Keep ProtocolLib installed.

## Important testing notes

- Test with a normal lingering potion, a lingering water bottle, and dragon's breath.
- Confirm that potion effects and dragon's-breath damage still happen server-side.
- Confirm that Eaglercraft clients no longer receive AreaEffectCloud spawn packets.
- Confirm that the splash-style particles render correctly on the target Eaglercraft client.

This is an initial implementation and must be tested on the actual Beast/Spigot-compatible server before being treated as production-ready.
