/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

import javafx.animation.FadeTransition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

/**
 * Provides the ImageView that display a map tile image.
 */
public class Tile {
    private static Duration fadeDuration;

    public static Duration getFadeDuration() {
        return fadeDuration;
    }

    public static void setFadeDuration(Duration duration) {
        fadeDuration = duration;
    }

    private final int zoomLevel;
    private final int x;
    private final int y;
    private final ImageView imageView;
    private boolean pending;

    public Tile(int zoomLevel, int x, int y) {
        this.zoomLevel = zoomLevel;
        this.x = x;
        this.y = y;
        imageView = new ImageView();
        imageView.setOpacity(0d);
        pending = true;
    }

    public final int getZoomLevel() {
        return zoomLevel;
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final int getXIndex() {
        int numTiles = 1 << zoomLevel;
        return ((x % numTiles) + numTiles) % numTiles;
    }

    public final boolean isPending() {
        return pending;
    }

    public final ImageView getImageView() {
        return imageView;
    }

    public final Image getImage() {
        return imageView.getImage();
    }

    public final void setImage(Image image, boolean fade) {
        pending = false;

        if (image != null) {
            imageView.setImage(image);
            if (fade && fadeDuration != null && fadeDuration.greaterThan(Duration.ZERO)) {
                FadeTransition fadeTransition = new FadeTransition(fadeDuration, imageView);
                fadeTransition.setToValue(1d);
                fadeTransition.play();
            } else {
                imageView.setOpacity(1d);
            }
        }
    }
}
