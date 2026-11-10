package com.qualificamaisanaliticinterface.features.users;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.qualificamaisanaliticinterface.features.users.UserTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class UserActionsCellTest {
    @BeforeAll static void startJavaFx() { startFx(); }

    @Test
    void buttonsAppearOnlyForNonEmptyRows() throws Exception {
        fx(() -> {
            UserActionsCell cell = new UserActionsCell(() -> {});
            cell.updateItem(null, false);
            assertInstanceOf(HBox.class, cell.getGraphic());
            HBox buttons = (HBox) cell.getGraphic();
            assertEquals(2, buttons.getChildren().size());
            assertTrue(buttons.getChildren().stream().allMatch(Button.class::isInstance));

            cell.updateItem(null, true);
            assertNull(cell.getGraphic());
            return null;
        });
    }
}
