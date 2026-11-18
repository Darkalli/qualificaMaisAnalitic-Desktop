package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.features.login.AuthService;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import com.qualificamaisanaliticinterface.navigation.WindowBarController;
import com.qualificamaisanaliticinterface.session.UserSession;
import com.qualificamaisanaliticinterface.session.SessionService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;


import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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

    private final AuthService authService;
    private final SessionService sessionService;
    private final HomeSummaryService summaryService;
    private final Clock clock;
    private long summaryRequest;
    private double dragOffsetX;
    private double dragOffsetY;

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
        homeContent.addEventFilter(MouseEvent.MOUSE_PRESSED, this::onDragPressed);
        homeContent.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onDragMoved);
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
        LocalDate today = LocalDate.now(clock);
        currentDate.setText(today.format(DateTimeFormatter.ofPattern(
                "EEEE, d 'de' MMMM 'de' uuuu", Locale.forLanguageTag("pt-BR"))));

        long request = ++summaryRequest;
        loadCount(coursesCount, coursesState, summaryService::activeCourses, "Em andamento", request);
        loadCount(registersCount, registersState, summaryService::activeRegisters, "Inscrições com situação ativa", request);
        loadCount(peopleCount, peopleState, summaryService::registeredPeople, "Total de cadastros", request);
        loadCount(classesCount, classesState, () -> summaryService.todaysClasses(today), "Aulas ativas agendadas para hoje", request);
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

    @FXML
    private void onDragPressed(MouseEvent event) {
        Window window = homePane.getScene().getWindow();
        dragOffsetX = event.getScreenX() - window.getX();
        dragOffsetY = event.getScreenY() - window.getY();
    }

    @FXML
    private void onDragMoved(MouseEvent event) {
        Window window = homePane.getScene().getWindow();
        window.setX(event.getScreenX() - dragOffsetX);
        window.setY(event.getScreenY() - dragOffsetY);
    }
}
