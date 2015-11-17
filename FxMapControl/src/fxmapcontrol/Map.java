/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableBooleanProperty;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;

/**
 * MapBase with default input event handling.
 */
public class Map extends MapBase {

    private static final StyleablePropertyFactory<Map> propertyFactory
            = new StyleablePropertyFactory<>(MapBase.getClassCssMetaData());

    private static final CssMetaData<Map, Boolean> rotationGestureEnabledCssMetaData
            = propertyFactory.createBooleanCssMetaData("-fx-rotation-gesture-enabled", s -> s.rotationGestureEnabledProperty);

    private static final CssMetaData<Map, Number> mouseWheelZoomDeltaCssMetaData
            = propertyFactory.createSizeCssMetaData("-fx-mouse-wheel-zoom-delta", s -> s.mouseWheelZoomDeltaProperty);

    private final StyleableBooleanProperty rotationGestureEnabledProperty
            = new SimpleStyleableBooleanProperty(rotationGestureEnabledCssMetaData, this, "rotationGestureEnabled");

    private final StyleableDoubleProperty mouseWheelZoomDeltaProperty
            = new SimpleStyleableDoubleProperty(mouseWheelZoomDeltaCssMetaData, this, "mouseWheelZoomDelta", 1d);

    private Point2D mousePosition;

    public Map() {
        getStyleClass().add("map");

        addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getTarget() == this
                    && e.isPrimaryButtonDown()
                    && e.getClickCount() == 1) {
                mousePosition = new Point2D(e.getX(), e.getY());
                setCursor(Cursor.CLOSED_HAND);
            }
        });

        addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            mousePosition = null;
            setCursor(null);
        });

        addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (mousePosition != null) {
                final Point2D position = new Point2D(e.getX(), e.getY());
                translateMap(position.subtract(mousePosition));
                mousePosition = position;
            }
        });

        addEventHandler(ScrollEvent.SCROLL, e -> {
            if (Math.abs(e.getTextDeltaY()) >= 1d) { // handle only mouse wheel
                zoomMap(new Point2D(e.getX(), e.getY()),
                        getTargetZoomLevel() + Math.signum(e.getTextDeltaY()) * getMouseWheelZoomDelta(),
                        true);
            }
        });

        addEventHandler(ZoomEvent.ZOOM, e -> {
            zoomMap(new Point2D(e.getX(), e.getY()),
                    getZoomLevel() + Math.log(e.getZoomFactor()) / Math.log(2d),
                    false);
        });

        final EventHandler<RotateEvent> rotationGestureHandler = e -> {
            rotateMap(new Point2D(e.getX(), e.getY()),
                    getHeading() + e.getAngle(),
                    false);
        };

        rotationGestureEnabledProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                addEventHandler(RotateEvent.ROTATE, rotationGestureHandler);
            } else {
                removeEventHandler(RotateEvent.ROTATE, rotationGestureHandler);
            }
        });
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return propertyFactory.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        List<CssMetaData<? extends Styleable, ?>> md = getClassCssMetaData();
        return md;
    }

    public final BooleanProperty rotationGestureEnabledProperty() {
        return rotationGestureEnabledProperty;
    }

    public final Boolean getRotationGestureEnabled() {
        return rotationGestureEnabledProperty.get();
    }

    public final void setRotationGestureEnabled(Boolean rotationGestureEnabled) {
        rotationGestureEnabledProperty.set(rotationGestureEnabled);
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
}
