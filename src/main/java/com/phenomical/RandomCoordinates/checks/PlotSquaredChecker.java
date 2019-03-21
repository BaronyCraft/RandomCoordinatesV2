/* This is disabled as PlotSquared isn't done for 1.13 yet and they say the api changes as well */

package com.phenomical.RandomCoordinates.checks;

import org.bukkit.Bukkit;
import org.bukkit.Location;

// import com.intellectualcrafters.plot.PS;
import com.jolbol1.RandomCoordinates.RandomCoords;
import com.jolbol1.RandomCoordinates.managers.DebugManager;

public class PlotSquaredChecker {
	DebugManager debugManager = new DebugManager();
	
    /**
     * Used to check whether or not the location is in a PlotSquared claim/road, or within the buffer.
     * @param l The location that we are checking.
     * @return True or False, is it a Plot claim or Road?
     */	
	public boolean PlotSquaredCheck(final Location l) {
		if (!(Bukkit.getServer().getPluginManager().getPlugin("PlotSquared") == null)) {
			if (RandomCoords.getPlugin().config.getString("PlotSquared").equals("true")) {
/*
				if(PS.get().hasPlotArea(l.getWorld().getName())) {
					debugManager.logToFile("World has Plot Area/s!");
					com.intellectualcrafters.plot.object.Location plotLocation = new com.intellectualcrafters.plot.object.Location(l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
					if(plotLocation.isPlotArea()) {
						debugManager.logToFile("Location in PlotArea!");
						
						if(plotLocation.isPlotRoad()) {
							debugManager.logToFile("Road detected!");
							return true;
						}
						
						if(plotLocation.isUnownedPlotArea()) {
							debugManager.logToFile("Unowned Plot found!");
							return false;
						}						
					}
				}
*/
			}
		}
		return false;
	}
}
