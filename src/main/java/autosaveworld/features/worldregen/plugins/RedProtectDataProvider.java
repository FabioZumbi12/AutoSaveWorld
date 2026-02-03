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

package autosaveworld.features.worldregen.plugins;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Bukkit.region.RegionManager;

public class RedProtectDataProvider extends DataProvider {

	public RedProtectDataProvider(World world) throws Throwable {
		super(world);
	}

	@Override
	protected void init() throws Throwable {
		RedProtect redProtect = RedProtect.get();
		if (redProtect == null) {
			return;
		}
		RegionManager regionManager = redProtect.getRegionManager();
		if (regionManager == null) {
			return;
		}
		Set<Region> regions = regionManager.getRegionsByWorld(world.getName());
		if (regions == null || regions.isEmpty()) {
			return;
		}
		for (Region region : regions) {
			if (region == null) {
				continue;
			}
			Location min = region.getMinLocation();
			Location max = region.getMaxLocation();
			if (min == null || max == null) {
				continue;
			}
			addChunksInBounds(
				min.getBlockX(),
				min.getBlockZ(),
				max.getBlockX(),
				max.getBlockZ()
			);
		}
	}

}
