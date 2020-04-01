/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.EnumSet;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;

/**
 * MapBase with default input event handling.
 */
public class Map extends MapBase {

    public enum ManipulationModes {
        ROTATE, TRANSLATE, ZOOM;

        public static final EnumSet<ManipulationModes> ALL = EnumSet.allOf(ManipulationModes.class);
        public static final EnumSet<ManipulationModes> DEFAULT = EnumSet.of(ManipulationModes.TRANSLATE, ManipulationModes.ZOOM);
    }

    private static final StyleablePropertyFactory<Map> propertyFactory
            = new StyleablePropertyFactory<>(MapBase.getClassCssMetaData());

    private static final CssMetaData<Map, Number> mouseWheelZoomDeltaCssMetaData
            = propertyFactory.createSizeCssMetaData("-fx-mouse-wheel-zoom-delta", s -> s.mouseWheelZoomDeltaProperty);

    private final StyleableDoubleProperty mouseWheelZoomDeltaProperty
            = new SimpleStyleableDoubleProperty(mouseWheelZoomDeltaCssMetaData, this, "mouseWheelZoomDelta", 1d);

    private final ObjectProperty<EnumSet<ManipulationModes>> manipulationModesProperty
            = new SimpleObjectProperty<>(this, "manipulationModes", ManipulationModes.DEFAULT);

    private final ReadOnlyBooleanWrapper mouseDraggingProperty = new ReadOnlyBooleanWrapper(this, "mouseDragging");

    private Point2D mousePosition;

    public Map() {
        getStyleClass().add("map");

        addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (getManipulationModes().contains(ManipulationModes.TRANSLATE)
                    && e.getTarget() == this
                    && e.getButton() == MouseButton.PRIMARY
                    && e.getClickCount() == 1) {

                mousePosition = new Point2D(e.getX(), e.getY());
                mouseDraggingProperty.set(true);
                setCursor(Cursor.CLOSED_HAND);
            }
        });

        addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (e.getTarget() == this && e.getButton() == MouseButton.PRIMARY) {
                mousePosition = null;
                mouseDraggingProperty.set(false);
                setCursor(null);
            }
        });

        addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (mousePosition != null) {
                Point2D position = new Point2D(e.getX(), e.getY());
                translateMap(position.subtract(mousePosition));
                mousePosition = position;
            }
        });

        addEventHandler(ScrollEvent.SCROLL, e -> {
            if (getManipulationModes().contains(ManipulationModes.ZOOM)
                    && e.getTouchCount() == 0
                    && !e.isInertia()) { // handle only mouse wheel events

                Point2D center = getManipulationModes().contains(ManipulationModes.TRANSLATE)
                        ? new Point2D(e.getX(), e.getY())
                        : new Point2D(getWidth() / 2d, getHeight() / 2d);

                zoomMap(center, getTargetZoomLevel() + Math.signum(e.getDeltaY()) * getMouseWheelZoomDelta(), true);
            }
        });

        addEventHandler(ZoomEvent.ZOOM, e -> {
            if (getManipulationModes().contains(ManipulationModes.ZOOM)) {
                Point2D center = getManipulationModes().contains(ManipulationModes.TRANSLATE)
                        ? new Point2D(e.getX(), e.getY())
                        : new Point2D(getWidth() / 2d, getHeight() / 2d);

                zoomMap(center, getZoomLevel() + Math.log(e.getZoomFactor()) / Math.log(2d), false);
            }
        });

        addEventHandler(RotateEvent.ROTATE, e -> {
            if (getManipulationModes().contains(ManipulationModes.ROTATE)) {
                Point2D center = getManipulationModes().contains(ManipulationModes.TRANSLATE)
                        ? new Point2D(e.getX(), e.getY())
                        : new Point2D(getWidth() / 2d, getHeight() / 2d);

                rotateMap(center, getHeading() + e.getAngle(), false);
            }
        });
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return propertyFactory.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    public final DoubleProperty mouseWheelZoomDeltaProperty() {
        return mouseWheelZoomDeltaProperty;
    }

    public final double getMouseWheelZoomDelta() {
        return mouseWheelZoomDeltaProperty.get();
    }

    public final void setMouseWheelZoomDelta(double mouseWheelZoomDelta) {
        mouseWheelZoomDeltaProperty.set(mouseWheelZoomDelta);
    }

    public final ObjectProperty<EnumSet<ManipulationModes>> manipulationModesProperty() {
        return manipulationModesProperty;
    }

    public final EnumSet<ManipulationModes> getManipulationModes() {
        return manipulationModesProperty.get();
    }

    public final void setManipulationModes(EnumSet<ManipulationModes> manipulationModes) {
        manipulationModesProperty.set(manipulationModes);
    }

    public final ReadOnlyBooleanProperty mouseDraggingProperty() {
        return mouseDraggingProperty.getReadOnlyProperty();
    }

    public final boolean isMouseDragging() {
        return mouseDraggingProperty.get();
    }
}
