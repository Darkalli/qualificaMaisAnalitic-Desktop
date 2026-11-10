package com.qualificamaisanaliticinterface.features.registers;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.qualificamaisanaliticinterface.features.registers.RegisterTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class RegisterActionsCellTest {
    @BeforeAll static void startJavaFx() { startFx(); }

    @Test
    void buttonsAppearOnlyForNonEmptyRows() throws Exception {
        fx(() -> {
            RegisterActionsCell cell = new RegisterActionsCell(() -> {});
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
