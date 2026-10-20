# Velocity Combat

Velocity Combat is an original Minecraft Forge mod focused on fast, skill-driven combat. It adds a short burst dash, a timed parry that counters incoming melee attacks, quick hotbar weapon cycling, and a combo/style score for landing hits and parries.

## Compatibility

- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.2.x
- Java 17

## Controls

| Action | Default key | Effect |
| --- | --- | --- |
| Combat Dash | R | Burst in the direction you are looking; 1-second cooldown |
| Parry | G | Open an 8-tick parry window; 2-second cooldown |
| Cycle Weapon | V | Select the next occupied hotbar slot |

All controls can be changed in Minecraft's Controls menu. Successful hits and parries build a combo and award style points, shown in the action bar. The combo expires after 5 seconds without another scored action.

## Build

Install a Java 17 JDK, then run the Gradle wrapper from the repository root:

```powershell
.\gradlew.bat build
```

On macOS or Linux, run:

```text
./gradlew build
```

The distributable mod jar is created in `build/libs/velocitycombat-1.0.0.jar`. A ready-to-install copy is checked in at [`release/velocitycombat-1.0.0.jar`](release/velocitycombat-1.0.0.jar). Install Forge 47.2.x for Minecraft 1.20.1 and copy the jar into the instance's `mods` folder, or upload it as a Minecraft Java Forge file on CurseForge.
