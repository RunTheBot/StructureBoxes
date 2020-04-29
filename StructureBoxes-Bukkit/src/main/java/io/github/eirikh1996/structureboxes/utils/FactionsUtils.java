package io.github.eirikh1996.structureboxes.utils;

import com.massivecraft.factions.entity.*;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FactionsUtils {
    public static boolean allowBuild(Player player, Location location){
        PS ps = PS.valueOf(location);
        MPlayer mp = MPlayer.get(player);
        Faction f = BoardColl.get().getFactionAt(ps);
        if (mp.isOverriding()){
            return true;
        }
        return f.isPermitted(MPerm.getPermBuild(), f.getRelationTo(mp)) || BoardColl.get().getTerritoryAccessAt(ps).isMPlayerGranted(mp);
    }

    public static boolean withinRegion(Location location){
        return BoardColl.get().getFactionAt(PS.valueOf(location)) != FactionColl.get().getNone();

    }
}
