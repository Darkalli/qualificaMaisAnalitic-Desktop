package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.classes.dto.PresenceDto;
import com.qualificamaisanaliticinterface.features.enuns.PresenceStatus;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.people.PersonService;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ClassInfoController {

    private final RegisterService registerService = new RegisterService();
    private final ClassService classService = new ClassService();
    private final PersonService personService = new PersonService();
    @FXML
    public DialogPane infoDialogPane;
    @FXML
    public Label courseField;
    @FXML
    public Label dateField;
    @FXML
    public Label startField;
    @FXML
    public Label finishField;
    @FXML
    public TableView<PersonDto> peopleTable;
    @FXML
    public TableColumn<PersonDto, String> namesColumn;
    @FXML
    public TableColumn<PersonDto, Void> personActionsColumn;
    @FXML
    public Label info;
    @FXML
    public Button confirm;
    @FXML
    public Button close;

    private final Map<Long, PresenceStatus> presences = new HashMap<>();
    private final Map<Long, PresenceStatus> savedPresences = new HashMap<>();

    private ClassDto classDto;
    private RegisterDto registerDto;
    private List<PersonDto> allPeople = List.of();

    private boolean savingPresences;
    private double dragOffsetX;
    private double dragOffsetY;

    @FXML
    private void initialize() {
        info.visibleProperty().bind(info.textProperty().isNotEmpty());
        info.managedProperty().bind(info.visibleProperty());
    }

    public void setClass(ClassDto classDto) {
        this.classDto = classDto;
        getPeople(classDto.course().id())
                .whenComplete((people, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        info.setText("Não foi possível carregar as pessoas.");
                        return;
                    }
                    peopleTable.getItems().setAll(people);
                    namesColumn.setCellValueFactory(cell ->
                            new ReadOnlyStringWrapper(cell.getValue().fullName())
                    );
                }));
        personActionsColumn.setCellFactory(column ->
                new ClassPresenceActionsCell(presences));
        courseField.setText(classDto.course().name());
        dateField.setText(classDto.day() == null ? "Não informada"
                : LocalDate.parse(classDto.day()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        startField.setText(classDto.start());
        finishField.setText(classDto.finish());

        confirm.setDisable(true);
        peopleTable.setDisable(true);
        classService.findAllPresences(classDto.course().id(), classDto.id())
                .whenComplete((list, failure) -> Platform.runLater(() -> {
                    if (failure != null){
                        info.setText("Não foi possivel carregar as presenças.");
                        return;
                    }
                    savedPresences.clear();
                    presences.clear();

                    list.forEach(dto -> savedPresences.put(dto.person().id(), dto.status()));

                    presences.putAll(savedPresences);
                    peopleTable.refresh();

                    peopleTable.setDisable(false);
                    confirm.setDisable(false);
                }));
    }

    public CompletableFuture<List<PersonDto>> getPeople(Long courseId) {
        return registerService.findAll().thenApply(registerDto -> registerDto.stream()
                .filter(register ->
                        register.courseOfInterest().id().equals(courseId))
                .filter(register ->
                        register.status() == StatusRegister.ACTIVE)
                .map(RegisterDto::person)
                .toList());
    }

    @FXML
    private void onSavePresencesClick() {
        if (savingPresences) {
            return;
        }
        if (presences.isEmpty()) {
            info.setText("Marque pelo menos uma presença");
            return;
        }
        Map<Long, PresenceStatus> selections = Map.copyOf(presences);
        Long courseClassId = classDto.id();
        savingPresences = true;
        info.setText("");
        infoDialogPane.getContent().setDisable(true);

        List<Long> savedIds = new ArrayList<>();

        CompletableFuture<Void> sending = CompletableFuture.completedFuture(null);

        for (var entry : selections.entrySet()) {
            Long personId = entry.getKey();
            PresenceStatus current = entry.getValue();
            PresenceStatus saved = savedPresences.get(personId);

            if (current == saved) {
                continue;
            }

            boolean alreadySaved = savedPresences.containsKey(personId);

            sending = sending.thenCompose(ignored -> {
                CompletableFuture<String> request = alreadySaved
                        ? classService.updatePresence(personId, courseClassId, current)
                        : classService.addPresence(personId, courseClassId, current);

                return request.thenAccept(response -> savedIds.add(personId));
            });
        }
        sending.whenComplete((result, failure) -> Platform.runLater(() -> {
            savingPresences = false;
            infoDialogPane.getContent().setDisable(false);

            savedIds.forEach(personId ->
                    savedPresences.put(personId, selections.get(personId))
            );
            if (failure != null){
                peopleTable.refresh();
                Throwable cause = failure;
                while (cause instanceof CompletionException && cause.getCause() != null){
                    cause = cause.getCause();
                }

                String message = cause.getMessage()
                        == null ? "Não foi possivel salvar todas as presenças." : cause.getMessage();

                info.setText("Envios confirmados: " + savedIds.size() + ". " + message
                        + " As escolhas pendentes foram mantidas");
                return;
            }
            close.getScene().getWindow().hide();
        }));
    }

    private void showLoadError(Throwable failure) {
        Throwable cause = failure;
        while (cause instanceof CompletionException
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = "Não foi possível carregar as pessoas.";

        if (cause instanceof ApiException apiException
                && apiException.getStatusCode() == 401) {
            message = "Sessão inválida ou expirada. Entre novamente.";
        }
        peopleTable.setPlaceholder(new Label(message));
        info.setText(message);
    }

    @FXML
    private void onClickCancel() {
        close.getScene().getWindow().hide();
    }

    @FXML
    private void onDragPressed(MouseEvent event) {
        Window window = infoDialogPane.getScene().getWindow();
        dragOffsetX = event.getScreenX() - window.getX();
        dragOffsetY = event.getScreenY() - window.getY();
    }

    @FXML
    private void onDragMoved(MouseEvent event) {
        Window window = infoDialogPane.getScene().getWindow();
        window.setX(event.getScreenX() - dragOffsetX);
        window.setY(event.getScreenY() - dragOffsetY);
    }
}
