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

import org.bukkit.World;

import java.util.Set;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;

public class FactionsDataProvider extends DataProvider {

	public FactionsDataProvider(World world) throws Throwable {
		super(world);
	}

	@Override
	protected void init() throws Throwable {
		Board board = Board.getInstance();
		if (board == null) {
			return;
		}
		for (final Faction f : Factions.getInstance().getAllFactions()) {
			Set<FLocation> claims = board.getAllClaims(f);
			if (claims == null) {
				continue;
			}
			for (FLocation floc : claims) {
				String worldName = floc.getWorldName();
				if (worldName != null && worldName.equalsIgnoreCase(world.getName())) {
					addChunkAtCoord(floc.getIntX(), floc.getIntZ());
				}
			}
		}
	}

}
