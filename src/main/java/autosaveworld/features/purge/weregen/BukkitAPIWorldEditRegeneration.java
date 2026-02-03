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

package autosaveworld.features.purge.weregen;

import java.util.Iterator;
import java.util.LinkedList;

import org.bukkit.Material;
import org.bukkit.World;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;

import autosaveworld.core.logging.MessageLogger;
import autosaveworld.features.purge.weregen.UtilClasses.BlockToPlaceBack;
import autosaveworld.features.purge.weregen.UtilClasses.ItemSpawnListener;
import autosaveworld.features.purge.weregen.WorldEditRegeneration.WorldEditRegenrationInterface;
import autosaveworld.utils.BukkitUtils;
public class BukkitAPIWorldEditRegeneration implements WorldEditRegenrationInterface {

	private ItemSpawnListener itemremover = new ItemSpawnListener();

	@Override
	public void regenerateRegion(World world, org.bukkit.util.Vector minpoint, org.bukkit.util.Vector maxpoint) {
		BlockVector3 minbpoint = BlockVector3.at(minpoint.getBlockX(), minpoint.getBlockY(), minpoint.getBlockZ());
		BlockVector3 maxbpoint = BlockVector3.at(maxpoint.getBlockX(), maxpoint.getBlockY(), maxpoint.getBlockZ());
		regenerateRegion(world, minbpoint, maxbpoint);
	}

	@Override
	public void regenerateRegion(World world, BlockVector3 minpoint, BlockVector3 maxpoint) {
		int miny = getMinHeight(world);
		int maxy = world.getMaxHeight();
		Region region = new CuboidRegion(BukkitAdapter.adapt(world), minpoint, maxpoint);
		LinkedList<BlockToPlaceBack> placeBackQueue = new LinkedList<BlockToPlaceBack>();

		// register listener that will prevent trash items from spawning
		BukkitUtils.registerListener(itemremover);

		try (EditSession es = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
			// first save all blocks that are inside affected chunks but outside the region
			for (BlockVector2 chunk : region.getChunks()) {
				BlockVector3 min = BlockVector3.at(chunk.getBlockX() * 16, miny, chunk.getBlockZ() * 16);
				for (int x = 0; x < 16; ++x) {
					for (int y = miny; y < maxy; ++y) {
						for (int z = 0; z < 16; ++z) {
							BlockVector3 pt = min.add(x, y - miny, z);
							if (!region.contains(pt)) {
							placeBackQueue.add(new BlockToPlaceBack(pt, es.getBlock(pt)));
							}
						}
					}
				}
			}

			// regenerate all affected chunks
			for (BlockVector2 chunk : region.getChunks()) {
				try {
					world.regenerateChunk(chunk.getBlockX(), chunk.getBlockZ());
				} catch (Exception t) {
					MessageLogger.exception("Unable to regenerate chunk " + chunk.getBlockX() + " " + chunk.getBlockZ(), t);
				}
			}

			// set all blocks that were outside the region back
			Iterator<BlockToPlaceBack> entryit = placeBackQueue.iterator();
			while (entryit.hasNext()) {
				BlockToPlaceBack blockToPlaceBack = entryit.next();
				BlockState block = blockToPlaceBack.getBlock();
				BlockVector3 pt = blockToPlaceBack.getPosition();
				try {
					// set block to air to fix one really weird problem
					world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()).setType(Material.AIR);
					es.setBlock(pt, block);
				} catch (Exception t) {
					MessageLogger.exception("Unable to place back block " + pt.getBlockX() + " " + pt.getBlockY() + " " + pt.getBlockZ(), t);
				} finally {
					entryit.remove();
				}
			}
		} finally {
			// unregister listener that prevents item drop
			BukkitUtils.unregisterListener(itemremover);
		}
	}

	private int getMinHeight(World world) {
		try {
			return world.getMinHeight();
		} catch (NoSuchMethodError ignored) {
			return 0;
		}
	}

}
