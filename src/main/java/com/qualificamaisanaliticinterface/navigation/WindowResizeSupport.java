package com.qualificamaisanaliticinterface.navigation;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public final class WindowResizeSupport {
    private static final double BORDER_SIZE = 6;

    private final Stage stage;
    private final Scene scene;
    private Cursor direction;
    private double startMouseX;
    private double startMouseY;
    private double startX;
    private double startY;
    private double startWidth;
    private double startHeight;

    private WindowResizeSupport(Stage stage, Scene scene) {
        this.stage = stage;
        this.scene = scene;
    }

    public static void install(Stage stage, Scene scene) {
        WindowResizeSupport support = new WindowResizeSupport(stage, scene);
        scene.addEventFilter(MouseEvent.MOUSE_MOVED,
                event -> scene.setCursor(support.directionAt(event)));
        scene.addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
            if (event.getTarget() == scene && support.direction == null) {
                scene.setCursor(null);
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, support::startResize);
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, support::resize);
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (support.direction != null) {
                support.direction = null;
                scene.setCursor(support.directionAt(event));
                event.consume();
            }
        });
        stage.maximizedProperty().addListener((observable, previous, maximized) -> {
            if (maximized) {
                support.direction = null;
                scene.setCursor(null);
            }
        });
    }

    private boolean canResize() {
        return stage.isResizable() && !stage.isMaximized() && !stage.isFullScreen();
    }

    private Cursor directionAt(MouseEvent event) {
        if (!canResize()) {
            return null;
        }
        boolean left = event.getSceneX() <= BORDER_SIZE;
        boolean right = event.getSceneX() >= scene.getWidth() - BORDER_SIZE;
        boolean top = event.getSceneY() <= BORDER_SIZE;
        boolean bottom = event.getSceneY() >= scene.getHeight() - BORDER_SIZE;

        if (top && left) return Cursor.NW_RESIZE;
        if (top && right) return Cursor.NE_RESIZE;
        if (bottom && left) return Cursor.SW_RESIZE;
        if (bottom && right) return Cursor.SE_RESIZE;
        if (left) return Cursor.W_RESIZE;
        if (right) return Cursor.E_RESIZE;
        if (top) return Cursor.N_RESIZE;
        if (bottom) return Cursor.S_RESIZE;
        return null;
    }

    private void startResize(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        direction = directionAt(event);
        if (direction == null) {
            return;
        }
        startMouseX = event.getScreenX();
        startMouseY = event.getScreenY();
        startX = stage.getX();
        startY = stage.getY();
        startWidth = stage.getWidth();
        startHeight = stage.getHeight();
        scene.setCursor(direction);
        event.consume();
    }

    private void resize(MouseEvent event) {
        if (direction == null || !canResize()) {
            return;
        }
        boolean left = direction == Cursor.W_RESIZE || direction == Cursor.NW_RESIZE
                || direction == Cursor.SW_RESIZE;
        boolean right = direction == Cursor.E_RESIZE || direction == Cursor.NE_RESIZE
                || direction == Cursor.SE_RESIZE;
        boolean top = direction == Cursor.N_RESIZE || direction == Cursor.NW_RESIZE
                || direction == Cursor.NE_RESIZE;
        boolean bottom = direction == Cursor.S_RESIZE || direction == Cursor.SW_RESIZE
                || direction == Cursor.SE_RESIZE;

        if (left || right) {
            double delta = event.getScreenX() - startMouseX;
            double minimum = Math.max(stage.getMinWidth(), scene.getRoot().minWidth(-1));
            double width = Math.clamp(startWidth + (left ? -delta : delta),
                    Math.max(1, minimum), Math.max(Math.max(1, minimum), stage.getMaxWidth()));
            stage.setWidth(width);
            if (left) stage.setX(startX + startWidth - width);
        }
        if (top || bottom) {
            double delta = event.getScreenY() - startMouseY;
            double minimum = Math.max(stage.getMinHeight(), scene.getRoot().minHeight(-1));
            double height = Math.clamp(startHeight + (top ? -delta : delta),
                    Math.max(1, minimum), Math.max(Math.max(1, minimum), stage.getMaxHeight()));
            stage.setHeight(height);
            if (top) stage.setY(startY + startHeight - height);
        }
        event.consume();
    }
}
