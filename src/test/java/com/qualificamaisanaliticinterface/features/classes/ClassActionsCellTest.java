package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.features.enuns.PresenceStatus;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qualificamaisanaliticinterface.features.classes.ClassTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class ClassActionsCellTest {
    @BeforeAll
    static void startJavaFx() {
        startFx();
    }

    @Test
    void shouldRestorePresenceSelectionWhenCellIsReused() throws Exception {
        PersonDto first = person(1L, "Ana");
        PersonDto second = person(2L, "Bia");
        PersonDto third = person(3L, "Caio");
        PersonDto withoutPresence = person(4L, "Davi");
        Map<Long, PresenceStatus> selections = new HashMap<>(Map.of(
                1L, PresenceStatus.PRESENT,
                2L, PresenceStatus.ABSENT,
                3L, PresenceStatus.JUSTIFIED));

        record Fixture(Stage stage, TableView<PersonDto> table) {}
        Fixture fixture = fx(() -> {
            TableView<PersonDto> table = new TableView<>();
            TableColumn<PersonDto, Void> column = new TableColumn<>("Presença");
            column.setPrefWidth(220);
            column.setCellFactory(ignored -> new ClassPresenceActionsCell(selections));
            table.getColumns().add(column);
            table.getItems().add(first);
            Stage window = new Stage();
            window.setScene(new Scene(table, 240, 150));
            window.show();
            return new Fixture(window, table);
        });

        try {
            fx(() -> {
                TableView<PersonDto> table = fixture.table();
                table.applyCss();
                table.layout();
                ClassPresenceActionsCell cell = table.lookupAll(".table-cell").stream()
                        .filter(ClassPresenceActionsCell.class::isInstance)
                        .map(ClassPresenceActionsCell.class::cast)
                        .filter(candidate -> candidate.getIndex() == 0)
                        .findFirst().orElseThrow();

                assertSelection(cell, PresenceStatus.PRESENT);
                table.getItems().set(0, second);
                cell.updateItem(null, false);
                assertSelection(cell, PresenceStatus.ABSENT);
                table.getItems().set(0, third);
                cell.updateItem(null, false);
                assertSelection(cell, PresenceStatus.JUSTIFIED);
                table.getItems().set(0, withoutPresence);
                cell.updateItem(null, false);
                assertSelection(cell, null);
                assertEquals(Map.of(1L, PresenceStatus.PRESENT, 2L, PresenceStatus.ABSENT,
                        3L, PresenceStatus.JUSTIFIED), selections);
                return null;
            });
        } finally {
            fx(() -> {
                fixture.stage().close();
                return null;
            });
        }
    }

    @Test
    void shouldHideActionButtonsForEmptyRows() throws Exception {
        fx(() -> {
            ClassesActionsCell classCell = new ClassesActionsCell(() -> {});
            classCell.updateItem(null, false);
            assertInstanceOf(HBox.class, classCell.getGraphic());
            assertEquals(3, ((HBox) classCell.getGraphic()).getChildren().size());
            assertTrue(((HBox) classCell.getGraphic()).getChildren().stream().allMatch(Button.class::isInstance));
            classCell.updateItem(null, true);
            assertNull(classCell.getGraphic());

            ClassPresenceActionsCell presenceCell = new ClassPresenceActionsCell(new HashMap<>());
            presenceCell.updateItem(null, false);
            assertInstanceOf(HBox.class, presenceCell.getGraphic());
            assertEquals(3, presenceCell.getGraphic().lookupAll(".radio-button").size());
            presenceCell.updateItem(null, true);
            assertNull(presenceCell.getGraphic());
            return null;
        });
    }

    private static void assertSelection(ClassPresenceActionsCell cell, PresenceStatus expected) {
        for (String id : List.of("present", "absent", "justified")) {
            RadioButton radio = (RadioButton) cell.getGraphic().lookup("#" + id);
            assertNotNull(radio);
            assertEquals(expected == radio.getUserData(), radio.isSelected(), id);
        }
    }
}
