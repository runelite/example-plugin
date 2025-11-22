# Barracuda Trial Plugin

A RuneLite plugin to help navigate Barracuda Trials by displaying optimal routes and avoiding obstacles.

![Barracuda Trial Example](docs/barracuda_trial_example.png)

## Known Issues

- **Only Tempor Tantrum supported.** Route data for Jubbly Jive and Gwenith Glide trials needs to be captured.
- **Rum pickup/dropoff highlighting is not working.** The locations are tracked but not rendered correctly.
- **Debug mode needs cleanup.** Once everything is stable, debug logging and performance tracking should be removed.

## Approach

Barracuda Trials require sailing circular laps while collecting lost shipments and rum. There are 3 different trials (Tempor Tantrum, Jubbly Jive, Gwenith Glide), each with 3 difficulties (Swordfish/Shark/Marlin = 1/2/3 laps).

**The core challenge:** Shipments aren't all visible at start. You only see nearby ones as you sail past. The game doesn't reveal distant shipments until you're closer, making it appear random - but the locations are actually static and fixed per trial/difficulty.

**Our solution:** Use predefined static routes with hardcoded shipment locations. Even though you can't see distant shipments yet, we know they exist at specific coordinates and can route toward them. The plugin highlights the next expected shipment in sequence. If you miss one, it directs you back.

**Tactical navigation:** Between route waypoints, A\* pathfinding handles real-time navigation around moving lightning clouds and static rocks while preferring speed boost tiles.
