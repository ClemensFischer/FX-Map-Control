/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.text.Text;

/**
 * A MapNode with a Text element.
 */
public class MapText extends MapNode {
    private final Text text = new Text();

    public MapText() {
        getStyleClass().add("map-text");
        getChildren().add(text);
    }

    public final StringProperty textProperty() {
        return text.textProperty();
    }

    public final String getText() {
        return text.getText();
    }

    public final void setText(String text) {
        this.text.setText(text);
    }

    public final DoubleProperty xProperty() {
        return text.xProperty();
    }

    public final double getX() {
        return text.getX();
    }

    public final void setX(double x) {
        text.setX(x);
    }

    public final DoubleProperty yProperty() {
        return text.yProperty();
    }

    public final double getY() {
        return text.getY();
    }

    public final void setY(double y) {
        text.setY(y);
    }
}
