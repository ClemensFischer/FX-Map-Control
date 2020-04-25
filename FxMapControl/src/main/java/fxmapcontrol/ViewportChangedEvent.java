/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * Fired by MapBase when the viewport changes.
 */
public class ViewportChangedEvent extends Event {

    public static final EventType<ViewportChangedEvent> VIEWPORT_CHANGED = new EventType<>(Event.ANY, "VIEWPORT_CHANGED");

    private final boolean projectionChanged;
    private final double longitudeOffset;

    public ViewportChangedEvent(MapBase source, boolean projectionChanged, double longitudeOffset) {
        super(source, Event.NULL_SOURCE_TARGET, VIEWPORT_CHANGED);
        this.projectionChanged = projectionChanged;
        this.longitudeOffset = longitudeOffset;
    }

    /**
     * Indicates if the map projection has changed, i.e. if a TileLayer or MapImageLayer should be
     * immediately updated, or MapPath Data in cartesian map coordinates should be recalculated.
     */
    public final boolean getProjectionChanged() {
        return projectionChanged;
    }

    /**
     * Offset of the map center longitude value from the previous viewport.
     * Used to detect if the map center has moved across 180° longitude.
     */
    public final double getLongitudeOffset() {
        return longitudeOffset;
    }
}
