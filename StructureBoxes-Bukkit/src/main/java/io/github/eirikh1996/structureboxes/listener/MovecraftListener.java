package io.github.eirikh1996.structureboxes.listener;

import io.github.eirikh1996.structureboxes.StructureManager;
import io.github.eirikh1996.structureboxes.localisation.I18nSupport;
import io.github.eirikh1996.structureboxes.utils.Location;
import io.github.eirikh1996.structureboxes.utils.MovecraftUtils;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;

import static io.github.eirikh1996.structureboxes.utils.ChatUtils.COMMAND_PREFIX;

public class MovecraftListener implements Listener {

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event) {
        if (event.getCraft().getNotificationPlayer() == null) {
            return;
        }
        LinkedList<AbstractMap.SimpleImmutableEntry<Long, AbstractMap.SimpleImmutableEntry<String, HashMap<Location, Object>>>> sessions = StructureManager.getInstance().getSessions(event.getCraft().getNotificationPlayer().getUniqueId());

        for (AbstractMap.SimpleImmutableEntry<Long, AbstractMap.SimpleImmutableEntry<String, HashMap<Location, Object>>> session : sessions) {
            boolean terminate = false;
            for (MovecraftLocation ml : event.getCraft().getHitBox()) {
                if (!session.getValue().getValue().containsKey(MovecraftUtils.movecraftToSBloc(event.getCraft().getW(), ml))) {
                    continue;
                }
                event.getCraft().getNotificationPlayer().sendMessage(COMMAND_PREFIX + I18nSupport.getInternationalisedString("Movecraft - Session will expire"));
                terminate = true;
                break;
            }
            if (terminate) {
                break;
            }
        }

    }

    @EventHandler
    public void onCraftTranslate(CraftTranslateEvent event) {
        if (event.getCraft().getNotificationPlayer() == null) {
            return;
        }
        LinkedList<AbstractMap.SimpleImmutableEntry<Long, AbstractMap.SimpleImmutableEntry<String, HashMap<Location, Object>>>> sessions = StructureManager.getInstance().getSessions(event.getCraft().getNotificationPlayer().getUniqueId());

        for (AbstractMap.SimpleImmutableEntry<Long, AbstractMap.SimpleImmutableEntry<String, HashMap<Location, Object>>> session : sessions) {
            boolean terminate = false;
            for (MovecraftLocation ml : event.getOldHitBox()) {
                if (!session.getValue().getValue().containsKey(MovecraftUtils.movecraftToSBloc(event.getCraft().getW(), ml))) {
                    continue;
                }
                event.getCraft().getNotificationPlayer().sendMessage(COMMAND_PREFIX + I18nSupport.getInternationalisedString("Movecraft - Removed due to motion"));
                sessions.remove(session);
                terminate = true;
                break;
            }
            if (terminate) {
                break;
            }
        }
    }

    @EventHandler
    public void onCraftRotate(CraftRotateEvent event) {
        if (event.getCraft().getNotificationPlayer() == null) {
            return;
        }
        LinkedList<AbstractMap.SimpleImmutableEntry<Long, AbstractMap.SimpleImmutableEntry<String, HashMap<Location, Object>>>> sessions = StructureManager.getInstance().getSessions(event.getCraft().getNotificationPlayer().getUniqueId());

        for (AbstractMap.SimpleImmutableEntry<Long, AbstractMap.SimpleImmutableEntry<String, HashMap<Location, Object>>> session : sessions) {
            boolean terminate = false;
            for (MovecraftLocation ml : event.getOldHitBox()) {
                if (!session.getValue().getValue().containsKey(MovecraftUtils.movecraftToSBloc(event.getCraft().getW(), ml))) {
                    continue;
                }
                event.getCraft().getNotificationPlayer().sendMessage(COMMAND_PREFIX + I18nSupport.getInternationalisedString("Movecraft - Removed due to motion"));
                sessions.remove(session);
                terminate = true;
                break;
            }
            if (terminate) {
                break;
            }
        }
    }
}