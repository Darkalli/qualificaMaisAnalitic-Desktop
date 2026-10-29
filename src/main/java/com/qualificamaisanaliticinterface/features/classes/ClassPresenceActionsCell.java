package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.features.enuns.PresenceStatus;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

public class ClassPresenceActionsCell extends TableCell<PersonDto, Void> {
    @FXML
    private RadioButton present;
    @FXML
    private RadioButton absent;
    @FXML
    private RadioButton justified;
    @FXML
    private ToggleGroup presenceGroup;
    @FXML
    private final HBox buttons;

    private final Map<Long, PresenceStatus> presences;
    private boolean restoringSelection;

    @FXML
    private void initialize() {
        for (RadioButton radio : new RadioButton[]{present, absent, justified}) {
            radio.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && radio.isSelected()) {
                    presenceGroup.selectToggle(null);
                    event.consume();
                }
            });
        }

        present.setUserData(PresenceStatus.PRESENT);
        absent.setUserData(PresenceStatus.ABSENT);
        justified.setUserData(PresenceStatus.JUSTIFIED);
        presenceGroup.selectedToggleProperty().addListener((obs, last, selected) -> {
            if (restoringSelection) {
                return;
            }
            TableRow<PersonDto> line = getTableRow();
            if (line == null || line.getItem() == null) {
                return;
            }

            Long personId = line.getItem().id();

            if (selected == null) {
                presences.remove(personId);
            } else {
                PresenceStatus status = (PresenceStatus) selected.getUserData();
                presences.put(personId, status);
            }
        });
    }

    private void restoreSelection(boolean empty){
        restoringSelection = true;
        try{
            if (empty || getTableView() == null
                      || getIndex() < 0
                      || getIndex() >= getTableView().getItems().size()){
                presenceGroup.selectToggle(null);
                return;
            }
            PersonDto person = getTableView().getItems().get(getIndex());
            PresenceStatus status = presences.get(person.id());

            if (status == null){
                presenceGroup.selectToggle(null);
            }else {
                presenceGroup.selectToggle(switch (status){
                    case PRESENT -> present;
                    case ABSENT -> absent;
                    case JUSTIFIED -> justified;
                });
            }
        }finally {
            restoringSelection = false;
        }
    }

    public ClassPresenceActionsCell(Map<Long, PresenceStatus> presences) {
        this.presences = presences;
        FXMLLoader loader = new FXMLLoader(com.qualificamaisanaliticinterface.features.classes.ClassesActionsCell.class.getResource("/views/pages/classes/class-presence-actions.fxml"));
        try {
            loader.setController(this);
            buttons = loader.load();
        } catch (IOException exception) {
            throw new UncheckedIOException("Não foi possível carregar os botões da aula.", exception);
        }
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        restoreSelection(empty);
        setGraphic(empty ? null : buttons);
    }
}
