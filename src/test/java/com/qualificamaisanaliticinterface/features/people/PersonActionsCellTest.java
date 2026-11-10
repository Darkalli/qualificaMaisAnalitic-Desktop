package com.qualificamaisanaliticinterface.features.people;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.qualificamaisanaliticinterface.features.people.PersonTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class PersonActionsCellTest {
    @BeforeAll static void startJavaFx() { startFx(); }

    @Test
    void buttonsAppearOnlyForNonEmptyRows() throws Exception {
        fx(() -> {
            PersonActionsCell cell = new PersonActionsCell(() -> {});
            cell.updateItem(null, false);
            assertInstanceOf(HBox.class, cell.getGraphic());
            HBox buttons = (HBox) cell.getGraphic();
            assertEquals(3, buttons.getChildren().size());
            assertTrue(buttons.getChildren().stream().allMatch(Button.class::isInstance));

            cell.updateItem(null, true);
            assertNull(cell.getGraphic());
            return null;
        });
    }
}
