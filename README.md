# Barracuda Trial Plugin

A RuneLite plugin for the Barracuda Trial sailing minigame. Displays an optimal path to collect lost supplies in order, highlights lightning clouds and their danger zones, shows rocks and speed boost areas, and tracks rum pickup locations. Path calculation uses A* pathfinding and considers boat position, turning costs, obstacles, and speed boosts. Configurable colors and display options for all overlays.

![Barracuda Trial Example](docs/barracuda_trial_example.png)

## Known Issues

- **Difficulty Support:** The plugin has been primarily tested on **Swordfish** difficulty. **Shark** and **Marlin** difficulties have known issues and only partially work - the optimal path feature may behave incorrectly. For these difficulties, you can disable the "Show Optimal Path" option in the plugin settings and still use the other helpful features like supply highlights, cloud danger zones, and rock/boost markers.
- **Pathing sometimes makes weird choices**, especially mid-route. Ignore suggestions to go backwards.
- **Rum pickup/dropoff highlighting is not working.** The locations are tracked but not rendered correctly.
- **Debug mode needs cleanup.** Once everything is stable, debug logging and performance tracking should be removed.
- **Code needs refactoring.** The plugin is over 1500 lines and should be split into smaller, more maintainable modules.
