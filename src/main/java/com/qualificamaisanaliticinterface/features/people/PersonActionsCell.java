package com.qualificamaisanaliticinterface.features.people;

import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.io.UncheckedIOException;

public class PersonActionsCell extends TableCell<PersonDto, Void> {

    private final HBox buttons;
    private final Runnable onPeopleChanged;

    public PersonActionsCell(Runnable onPeopleChanged) {
        this.onPeopleChanged = onPeopleChanged;
        FXMLLoader loader = new FXMLLoader(PersonActionsCell.class.getResource("/views/pages/people/person-actions.fxml"));
        try {
            loader.setController(this);
            buttons = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível carregar os botões de ação de pessoas", e);
        }
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            setGraphic(buttons);
        }
    }

    @FXML
    public void onPersonInfoClick(ActionEvent actionEvent) throws IOException {
        PersonDto person = getTableRow().getItem();
        Navigator.openForm(getScene(), "/views/pages/people/person-info.fxml", (PersonInfoController controller) -> controller.getPersonInfo(person));
    }

    @FXML
    public void onUpdatePersonClick(ActionEvent actionEvent) throws IOException {
        PersonDto person = getTableRow().getItem();
        Navigator.openForm(getScene(), "/views/pages/people/person-update.fxml",
                (PersonUpdateController controller) -> {
                    controller.setPerson(person);
                    controller.setOnUpdated(onPeopleChanged);
                });
    }

    @FXML
    public void onDeletePersonClick(ActionEvent actionEvent) throws IOException {
        PersonDto person = getTableRow().getItem();

        Navigator.openForm(getScene(), "/views/pages/people/person-delete.fxml",
                (PersonDeleteController controller) -> {
                    controller.getDelectInfo(person);
                    controller.setOnDeleted(onPeopleChanged);
                });
    }
}
