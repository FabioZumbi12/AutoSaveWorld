/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package autosaveworld.features.save;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import autosaveworld.core.AutoSaveWorld;
import autosaveworld.core.logging.MessageLogger;
import autosaveworld.utils.BukkitUtils;
import autosaveworld.utils.CollectionsUtils;
import autosaveworld.utils.ReflectionUtils;
import autosaveworld.utils.SchedulerUtils;
import autosaveworld.utils.Threads.IntervalTaskThread;

public class AutoSaveThread extends IntervalTaskThread {

	public AutoSaveThread() {
		super("AutoSaveThread");
	}

	@Override
	public void onStart() {
		//disable bukkit built-in autosave
		try {
			Server server = Bukkit.getServer();
			Object minecraftserver = ReflectionUtils.getField(server.getClass(), "console").get(server);
			ReflectionUtils.getField(minecraftserver.getClass(), "autosavePeriod").set(minecraftserver, 0);
		} catch (Throwable t) {
		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public int getInterval() {
		return AutoSaveWorld.getInstance().getMainConfig().saveInterval;
	}

	@Override
	public void doTask() {
		if (AutoSaveWorld.getInstance().getBackupThread().isBackupInProcess()) {
			MessageLogger.debug("Backup is running, skipping autosave");
			return;
		}

		performSave();
	}

	public void performSaveNow() {
		MessageLogger.broadcast(AutoSaveWorld.getInstance().getMessageConfig().messageSaveBroadcastPre, AutoSaveWorld.getInstance().getMainConfig().saveBroadcast);

		MessageLogger.debug("Saving players");
		Bukkit.savePlayers();
		MessageLogger.debug("Saved Players");
		MessageLogger.debug("Saving worlds");
		for (World w : Bukkit.getWorlds()) {
			saveWorld(w);
		}
		MessageLogger.debug("Saved Worlds");

		MessageLogger.broadcast(AutoSaveWorld.getInstance().getMessageConfig().messageSaveBroadcastPost, AutoSaveWorld.getInstance().getMainConfig().saveBroadcast);
	}

	public void performSave() {

		MessageLogger.broadcast(AutoSaveWorld.getInstance().getMessageConfig().messageSaveBroadcastPre, AutoSaveWorld.getInstance().getMainConfig().saveBroadcast);

		// Save the players
		MessageLogger.debug("Saving players");
		for (final Collection<Player> playersPart : CollectionsUtils.split(BukkitUtils.getOnlinePlayers(), 6)) {
			SchedulerUtils.callSyncTaskAndWait(new Runnable() {
				@Override
				public void run() {
					for (Player player : playersPart) {
						player.saveData();
					}
				}
			});
		}
		MessageLogger.debug("Saved Players");

		// Save the worlds
		MessageLogger.debug("Saving worlds");
		for (final World world : Bukkit.getWorlds()) {
			SchedulerUtils.callSyncTaskAndWait(new Runnable() {
				@Override
				public void run() {
					saveWorld(world);
				}
			});
		}
		MessageLogger.debug("Saved Worlds");

		// Dump region cache
		if (AutoSaveWorld.getInstance().getMainConfig().saveDumpRegionCache) {
			MessageLogger.debug("Dumping cache");
			for (World world : Bukkit.getWorlds()) {
				dumpRegionCache(world);
			}
			MessageLogger.debug("Dumped cache");
		}

		MessageLogger.broadcast(AutoSaveWorld.getInstance().getMessageConfig().messageSaveBroadcastPost, AutoSaveWorld.getInstance().getMainConfig().saveBroadcast);
	}

	private void dumpRegionCache(World world) {
		if (world.isAutoSave()) {
			try {
				Object worldserver = getNMSWorld(world);
				// invoke saveLevel method which waits for all chunks to save and than dumps RegionFileCache
				if (!tryInvokeSaveLevel(worldserver)) {
					invokeSaveFallback(worldserver);
				}
			} catch (Exception e) {
				MessageLogger.exception("Could not dump RegionFileCache", e);
			}
		}
	}

	private boolean tryInvokeSaveLevel(Object worldserver) throws Exception {
		try {
			ReflectionUtils.getMethod(worldserver.getClass(), NMSNames.getSaveLevelMethodName(), 0).invoke(worldserver);
			return true;
		} catch (RuntimeException ex) {
			if (ex.getMessage() != null && ex.getMessage().startsWith("Can't find method")) {
				return false;
			}
			throw ex;
		}
	}

	private void invokeSaveFallback(Object worldserver) throws Exception {
		Method saveMethod = findSaveMethod(worldserver.getClass());
		if (saveMethod == null) {
			throw new RuntimeException("Can't find compatible save method for dumpRegionCache");
		}
		saveMethod.setAccessible(true);
		Object[] args = buildSaveArgs(saveMethod.getParameterTypes());
		saveMethod.invoke(worldserver, args);
	}

	private Method findSaveMethod(Class<?> clazz) {
		Method best = null;
		int bestScore = -1;
		for (Class<?> type = clazz; type != null; type = type.getSuperclass()) {
			for (Method method : type.getDeclaredMethods()) {
				if (!"save".equals(method.getName())) {
					continue;
				}
				Class<?>[] params = method.getParameterTypes();
				if (!areSupportedSaveParams(params)) {
					continue;
				}
				int score = params.length;
				if (params.length == 3 && isBooleanType(params[1]) && isBooleanType(params[2])) {
					score += 3;
				} else if (params.length == 2 && isBooleanType(params[0]) && isBooleanType(params[1])) {
					score += 2;
				} else if (params.length == 1 && isBooleanType(params[0])) {
					score += 1;
				}
				if (score > bestScore) {
					best = method;
					bestScore = score;
				}
			}
		}
		return best;
	}

	private boolean areSupportedSaveParams(Class<?>[] params) {
		for (Class<?> param : params) {
			if (param.isPrimitive() && !isBooleanType(param)) {
				return false;
			}
		}
		return true;
	}

	private boolean isBooleanType(Class<?> type) {
		return type == boolean.class || type == Boolean.class;
	}

	private Object[] buildSaveArgs(Class<?>[] params) {
		Object[] args = new Object[params.length];
		for (int i = 0; i < params.length; i++) {
			if (isBooleanType(params[i])) {
				args[i] = Boolean.TRUE;
			} else {
				args[i] = null;
			}
		}
		return args;
	}

	private void saveWorld(World world) {
		if (!world.isAutoSave()) {
			return;
		}

		if (AutoSaveWorld.getInstance().getMainConfig().saveDisableStructureSaving && needSaveWorkAround()) {
			saveWorldDoNoSaveStructureInfo(world);
		} else {
			saveWorldNormal(world);
		}
	}

	private boolean needSaveWorkAround() {
		return true;
	}

	private void saveWorldNormal(World world) {
		world.save();
	}

	private void saveWorldDoNoSaveStructureInfo(World world) {
		try {
			// get worldserver, dataManager, chunkProvider, worldData
			Object worldserver = getNMSWorld(world);
			Object dataManager = ReflectionUtils.getField(worldserver.getClass(), NMSNames.getDataManagerFieldName()).get(worldserver);
			Object chunkProvider = ReflectionUtils.getField(worldserver.getClass(), NMSNames.getChunkProviderFieldName()).get(worldserver);
			Object worldData = ReflectionUtils.getField(worldserver.getClass(), NMSNames.getWorldDataFieldName()).get(worldserver);
			// invoke check session
			ReflectionUtils.getMethod(dataManager.getClass(), NMSNames.getCheckSessionMethodName(), 0).invoke(dataManager);
			// invoke saveWorldData
			ReflectionUtils.getMethod(dataManager.getClass(), NMSNames.getSaveWorldDataMethodName(), 2).invoke(dataManager, worldData, null);
			// invoke saveChunks
			try {
				ReflectionUtils.getMethod(chunkProvider.getClass(), NMSNames.getSaveChunksMethodName(), 2).invoke(chunkProvider, true, null);
			} catch (RuntimeException e) {
				ReflectionUtils.getMethod(chunkProvider.getClass(), "a", 1).invoke(chunkProvider, true);
			}
		} catch (Throwable e) {
			MessageLogger.exception("Failed to workaround stucture saving, saving world using normal methods", e);
			saveWorldNormal(world);
		}
	}

	private Object getNMSWorld(World world) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return ReflectionUtils.getMethod(world.getClass(), "getHandle", 0).invoke(world);
	}

}
