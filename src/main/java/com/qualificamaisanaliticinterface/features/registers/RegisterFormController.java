package com.qualificamaisanaliticinterface.features.registers;

import com.qualificamaisanaliticinterface.features.people.PersonFormController;
import com.qualificamaisanaliticinterface.features.people.PersonService;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.registers.dto.CourseDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.controlsfx.control.SearchableComboBox;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.CompletionException;

public class RegisterFormController {
    @FXML public DialogPane formDialogPane;
    @FXML public SearchableComboBox<PersonDto> personField;
    @FXML public SearchableComboBox<CourseDto> courseField;
    @FXML public DatePicker dateField;
    @FXML public Button add;
    @FXML public Button close;
    @FXML public Hyperlink newPerson;
    @FXML public Label info;

    private final PersonService personService = new PersonService();
    private final RegisterService registerService = new RegisterService();
    private Runnable onAdded;
    private double dragOffsetX;
    private double dragOffsetY;

    @FXML
    private void initialize() {
        dateField.setValue(LocalDate.now());
        personField.setConverter(new StringConverter<PersonDto>() {
            @Override
            public String toString(PersonDto person) {
                return person == null ? "" : person.fullName() + " — " + person.cpf();
            }
            @Override
            public PersonDto fromString(String value) {
                return null;
            }
        });
        personField.setOnShown(event -> {
            if (personField.lookup(".combo-box-search") instanceof TextField search) {
                search.setPromptText("Pesquisar por nome ou CPF...");
            }
        });
        courseField.setOnShown(event -> {
            if (courseField.lookup(".combo-box-search") instanceof TextField search) {
                search.setPromptText("Pesquisar curso...");
            }
        });
        loadOptions();
    }

    private void loadOptions() {
        add.setDisable(true);
        info.setText("Carregando pessoas e cursos...");
        PersonDto selectedPerson = personField.getValue();
        CourseDto selectedCourse = courseField.getValue();
        personService.findAll().thenCombine(registerService.findCourses(), (people, courses) -> {
            Platform.runLater(() -> {
                personField.getItems().setAll(people);
                courseField.getItems().setAll(courses);
                personField.setValue(selectedPerson == null ? null : people.stream()
                        .filter(person -> person.id().equals(selectedPerson.id())).findFirst().orElse(null));
                courseField.setValue(selectedCourse == null ? null : courses.stream()
                        .filter(course -> course.id().equals(selectedCourse.id())).findFirst().orElse(null));
                add.setDisable(people.isEmpty() || courses.isEmpty());
                info.setText(courses.isEmpty() ? "Cadastre um curso antes de criar uma inscrição."
                        : people.isEmpty() ? "Cadastre uma pessoa para continuar." : "");
            });
            return null;
        }).exceptionally(failure -> {
            Platform.runLater(() -> showError(failure));
            return null;
        });
    }

    @FXML
    private void onClickAddRegister() {
        PersonDto person = personField.getValue();
        CourseDto course = courseField.getValue();
        if (person == null || course == null) {
            showError(new IllegalArgumentException("Selecione a pessoa e o curso para continuar."));
            return;
        }
        LocalDate date;
        try {
            date = dateField.getConverter().fromString(dateField.getEditor().getText());
            if (date == null) {
                throw new IllegalArgumentException("Informe a data da inscrição.");
            }
        } catch (RuntimeException invalidDate) {
            showError(new IllegalArgumentException("Informe uma data de inscrição válida."));
            return;
        }
        info.setText("");
        formDialogPane.getContent().setDisable(true);
        registerService.addRegister(person.cpf(), course.id(), date.toString())
                .whenComplete((response, failure) -> Platform.runLater(() -> {
                    formDialogPane.getContent().setDisable(false);
                    if (failure != null) {
                        showError(failure);
                        return;
                    }
                    close.getScene().getWindow().hide();
                    if (onAdded != null) {
                        onAdded.run();
                    }
                }));
    }

    @FXML
    private void onNewPersonClick() throws IOException {
        Navigator.openForm(formDialogPane.getScene(), "/views/pages/people/person-form.fxml",
                (PersonFormController controller) -> controller.setOnAdded(this::loadOptions));
    }

    @FXML
    private void onClickCancel() {
        close.getScene().getWindow().hide();
    }

    private void showError(Throwable failure) {
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        info.setText(cause.getMessage() == null ? "Não foi possível criar a inscrição." : cause.getMessage());
        if (info.getScene() != null && info.getScene().getWindow() instanceof Stage stage) {
            stage.sizeToScene();
        }
    }

    public void setOnAdded(Runnable onAdded) {
        this.onAdded = onAdded;
    }

    @FXML
    private void onDragPressed(MouseEvent event) {
        Window window = formDialogPane.getScene().getWindow();
        dragOffsetX = event.getScreenX() - window.getX();
        dragOffsetY = event.getScreenY() - window.getY();
    }

    @FXML
    private void onDragMoved(MouseEvent event) {
        Window window = formDialogPane.getScene().getWindow();
        window.setX(event.getScreenX() - dragOffsetX);
        window.setY(event.getScreenY() - dragOffsetY);
    }
}
