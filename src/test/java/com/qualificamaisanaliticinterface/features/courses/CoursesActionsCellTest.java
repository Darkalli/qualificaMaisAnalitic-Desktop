package com.qualificamaisanaliticinterface.features.courses;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CoursesActionsCellTest {
    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException alreadyStarted) {
            // JavaFX may already be running in another test class.
        }
    }

    @Test
    void buttonsAppearOnlyForNonEmptyRows() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            CoursesActionsCell cell = new CoursesActionsCell(() -> {});
            cell.updateItem(null, false);
            assertInstanceOf(HBox.class, cell.getGraphic());
            HBox buttons = (HBox) cell.getGraphic();
            assertEquals(3, buttons.getChildren().size());
            assertTrue(buttons.getChildren().stream().allMatch(Button.class::isInstance));

            cell.updateItem(null, true);
            assertNull(cell.getGraphic());
            return null;
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }
}
