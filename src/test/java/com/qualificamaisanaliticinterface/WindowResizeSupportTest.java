package com.qualificamaisanaliticinterface;

import com.qualificamaisanaliticinterface.navigation.WindowResizeSupport;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.qualificamaisanaliticinterface.AuthTestSupport.fx;
import static com.qualificamaisanaliticinterface.AuthTestSupport.startFx;

class WindowResizeSupportTest {
    private Stage stage;
    private Scene scene;

    @BeforeAll
    static void startJavaFx() {
        startFx();
    }

    @BeforeEach
    void setUp() throws Exception {
        fx(() -> {
            Pane root = new Pane();
            root.setMinSize(300, 200);
            scene = new Scene(root, 600, 400);
            stage = new Stage(StageStyle.TRANSPARENT);
            stage.setScene(scene);
            stage.setX(100);
            stage.setY(100);
            WindowResizeSupport.install(stage, scene);
            stage.show();
            return null;
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        fx(() -> {
            if (stage != null) stage.close();
            return null;
        });
    }

    @Test
    void shouldShowResizeCursorsAtEdgesAndCorners() throws Exception {
        fx(() -> {
            double width = scene.getWidth();
            double height = scene.getHeight();
            double[][] positions = {{2, 2}, {width - 2, 2}, {2, height - 2},
                    {width - 2, height - 2}, {2, 100}, {width - 2, 100},
                    {100, 2}, {100, height - 2}};
            Cursor[] cursors = {Cursor.NW_RESIZE, Cursor.NE_RESIZE, Cursor.SW_RESIZE,
                    Cursor.SE_RESIZE, Cursor.W_RESIZE, Cursor.E_RESIZE,
                    Cursor.N_RESIZE, Cursor.S_RESIZE};
            for (int i = 0; i < positions.length; i++) {
                fire(MouseEvent.MOUSE_MOVED, positions[i][0], positions[i][1], 0, 0);
                Assertions.assertSame(cursors[i], scene.getCursor());
            }
            fire(MouseEvent.MOUSE_MOVED, 100, 100, 0, 0);
            Assertions.assertNull(scene.getCursor());
            return null;
        });
    }

    @Test
    void shouldResizeFromBottomRightWithoutMovingWindow() throws Exception {
        fx(() -> {
            double width = stage.getWidth();
            double height = stage.getHeight();
            drag(scene.getWidth() - 2, scene.getHeight() - 2, 80, 60);
            Assertions.assertEquals(width + 80, stage.getWidth(), 1);
            Assertions.assertEquals(height + 60, stage.getHeight(), 1);
            Assertions.assertEquals(100, stage.getX(), 1);
            Assertions.assertEquals(100, stage.getY(), 1);
            return null;
        });
    }

    @Test
    void shouldKeepOppositeCornerFixedWhenResizingFromTopLeft() throws Exception {
        fx(() -> {
            double right = stage.getX() + stage.getWidth();
            double bottom = stage.getY() + stage.getHeight();
            drag(2, 2, 80, 60);
            Assertions.assertEquals(180, stage.getX(), 1);
            Assertions.assertEquals(160, stage.getY(), 1);
            Assertions.assertEquals(right, stage.getX() + stage.getWidth(), 1);
            Assertions.assertEquals(bottom, stage.getY() + stage.getHeight(), 1);
            return null;
        });
    }

    @Test
    void shouldRespectMinimumAndMaximumWindowSize() throws Exception {
        fx(() -> {
            stage.setMinWidth(400);
            stage.setMinHeight(250);
            stage.setMaxWidth(700);
            stage.setMaxHeight(500);
            drag(2, 2, 1000, 1000);
            Assertions.assertEquals(400, stage.getWidth(), 1);
            Assertions.assertEquals(250, stage.getHeight(), 1);
            drag(2, 2, -1000, -1000);
            Assertions.assertEquals(700, stage.getWidth(), 1);
            Assertions.assertEquals(500, stage.getHeight(), 1);
            return null;
        });
    }

    @Test
    void shouldLeaveContentDraggingAndFixedWindowsAlone() throws Exception {
        fx(() -> {
            double width = stage.getWidth();
            double height = stage.getHeight();
            drag(100, 100, 80, 60);
            stage.setResizable(false);
            drag(2, 2, 80, 60);
            Assertions.assertEquals(width, stage.getWidth(), 1);
            Assertions.assertEquals(height, stage.getHeight(), 1);
            Assertions.assertEquals(100, stage.getX(), 1);
            Assertions.assertEquals(100, stage.getY(), 1);
            return null;
        });
    }

    private void drag(double x, double y, double deltaX, double deltaY) {
        double screenX = stage.getX() + x;
        double screenY = stage.getY() + y;
        fire(MouseEvent.MOUSE_PRESSED, x, y, screenX, screenY);
        fire(MouseEvent.MOUSE_DRAGGED, x + deltaX, y + deltaY,
                screenX + deltaX, screenY + deltaY);
        fire(MouseEvent.MOUSE_RELEASED, x + deltaX, y + deltaY,
                screenX + deltaX, screenY + deltaY);
    }

    private void fire(EventType<MouseEvent> type, double x, double y,
                      double screenX, double screenY) {
        Event.fireEvent(scene, new MouseEvent(type, x, y, screenX, screenY,
                MouseButton.PRIMARY, 1, false, false, false, false,
                type != MouseEvent.MOUSE_RELEASED, false, false, false, false, true, null));
    }
}
