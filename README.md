# Velocity Combat

Velocity Combat is an original Minecraft Forge mod focused on fast, skill-driven combat. It adds a short burst dash, a timed parry that counters incoming melee attacks, two hitscan weapons, quick hotbar weapon cycling, and a combo/style score for landing hits and parries.

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
| Fire weapon | Right-click | Fire the selected weapon |

The Breach Shotgun fires a close-range spread of pellets. The Token Revolver fires a powerful shot that can ricochet between nearby, visible enemies. Both weapons are available in the Combat creative tab and use vanilla item textures as icons. All controls can be changed in Minecraft's Controls menu. Successful hits and parries build a combo and award style points, shown in the action bar. A successful parry flashes the parrying player's screen white for 0.1 seconds; this local visual hit-stop does not pause the server or other players. The combo expires after 5 seconds without another scored action.

## Build

Install a Java 17 JDK, then run the Gradle wrapper from the repository root:

```powershell
.\gradlew.bat build
```

On macOS or Linux, run:

```text
./gradlew build
```

The distributable mod jar is created in `build/libs/velocitycombat-1.1.1.jar`. A ready-to-install copy is checked in at [`release/velocitycombat-1.1.1.jar`](release/velocitycombat-1.1.1.jar). Install Forge 47.2.x for Minecraft 1.20.1 and copy the jar into the instance's `mods` folder, or upload it as a Minecraft Java Forge file on CurseForge.
