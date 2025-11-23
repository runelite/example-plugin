# Barracuda Trial Plugin

A RuneLite plugin to help navigate Barracuda Trials by displaying optimal routes and avoiding obstacles.
Uses A\* pathfinding to handle real-time navigation around moving lightning clouds and static rocks while preferring speed boost tiles

Supports all three difficulties of Tempor Tantrum trials.

![Barracuda Trial Example](docs/barracuda_trial_example.png)

## Known Issues

- **Rum pickup/dropoff highlighting is not working.** The locations are tracked but not rendered correctly.
- **Navigation not provided for distant waypoints.** When a supply's tile is out of view we do not currently render a path towards it.
- **Pathfinding can be improved.** There is a lot of tweaking we can do.
- **Debug mode needs cleanup.** Once everything is stable, debug logging and performance tracking should be removed.
