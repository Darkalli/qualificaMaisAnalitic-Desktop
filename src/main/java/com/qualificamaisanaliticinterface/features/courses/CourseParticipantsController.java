package com.qualificamaisanaliticinterface.features.courses;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.people.PersonInfoController;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.text.Collator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CourseParticipantsController {
    @FXML
    private DialogPane participantsPane;
    @FXML
    private Label courseName;
    @FXML
    private Label enrolledHeading;
    @FXML
    private Label waitingHeading;
    @FXML
    private TableView<RegisterDto> enrolled;
    @FXML
    private TableView<RegisterDto> waiting;
    @FXML
    private TableColumn<RegisterDto, String> enrolledName;
    @FXML
    private TableColumn<RegisterDto, String> waitingName;
    @FXML
    private Button enrolledPersonButton;
    @FXML
    private Button waitingPersonButton;

    @FXML
    private void initialize() {
        configureTable(enrolled, enrolledName, enrolledPersonButton);
        configureTable(waiting, waitingName, waitingPersonButton);
    }

    private void configureTable(TableView<RegisterDto> table, TableColumn<RegisterDto, String> nameColumn,
                                Button personButton) {
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(personName(cell.getValue())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Carregando inscrições…"));
        personButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty()
                .map(registration -> registration.person() == null)
                .orElse(true));
    }

    public void setCourse(CourseDto course) {
        courseName.setText(course.toString());
        new RegisterService().findAll().whenComplete((registrations, failure) -> Platform.runLater(() -> {
            if (failure != null) {
                enrolled.setPlaceholder(new Label("Falha ao carregar os inscritos."));
                waiting.setPlaceholder(new Label("Falha ao carregar a lista de espera."));
                return;
            }
            showParticipants(course.id(), registrations);
        }));
    }

    private void showParticipants(Long courseId, List<RegisterDto> registrations) {
        List<RegisterDto> courseRegistrations = registrations.stream()
                .filter(registration -> registration.courseOfInterest() != null
                        && courseId.equals(registration.courseOfInterest().id()))
                .toList();

        Collator nameOrder = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        enrolled.getItems().setAll(courseRegistrations.stream()
                .filter(registration -> registration.status() == StatusRegister.ACTIVE)
                .sorted(Comparator.comparing(this::personName, nameOrder))
                .toList());
        waiting.getItems().setAll(courseRegistrations.stream()
                .filter(registration -> registration.status() == StatusRegister.WAITLISTED)
                .sorted(Comparator.comparing(this::registrationDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RegisterDto::id, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList());

        enrolledHeading.setText("Inscritos (" + enrolled.getItems().size() + ")");
        waitingHeading.setText("Lista de espera (" + waiting.getItems().size() + ")");
        enrolled.setPlaceholder(new Label("Nenhuma inscrição ativa."));
        waiting.setPlaceholder(new Label("Ninguém na lista de espera."));
    }

    private String personName(RegisterDto registration) {
        PersonDto person = registration.person();
        return person == null || person.fullName() == null || person.fullName().isBlank()
                ? "Nome não informado" : person.fullName();
    }

    private LocalDateTime registrationDate(RegisterDto registration) {
        String value = registration.registerDate();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return value.contains("T") ? LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME)
                    : LocalDate.parse(value).atStartOfDay();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    @FXML
    private void onEnrolledPersonClick() throws IOException {
        openPerson(enrolled.getSelectionModel().getSelectedItem().person());
    }

    @FXML
    private void onWaitingPersonClick() throws IOException {
        openPerson(waiting.getSelectionModel().getSelectedItem().person());
    }

    private void openPerson(PersonDto person) throws IOException {
        Navigator.openForm(participantsPane.getScene(), "/views/pages/people/person-info.fxml",
                (PersonInfoController controller) -> controller.getPersonInfo(person));
    }

    @FXML
    private void onCloseClick() {
        participantsPane.getScene().getWindow().hide();
    }
}
