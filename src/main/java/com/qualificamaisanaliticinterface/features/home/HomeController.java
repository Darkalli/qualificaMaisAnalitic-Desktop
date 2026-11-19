package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.features.classes.ClassInfoController;
import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.courses.CoursesFormController;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import com.qualificamaisanaliticinterface.features.login.AuthService;
import com.qualificamaisanaliticinterface.features.people.PersonFormController;
import com.qualificamaisanaliticinterface.features.registers.RegisterFormController;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import com.qualificamaisanaliticinterface.navigation.WindowBarController;
import com.qualificamaisanaliticinterface.session.UserSession;
import com.qualificamaisanaliticinterface.session.SessionService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;


import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.Function;

public class HomeController {

    @FXML
    public BorderPane workspace;
    @FXML
    public FontIcon maximise;
    @FXML
    public BorderPane homePane;
    @FXML
    private ScrollPane homeContent;
    @FXML
    private Label pageTitle;
    @FXML
    private Label greeting;
    @FXML
    private Label currentDate;
    @FXML
    private Label coursesCount;
    @FXML
    private Label coursesState;
    @FXML
    private Label registersCount;
    @FXML
    private Label registersState;
    @FXML
    private Label peopleCount;
    @FXML
    private Label peopleState;
    @FXML
    private Label classesCount;
    @FXML
    private Label classesState;
    @FXML
    private Label todayClassesMessage;
    @FXML
    private VBox todayClassesList;
    @FXML
    private Label recentRegistersMessage;
    @FXML
    private VBox recentRegistersList;
    @FXML
    private Label pendingPresencesMessage;
    @FXML
    private VBox pendingPresencesList;

    private final AuthService authService;
    private final SessionService sessionService;
    private final HomeSummaryService summaryService;
    private final Clock clock;
    private long summaryRequest;

    public HomeController() {
        this(new AuthService(), new SessionService());
    }

    HomeController(AuthService authService, SessionService sessionService) {
        this(authService, sessionService, new HomeSummaryService(), Clock.systemDefaultZone());
    }

