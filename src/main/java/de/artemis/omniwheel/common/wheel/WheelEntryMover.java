package de.artemis.omniwheel.common.wheel;

import de.artemis.omniwheel.common.action.OpenWheelAction;
import de.artemis.omniwheel.common.profile.WheelProfile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/** Atomic moves: move uses editor-list boundaries; moveRadial uses radial-layout boundaries. */
public final class WheelEntryMover {
    private WheelEntryMover() {
    }

    public static boolean canMove(WheelProfile profile, String sourceId, String entryId, String destinationId) {
        if (profile == null || sourceId == null || entryId == null || destinationId == null
                || !profile.wheels().containsKey(sourceId) || !profile.wheels().containsKey(destinationId)) {
            return false;
        }
        WheelEntry entry = profile.wheel(sourceId).entries().stream().filter(value -> value.id().equals(entryId)).findFirst().orElse(null);
        return entry != null && (!(entry.action() instanceof OpenWheelAction submenu)
                || !reaches(profile, submenu.wheelId(), destinationId, new HashSet<>()));
    }

    private static boolean reaches(WheelProfile profile, String wheelId, String destinationId, Set<String> visited) {
        if (wheelId.equals(destinationId)) {
            return true;
        }
        if (!visited.add(wheelId) || !profile.wheels().containsKey(wheelId)) {
            return false;
        }
        for (WheelEntry entry : profile.wheel(wheelId).entries()) {
            if (entry.action() instanceof OpenWheelAction submenu && reaches(profile, submenu.wheelId(), destinationId, visited)) {
                return true;
            }
        }
        return false;
    }

    public static Result move(WheelProfile profile, String sourceId, String entryId, String destinationId, int insertionIndex) {
        if (!canMove(profile, sourceId, entryId, destinationId)) {
            throw new IllegalArgumentException("Invalid entry move");
        }
        WheelDefinition source = profile.wheel(sourceId);
        WheelDefinition destination = profile.wheel(destinationId);
        var sourceEntries = new ArrayList<>(source.entries());
        int sourceIndex = -1;
        for (int index = 0; index < sourceEntries.size(); index++) {
            if (sourceEntries.get(index).id().equals(entryId)) {
                sourceIndex = index;
                break;
            }
        }
        WheelEntry moved = sourceEntries.remove(sourceIndex);
        var destinationEntries = sourceId.equals(destinationId) ? sourceEntries : new ArrayList<>(destination.entries());
        int index = de.artemis.omniwheel.common.Numbers.clamp(insertionIndex, 0, destination.entries().size());
        if (sourceId.equals(destinationId) && sourceIndex < index) {
            index--;
        }
        Set<String> ids = new HashSet<>();
        destinationEntries.forEach(entry -> ids.add(entry.id()));
        String movedId = entryId;
        for (int suffix = 2; ids.contains(movedId); suffix++) {
            movedId = entryId + "_" + suffix;
        }
        if (!movedId.equals(entryId)) {
            moved = new WheelEntry(movedId, moved.label(), moved.description(), moved.glyph(), moved.color(), moved.guiColor(),
                    moved.action(), moved.active(), moved.showIcon(), moved.showLabel(), moved.showShortcut(), moved.shortcut());
        }
        destinationEntries.add(de.artemis.omniwheel.common.Numbers.clamp(index, 0, destinationEntries.size()), moved);
        var wheels = new LinkedHashMap<>(profile.wheels());
        wheels.put(sourceId, withEntries(source, sourceEntries));
        wheels.put(destinationId, withEntries(destination, destinationEntries));
        return new Result(new WheelProfile(profile.id(), profile.displayName(), profile.rootWheelId(), wheels), movedId);
    }

    private static WheelDefinition withEntries(WheelDefinition wheel, java.util.List<WheelEntry> entries) {
        return new WheelDefinition(wheel.id(), wheel.title(), wheel.description(), de.artemis.omniwheel.common.Numbers.clamp(entries.size(), 1, 8), entries, wheel.active(), wheel.radialOrder());
    }

    /** Preview changes radial positions without reorganizing editor rows. */
    public static Result moveRadial(WheelProfile profile, String sourceId, String entryId, String destinationId, int insertionIndex) {
        if (!canMove(profile, sourceId, entryId, destinationId)) throw new IllegalArgumentException("Invalid entry move");
        WheelDefinition destination = profile.wheel(destinationId);
        var order = new ArrayList<>(destination.radialOrder());
        Result result;
        int index = de.artemis.omniwheel.common.Numbers.clamp(insertionIndex, 0, order.size());
        if (sourceId.equals(destinationId)) {
            int previous = order.indexOf(entryId);
            order.remove(previous);
            if (previous < index) index--;
            result = new Result(profile, entryId);
        } else {
            result = move(profile, sourceId, entryId, destinationId, destination.entries().size());
        }
        order.add(de.artemis.omniwheel.common.Numbers.clamp(index, 0, order.size()), result.entryId());
        destination = result.profile().wheel(destinationId);
        var wheels = new LinkedHashMap<>(result.profile().wheels());
        wheels.put(destinationId, new WheelDefinition(destination.id(), destination.title(), destination.description(),
                destination.segmentCount(), destination.entries(), destination.active(), order));
        return new Result(new WheelProfile(profile.id(), profile.displayName(), profile.rootWheelId(), wheels), result.entryId());
    }

    public record Result(WheelProfile profile, String entryId) {
    }
}
