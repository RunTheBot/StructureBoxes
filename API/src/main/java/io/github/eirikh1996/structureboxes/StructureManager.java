package io.github.eirikh1996.structureboxes;

import io.github.eirikh1996.structureboxes.settings.Settings;
import io.github.eirikh1996.structureboxes.utils.Location;
import javafx.util.Pair;

import java.util.*;

import static java.lang.System.currentTimeMillis;

public class StructureManager implements Iterable<ArrayList<Location>> {
    private final Set<ArrayList<Location>> locationSets = new HashSet<>();
    private final Map<UUID,  LinkedList<Pair<Long,HashMap<Location, String>>>> playerTimeStructureMap = new HashMap<>();
    private StructureManager() {}

    public boolean isPartOfStructure(Location location){
        for (ArrayList<Location> locationSet : locationSets){
            if (!locationSet.contains(location)){
                continue;
            }
            return true;
        }
        return false;
    }
    public void processRemovalOfSavedStructures(UUID id){
        LinkedList<Pair<Long, HashMap<Location, String>>> pairLinkedList = playerTimeStructureMap.get(id);
        long timeStamp = pairLinkedList.getLast().getKey();
        if (currentTimeMillis() - timeStamp > Settings.MaxSessionTime * 1000){
            pairLinkedList.pollLast();
        }
    }

    public HashMap<Location, String> getLatestStructure(UUID playerID){
        LinkedList<Pair<Long, HashMap<Location, String>>> pairLinkedList = playerTimeStructureMap.get(playerID);
        Pair<Long, HashMap<Location, String>> pair = pairLinkedList.pollFirst();
        return pair != null ? pair.getValue() : null;

    }
    public void addStructureByPlayer(UUID id, HashMap<Location, String> structure){
        Pair<Long, HashMap<Location, String>> timePair = new Pair<>(currentTimeMillis(), structure);
        if (playerTimeStructureMap.containsKey(id)){
            playerTimeStructureMap.get(id).addFirst(timePair);
        } else {
            LinkedList<Pair<Long, HashMap<Location, String>>> pairLinkedList = new LinkedList<>();
            pairLinkedList.addFirst(timePair);
            playerTimeStructureMap.put(id, pairLinkedList);
        }

    }

    public void addStructure(ArrayList<Location> structure){
        locationSets.add(structure);
    }

    public void removeStructure(ArrayList<Location> structure){
        locationSets.remove(structure);
    }

    @Override
    public Iterator<ArrayList<Location>> iterator() {
        return Collections.unmodifiableSet(locationSets).iterator();
    }

    public static synchronized StructureManager getInstance(){
        return StructureManagerHolder.instance;
    }

    private static class StructureManagerHolder{
        static StructureManager instance = new StructureManager();
    }
}