    HomeController(AuthService authService, SessionService sessionService,
                   HomeSummaryService summaryService, Clock clock) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.summaryService = summaryService;
        this.clock = clock;
    }

    private boolean logoutInProgress;

    @FXML
    private void initialize() {
        onRefreshSummary();
    }

    @FXML
    private void onHomeClick() {
        workspace.setCenter(homeContent);
        pageTitle.setText("Início");
        onRefreshSummary();
    }

    @FXML
    private void onRefreshSummary() {
        var session = UserSession.current();
        String username = session == null || session.user() == null ? null : session.user().username();
        greeting.setText(username == null || username.isBlank() ? "Olá!" : "Olá, " + username.strip() + "!");
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        currentDate.setText(today.format(DateTimeFormatter.ofPattern(
                "EEEE, d 'de' MMMM 'de' uuuu", Locale.forLanguageTag("pt-BR"))));

        long request = ++summaryRequest;
        loadCount(coursesCount, coursesState, summaryService::activeCourses, "Em andamento", request);
        loadCount(registersCount, registersState, summaryService::activeRegisters, "Inscrições com situação ativa", request);
        loadCount(peopleCount, peopleState, summaryService::registeredPeople, "Total de cadastros", request);
        loadTodaysClasses(today, request);
        loadActivityList(recentRegistersList, recentRegistersMessage, summaryService::recentRegisters,
                this::createRecentRegisterRow, "Carregando inscrições recentes...", "Nenhuma inscrição cadastrada.",
                "Não foi possível carregar as inscrições recentes. Clique em Atualizar para tentar novamente.", request);
        loadActivityList(pendingPresencesList, pendingPresencesMessage, () -> summaryService.pendingPresences(now),
                this::createPendingPresenceRow, "Verificando chamadas...", "Nenhuma chamada pendente.",
                "Não foi possível verificar as chamadas pendentes. Clique em Atualizar para tentar novamente.", request);
    }

    private <T> void loadActivityList(VBox list, Label message, Supplier<CompletableFuture<List<T>>> query,
                                    Function<T, Node> createRow, String loading, String empty, String error, long request) {
        list.getChildren().clear();
        setListMessage(message, loading);
        CompletableFuture<List<T>> result;
        try {
            result = query.get();
        } catch (RuntimeException exception) {
            result = CompletableFuture.failedFuture(exception);
        }
        result.whenComplete((items, failure) -> Platform.runLater(() -> {
            if (request != summaryRequest || logoutInProgress) {
                return;
            }
            if (failure != null) {
                setListMessage(message, error);
                return;
            }
            list.getChildren().setAll(items.stream().map(createRow).toList());
            setListMessage(message, items.isEmpty() ? empty : "");
        }));
    }

    private HBox createRecentRegisterRow(RegisterDto register) {
        Label person = new Label(register.person() == null ? "Pessoa não informada" : register.person().fullName());
        person.getStyleClass().add("home-agenda-course");
        person.setWrapText(true);
        Label course = new Label(register.courseOfInterest() == null ? "Curso não informado" : register.courseOfInterest().name());
        course.getStyleClass().add("home-agenda-time");
        course.setWrapText(true);
        VBox details = new VBox(6, person, course);
        HBox.setHgrow(details, Priority.ALWAYS);
        Label date = new Label(formatActivityDate(register.registerDate()));
        date.getStyleClass().add("home-agenda-time");
        Label status = new Label(register.status() == null ? "Não informada" : register.status().getDescription());
        status.getStyleClass().add("home-agenda-status");
        if (register.status() != null) {
            status.getStyleClass().add("home-agenda-status-" + register.status().name().toLowerCase(Locale.ROOT));
        }
        HBox row = new HBox(18, details, date, status);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("home-agenda-row", "home-register-row");
        return row;
    }

    private HBox createPendingPresenceRow(ClassDto courseClass) {
        return createClassRow(courseClass, true);
    }

    private String formatActivityDate(String value) {
        try {
            return value == null ? "Data não informada"
                    : LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException exception) {
            return "Data não informada";
        }
    }

    private void setListMessage(Label label, String message) {
        label.setText(message);
        label.setVisible(!message.isEmpty());
        label.setManaged(!message.isEmpty());
    }

    private void loadTodaysClasses(LocalDate today, long request) {
        classesCount.setText("-");
        classesState.setText("Carregando...");
        todayClassesList.getChildren().clear();
        setTodayClassesMessage("Carregando aulas de hoje...");
        CompletableFuture<List<ClassDto>> result;
        try {
            result = summaryService.findTodaysClasses(today);
        } catch (RuntimeException exception) {
            result = CompletableFuture.failedFuture(exception);
        }
        result.whenComplete((items, failure) -> Platform.runLater(() -> {
            if (request != summaryRequest || logoutInProgress) {
                return;
            }
            if (failure != null) {
                classesCount.setText("-");
                classesState.setText("Não foi possível carregar.");
                setTodayClassesMessage("Não foi possível carregar as aulas de hoje. Clique em Atualizar para tentar novamente.");
                return;
            }
            classesCount.setText(String.valueOf(items.stream()
                    .filter(item -> item.statusClass() == StatusClass.ACTIVE).count()));
            classesState.setText("Aulas ativas agendadas para hoje");
            todayClassesList.getChildren().setAll(items.stream().map(this::createTodayClassRow).toList());
            setTodayClassesMessage(items.isEmpty() ? "Nenhuma aula agendada para hoje." : "");
        }));
    }

    private HBox createTodayClassRow(ClassDto courseClass) {
        return createClassRow(courseClass, false);
    }

    private HBox createClassRow(ClassDto courseClass, boolean pending) {
        Label course = new Label(courseClass.course() == null ? "Curso não informado" : courseClass.course().name());
        course.getStyleClass().add("home-agenda-course");
        course.setWrapText(true);
        course.setMaxWidth(Double.MAX_VALUE);
        Label time = new Label(formatClassTime(courseClass.start()) + " às " + formatClassTime(courseClass.finish()));
        if (pending) {
            time.setText(formatActivityDate(courseClass.day()) + " • " + time.getText());
        }
        time.getStyleClass().add("home-agenda-time");
        VBox details = new VBox(6, course, time);
        HBox.setHgrow(details, Priority.ALWAYS);

        StatusClass statusClass = courseClass.statusClass();
        Label status = new Label(pending ? "Sem chamada"
                : statusClass == null ? "Não informada" : statusClass.getDescription());
        status.getStyleClass().add("home-agenda-status");
        if (pending) {
            status.getStyleClass().add("home-agenda-status-postponed");
        } else if (statusClass != null) {
            status.getStyleClass().add("home-agenda-status-" + statusClass.name().toLowerCase(Locale.ROOT));
        }
        Button presences = new Button("Abrir presenças", new FontIcon("fas-clipboard-check"));
        presences.getStyleClass().add("home-agenda-button");
        presences.setDisable(courseClass.id() == null || courseClass.course() == null || courseClass.course().id() == null);
        presences.setOnAction(event -> {
            try {
                openClassPresences(courseClass);
                onRefreshSummary();
            } catch (IOException exception) {
                setListMessage(pending ? pendingPresencesMessage : todayClassesMessage,
                        "Não foi possível abrir as presenças desta aula. Tente novamente.");
            }
        });
        HBox row = new HBox(18, details, status, presences);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("home-agenda-row");
        if (pending) {
            row.getStyleClass().add("home-pending-row");
        }
        return row;
    }

    private String formatClassTime(String time) {
        return time == null || time.isBlank() ? "Horário não informado"
                : LocalTime.parse(time).format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private void setTodayClassesMessage(String message) {
        setListMessage(todayClassesMessage, message);
    }

    protected void openClassPresences(ClassDto courseClass) throws IOException {
        Navigator.openForm(homePane.getScene(), "/views/pages/classes/class-info.fxml",
                (ClassInfoController controller) -> controller.setClass(courseClass));
    }

    @FXML
    private void onNewRegisterClick() throws IOException {
        Navigator.openForm(homePane.getScene(), "/views/pages/registers/register-form.fxml",
                (RegisterFormController controller) -> controller.setOnAdded(this::onRefreshSummary));
    }

    @FXML
    private void onNewPersonClick() throws IOException {
        Navigator.openForm(homePane.getScene(), "/views/pages/people/person-form.fxml",
                (PersonFormController controller) -> controller.setOnAdded(this::onRefreshSummary));
    }

    @FXML
    private void onNewCourseClick() throws IOException {
        Navigator.openForm(homePane.getScene(), "/views/pages/courses/course-form.fxml",
                (CoursesFormController controller) -> controller.setOnAdded(this::onRefreshSummary));
    }

    private void loadCount(Label count, Label state, Supplier<CompletableFuture<Long>> query,
                           String description, long request) {
        count.setText("—");
        state.setText("Carregando...");
        CompletableFuture<Long> result;
        try {
            result = query.get();
        } catch (RuntimeException exception) {
            result = CompletableFuture.failedFuture(exception);
        }
        result.whenComplete((total, failure) -> Platform.runLater(() -> {
            if (request != summaryRequest || logoutInProgress) {
                return;
            }
            count.setText(failure == null ? String.valueOf(total) : "—");
            state.setText(failure == null ? description : "Não foi possível carregar.");
        }));
    }

    @FXML
    private void onLogoutClick(ActionEvent event) throws IOException {
        if (logoutInProgress) {
            return;
        }
        Node source = (Node) event.getSource();
        Scene scene = source.getScene();
        Parent loginRoot;
        try {
            loginRoot = loadLoginScreen();
        } catch (IOException exception) {
            showLogoutWarning(
                    "Não foi possível preparar a tela de login. "
                            + "A sessão atual foi mantida."
            );
            return;
        }
        try {
            sessionService.clearSession();
        } catch (IOException exception) {
            showLogoutWarning("Não foi possível apagar a sessão salva. Tente sair novamente.");
            return;
        }
        var session = UserSession.current();
        String token = session == null ? null : session.token();
        logoutInProgress = true;
        // Bloqueia a Home enquanto o logout está em andamento.
        scene.getRoot().setDisable(true);
        authService.logout(token)
                .whenComplete((ignored, failure) ->
                        Platform.runLater(() -> {
                            UserSession.clear();
                            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource("/views/window-bar.fxml"));
                            Node header = null;
                            try {
                                header = loader.load();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            BorderPane pane = new BorderPane();
                            WindowBarController controller = loader.getController();
                            controller.setMaximise();
                            pane.setTop(header);
                            pane.setCenter(loginRoot);

                            scene.setRoot(pane);
                            Stage stage = (Stage) scene.getWindow();
                            stage.setMaximized(false);
                            stage.setWidth(380);
                            stage.setHeight(420);
                            stage.centerOnScreen();
                            logoutInProgress = false;
                            if (failure != null) {
                                try {
                                    showLogoutWarning(
                                            "Você saiu deste aplicativo, mas não foi "
                                                    + "possível confirmar a revogação "
                                                    + "da sessão no servidor."
                                    );
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        })
                );
    }

    protected Parent loadLoginScreen() throws IOException {
        return FXMLLoader.load(HomeController.class.getResource("/views/login.fxml"));
    }

    protected void showLogoutWarning(String message) throws IOException {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.initOwner(homePane.getScene().getWindow());
        alert.setTitle("Qualifica");
        FXMLLoader loader = new FXMLLoader(Navigator.class.getResource("/views/window-bar.fxml"));
        Node header = loader.load();
        WindowBarController controller = loader.getController();
        controller.setMaximise();
        controller.setMinimise();
        alert.getDialogPane().setHeader(header);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onPageClick(ActionEvent event) throws IOException {
        Button buttonClicked = (Button) event.getSource();
        String path = (String) buttonClicked.getUserData();

        if (path == null || path.isBlank()) {
            throw new IOException(
                    "Página não configurada para o botão: " + buttonClicked.getText()
            );
        }

        Navigator.changeContent(workspace, path);
        pageTitle.setText(buttonClicked.getText());
    }

    @FXML
    public void onHomeGoClick(ActionEvent mouseEvent) {
        onHomeClick();
    }

    public void onConfigClick(ActionEvent actionEvent) throws IOException {
        Navigator.openForm(homePane.getScene(), "/views/config.fxml");
    }
}
