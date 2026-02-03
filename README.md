AutoSaveWorld
=============

AutoSaveWorld is a Bukkit/Spigot plugin that automates world saves and adds a full toolbox for backups, purge routines, scheduled restarts, and maintenance commands.

Originally developed by [Shevchik](https://github.com/Shevchik) and [ChanceSD](https://github.com/ChanceSD).

Key features
------------
- Automated world saves with configurable intervals and broadcast messages.
- Multiple backup targets: local filesystem, FTP, SFTP, Dropbox, Google Drive, or custom scripts.
- Purge tools for inactive players (with integrations for common protection/permission plugins).
- Scheduled restarts, crash restarts, and pre-stop command hooks.
- World regeneration with protection-aware preservation (WorldGuard, Factions, GriefPrevention, Towny, PreciousStones).
- Process manager and plugin manager commands for server maintenance.
- Server status, locale switching, reloads, and manual maintenance commands.

Download
--------
- Use the Releases page of this repository for prebuilt jars (if available).
- Or build from source:
  - `mvn -B clean install`
  - The jar is created in `target/`.

Install
-------
1. Drop the jar into your server `plugins` folder.
2. Start the server once to generate:
   - `plugins/AutoSaveWorld/config.yml`
   - `plugins/AutoSaveWorld/configmsg.yml`
3. Configure the plugin and reload or restart the server.

Commands
--------
- `/asw save` or `/save` - Run a save now.
- `/asw backup` or `/backup` - Run a backup now.
- `/asw purge` or `/purge` - Purge inactive player data.
- `/asw restart` - Restart with countdown.
- `/asw forcerestart` - Restart without countdown.
- `/asw regenworld <world> <worldregionsfolder>` - Regenerate a world.
- `/asw pmanager load|unload|reload <plugin>` - Manage plugins.
- `/asw process start|stop|list|input|output ...` - Manage external processes.
- `/asw serverstatus` - Show CPU, memory, and disk usage.
- `/asw reload` - Reload all configs.
- `/asw reloadconfig` - Reload `config.yml`.
- `/asw reloadmsg` - Reload `configmsg.yml`.
- `/asw locale available|load <locale>` - Manage message locales.
- `/asw version` - Show plugin version.

Permissions
-----------
Permissions follow the pattern `autosaveworld.<subcommand>`, for example:
- `autosaveworld.save`
- `autosaveworld.backup`
- `autosaveworld.purge`

Configuration overview
----------------------
`config.yml` includes settings for:
- Save interval and save behavior (including region cache dumps).
- Backup scheduling, retention, destinations, and compression.
- Purge scheduling, inactivity thresholds, and integrations.
- Restart scheduling, crash handling, and countdowns.
- Timed console command execution.
- World regeneration behavior and protection preservation.

`configmsg.yml` controls all messages and locale settings.

License
-------
This plugin is licensed under the GNU GPLv3 (see `LICENSE`).
