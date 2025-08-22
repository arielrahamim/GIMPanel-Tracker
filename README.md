# GIMPanel Tracker Plugin

A RuneLite plugin for real-time Group Ironman progress tracking with the GIMPanel dashboard.

## Features

- **Real-time Skill Tracking**: Monitors XP gains and level changes across all skills
- **Drop Detection**: Tracks item drops via chat message parsing
- **Location Tracking**: Monitors player location and activity (if enabled)
- **Quest Progress**: Tracks quest completions and status changes
- **Inventory Sharing**: Optional inventory and equipment sharing with group members
- **Live Player Status**: Tracks health, prayer, energy, and online status
- **API Integration**: Seamlessly syncs data with GIMPanel dashboard

## Installation

1. Build the plugin:
   ```bash
   ./gradlew build
   ```

2. The plugin JAR will be created in `build/libs/`
   - `example-1.0.0.jar` - Standard plugin
   - `example-1.0.0-all.jar` - Plugin with all dependencies (use this for distribution)

3. Copy the JAR file to your RuneLite plugins directory or install through the RuneLite Plugin Hub

## Configuration

Configure the plugin through the RuneLite settings panel:

### Required Settings
- **GIMPanel Backend URL**: Your GIMPanel backend URL (e.g., `https://your-backend.com` or `http://localhost:3000`)
- **Authentication Token**: Token provided by your GIMPanel group

### Optional Settings
- **Share Inventory**: Allow group members to see inventory contents (default: false)
- **Share Location**: Allow group members to see current location (default: true)
- **Share Resources**: Allow group members to see health, prayer, energy (default: true)
- **Update Interval**: How often to sync data with GIMPanel in seconds (default: 30)
- **Track Drops**: Enable drop tracking and notifications (default: true)
- **Track Skills**: Enable skill and XP tracking (default: true)
- **Track Quests**: Enable quest and achievement tracking (default: true)

## Data Collected

The plugin collects and transmits the following data to your GIMPanel dashboard:

### Player Data
- Username and display name
- Total level and combat level
- Total XP across all skills
- Online status and current world
- Current location and activity
- Health, prayer, and energy levels

### Skill Data
- Individual skill levels and XP
- XP gains and level changes
- Skill rankings (when available)

### Drop Data
- Item drops with quantities
- Drop source (when detectable)
- Item rarity classification
- Drop location and timestamp

### Quest Data
- Quest completion status
- Quest progress changes
- Quest points earned

### Activity Data
- Current player activity
- Location and region
- World information
- Idle status detection

## API Endpoints

The plugin communicates with the GIMPanel backend API:

- `POST /api/webhook` - Main webhook endpoint for all data types
  - Accepts: Skill updates, drops, activities, quests, player sync, heartbeat
  - Format: Form data with `payload_json` field containing JSON payload
  - Authentication: Token via query parameter or Authorization header

The plugin automatically formats data for the following notification types:
- `LEVEL` - Skill level changes
- `XP_GAIN` - Experience gains
- `LOOT` - Item drops
- `QUEST` - Quest completions
- `PLAYER_SYNC` - Player status updates
- `HEARTBEAT` - Online status pings

## Privacy & Security

- All data transmission uses HTTPS with bearer token authentication
- Inventory sharing is opt-in and disabled by default
- Location sharing can be disabled in settings
- No sensitive account information is collected or transmitted
- All data is sent only to your configured GIMPanel instance

## Technical Details

### Architecture
- **Main Plugin**: `GIMPanelTrackerPlugin` - Main plugin class with event subscriptions
- **Data Collectors**: Specialized classes for collecting different types of game data
- **Managers**: `DataManager` for API coordination, `StateTracker` for game state
- **Models**: Data structures for different types of tracked information
- **Utilities**: HTTP client and JSON serialization utilities

### Dependencies
- RuneLite Client API
- OkHttp 4.12.0 for HTTP communication
- Gson 2.10.1 for JSON serialization
- Lombok for code generation

### Performance
- Asynchronous API calls prevent game lag
- Rate limiting prevents API abuse
- Configurable update intervals
- Efficient queue-based data processing

## Development

### IDE Setup (VS Code/Cursor or IntelliJ)

**For VS Code/Cursor:**
1. Install Java Extension Pack and Gradle for Java extensions
2. Open the project folder in VS Code/Cursor
3. Use `Ctrl+Shift+P` → "Java: Run and Debug" → "Launch GIMPanel Tracker Plugin"
4. The project includes pre-configured `.vscode/` files for debugging and tasks

**For IntelliJ IDEA:**
1. Import as Gradle project
2. Create run configuration with main class `gimpanel.tracker.GIMPanelTrackerPluginTest`
3. Add VM options: `-ea -Drunelite.pluginhub.version=1.0.0`

### Building
```bash
./gradlew build
```

### Testing
```bash
./gradlew test
```

### Creating Distribution JAR
```bash
./gradlew shadowJar
```

### Running the Plugin in Development
```bash
# Method 1: Using the shadow JAR
java -ea -jar build/libs/example-1.0.0-all.jar

# Method 2: Using the test class directly (after build)
java -ea -cp "build/classes/java/main:build/classes/java/test:$(./gradlew -q dependencies --configuration testRuntimeClasspath | grep -v "^$" | tail -n +3)" gimpanel.tracker.GIMPanelTrackerPluginTest
```

See `DEVELOPMENT.md` for detailed development setup instructions.

## Support

For issues and feature requests, please check with your GIMPanel dashboard administrator or refer to the plugin documentation.

## License

This plugin is designed specifically for use with GIMPanel dashboard systems.