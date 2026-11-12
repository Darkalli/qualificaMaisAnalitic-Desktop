package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.control.SearchableComboBox;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ClassBatchFormController {

    private final RegisterService registerService;
    private final ClassService classService;

    public ClassBatchFormController() {
        this(new RegisterService(), new ClassService());
    }

    ClassBatchFormController(RegisterService registerService, ClassService classService) {
        this.registerService = registerService;
        this.classService = classService;
    }

    @FXML
    public DialogPane formDialogPane;
    @FXML
    public SearchableComboBox<CourseDto> courseField;
    @FXML
    public DatePicker startDateField;
    @FXML
    public DatePicker finishDateField;
    @FXML
    public TextField startField;
    @FXML
    public TextField finishField;
    @FXML
    public Label info;
    @FXML
    public Button close;
    @FXML
    public Button add;
    @FXML
    public RadioButton sunday;
    @FXML
    public RadioButton monday;
    @FXML
    public RadioButton tuesday;
    @FXML
    public RadioButton wednesday;
    @FXML
    public RadioButton thursday;
    @FXML
    public RadioButton friday;
    @FXML
    public RadioButton saturday;


    private Runnable onAdded;
    private double dragOffsetX;
    private double dragOffsetY;

    @FXML
    private void initialize() {
        startDateField.setValue(LocalDate.now());
        finishDateField.setValue(LocalDate.now().plusDays(1));

        startField.setTextFormatter(new TextFormatter<String>(change -> {
            String text = change.getControlNewText();
            if (!text.matches("[0-9]{0,2}(:[0-9]{0,2})?")) {
                return null;
            }
            if (change.isAdded() && text.matches("[0-9]{2}")) {
                change.setRange(0, change.getControlText().length());
                change.setText(text + ":");
                change.selectRange(3, 3);
            }
            return change;
        }));
        finishField.setTextFormatter(new TextFormatter<String>(change -> {
            String text = change.getControlNewText();
            if (!text.matches("[0-9]{0,2}(:[0-9]{0,2})?")) {
                return null;
            }
            if (change.isAdded() && text.matches("[0-9]{2}")) {
                change.setRange(0, change.getControlText().length());
                change.setText(text + ":");
                change.selectRange(3, 3);
            }
            return change;
        }));

        courseField.setOnShown(event -> {
            if (courseField.lookup(".combo-box-search") instanceof TextField search) {
                search.setPromptText("Pesquisar curso...");
            }
        });
        loadOptions();
    }

    private void loadOptions() {
        add.setDisable(true);
        info.setText("Carregando cursos...");
        CourseDto selectedCourse = courseField.getValue();
        registerService.findCourses().thenApply( courses -> {
            Platform.runLater(() -> {
                courseField.getItems().setAll(courses);
                courseField.setValue(selectedCourse == null ? null : courses.stream()
                        .filter(course -> course.id().equals(selectedCourse.id())).findFirst().orElse(null));
                add.setDisable(courses.isEmpty());
                info.setText(courses.isEmpty() ? "Cadastre um curso antes de criar uma inscrição." : "");
            });
            return null;
        }).exceptionally(failure -> {
            Platform.runLater(() -> showError(failure));
            return null;
        });
    }

    @FXML
    private void onClickAddClassInBatch() {
        LocalDate startDate;
        LocalDate finishDate;
        CourseDto course = courseField.getValue();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);


        if (course == null){
            showError(new IllegalArgumentException("Informe o curso"));
        }

        try {
            startDate = startDateField.getConverter().fromString(startDateField.getEditor().getText());
            finishDate = finishDateField.getConverter().fromString(finishDateField.getEditor().getText());
            if (startDate == null) {
                throw new IllegalArgumentException("Informe o dia de começo das aulas.");
            } else if (finishDate == null){
                throw new IllegalArgumentException("Informe o dia de final das aulas.");
            }
        } catch (RuntimeException invalidDate) {
            showError(new IllegalArgumentException("Informe uma data válida."));
            return;
        }

        LocalTime start = LocalTime.parse(startField.getText(), formatter);
        LocalTime finish = LocalTime.parse(finishField.getText(), formatter);
        Period period;
        LocalTime morning = LocalTime.of(12, 0),
                afternoon = LocalTime.of(18, 0);
        if (start.isBefore(morning)) {
            period = Period.MORNING;
        } else if (start.isBefore(afternoon)) {
            period = Period.AFTERNOON;
        } else {
            period = Period.NIGHT;
        }

        Set<DayOfWeek> includedDays = new HashSet<>();

        if(sunday.isSelected()){
            includedDays.add(DayOfWeek.SUNDAY);
        }
        if(monday.isSelected()){
            includedDays.add(DayOfWeek.MONDAY);
        }
        if(tuesday.isSelected()){
            includedDays.add(DayOfWeek.TUESDAY);
        }
        if(wednesday.isSelected()){
            includedDays.add(DayOfWeek.WEDNESDAY);
        }
        if(thursday.isSelected()){
            includedDays.add(DayOfWeek.THURSDAY);
        }
        if(friday.isSelected()){
            includedDays.add(DayOfWeek.FRIDAY);
        }
        if(saturday.isSelected()){
            includedDays.add(DayOfWeek.SATURDAY);
        }
        List<LocalDate> days = startDate.datesUntil(finishDate.plusDays(1)).toList().stream()
                .filter(date -> includedDays.contains(date.getDayOfWeek())).toList();

        info.setText("");
        formDialogPane.getContent().setDisable(true);
        List<CompletableFuture<String>> requests = new ArrayList<>();


        for(LocalDate date : days) {
            try {
                assert course != null;
                CompletableFuture<String> request = classService.addClass(date.toString(), period, start.toString(), finish.toString(), StatusClass.ACTIVE, course.id());
                requests.add(request);
            } catch (DateTimeParseException e) {
                showError(new IllegalArgumentException("Informe uma hora válida."));
            }
        }

        CompletableFuture<Void> batch = CompletableFuture.allOf(requests.toArray(new CompletableFuture<?>[0]))
                .whenComplete((response, failure) -> Platform.runLater(() -> {
                    formDialogPane.getContent().setDisable(false);
                    if (failure != null) {
                        showError(failure);
                    }
                    close.getScene().getWindow().hide();
                    if (onAdded != null) {
                        onAdded.run();
                    }
                }));
    }

    public void onClickAddClassPage(ActionEvent actionEvent) {
        Stage currentWindow = (Stage) formDialogPane.getScene().getWindow();
        Scene ownerScene = currentWindow.getOwner().getScene();
        currentWindow.close();
        Platform.runLater(() -> {
            try {
                Navigator.openForm(ownerScene, "/views/pages/classes/class-form.fxml",
                        (ClassFormController controller) -> controller.setOnAdded(this::loadOptions));
            } catch (IOException e) {
                throw new CompletionException("Não foi possível carregar a pagina de aulas!", e);
            }
        });
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
