# Velocity Combat

Velocity Combat is an original Minecraft Forge mod focused on fast, skill-driven combat. It adds a short burst dash, a timed parry that counters incoming melee attacks, two custom weapons, advanced movement abilities, close-combat vitality recovery, quick hotbar weapon cycling, and a combo/style score.

## Compatibility

- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.2.x
- Java 17

## Controls

| Action | Default key | Effect |
| --- | --- | --- |
| Combat Dash | R | Burst in the direction you are looking; 1-second cooldown |
| Combat Slide | Left Shift | While sprinting on the ground, tap to slide forward; normal sneak is unchanged when not running |
| Wall Kick | Jump (Space) | Jump against a wall while airborne to kick away and gain height |
| Ground Slam | X | While airborne, dive down and create a damaging knockback shockwave on landing |
| Parry | G | Open an 8-tick parry window; 2-second cooldown |
| Cycle Weapon | V | Select the next occupied hotbar slot |
| Fire shotgun / launch token | Right-click | Fire the Breach Shotgun or throw a Token Round from the Token Revolver |
| Fire Token Revolver | Left-click | Shoot; aim through a flying token to send it toward the nearest visible enemy |

The Breach Shotgun fires a close-range spread of pellets and has an iron-hoe appearance. Right-clicking the Token Revolver launches a physical Token Round; left-clicking fires the gun, and a shot that intersects a thrown token sends it homing toward the nearest visible enemy. The revolver has a blaze-rod appearance with a token mounted on top; this does not change the separate Token Round projectile/item. Jumping during a slide boosts forward speed while limiting jump height. Wall Kick uses the normal jump button against nearby walls while airborne. Ground Slam dives down and creates a damaging knockback shockwave on landing; jumping within 6 ticks after impact doubles the jump height. Vitality restores half a heart on a close-range direct melee hit, with a short cooldown. Both weapons use custom 3D geometry, dedicated pixel textures, and explicit first-person, third-person, and GUI transforms; they are available in the Combat creative tab. All controls can be changed in Minecraft's Controls menu. Successful hits and parries build a combo and award style points, shown in the action bar. A successful parry flashes the parrying player's screen white at 50% opacity for 0.3 seconds. During the flash, local player movement, view rotation, keyboard/mouse actions, and interactions are briefly held. Forge does not expose a safe way here to pause the complete client-world tick/render loop, so other entities and visual world simulation continue; server-authoritative updates remain active. The combo expires after 5 seconds without another scored action.

## Build

Install a Java 17 JDK, then run the Gradle wrapper from the repository root:

```powershell
.\gradlew.bat build
```

On macOS or Linux, run:

```text
./gradlew build
```

The distributable mod jar is created in `build/libs/velocitycombat-1.3.1.jar`. A ready-to-install copy is checked in at [`release/velocitycombat-1.3.1.jar`](release/velocitycombat-1.3.1.jar). Install Forge 47.2.x for Minecraft 1.20.1 and copy the jar into the instance's `mods` folder, or upload it as a Minecraft Java Forge file on CurseForge.
