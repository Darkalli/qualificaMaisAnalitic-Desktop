package com.qualificamaisanaliticinterface.features.registers;

import com.google.gson.Gson;
import com.qualificamaisanaliticinterface.api.ApiClient;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.registers.dto.AddRegisterRequest;
import com.qualificamaisanaliticinterface.features.registers.dto.SearchRegisterRequest;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;

import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorInput;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class RegisterService {
    private final ApiClient apiClient;
    private final Gson gson = new Gson();

    public RegisterService() {
        this(new ApiClient());
    }

    RegisterService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public CompletableFuture<List<RegisterDto>> findAll() {
        return apiClient.get("/api/register").thenApply(json -> {
            RegisterDto[] registers = gson.fromJson(json, RegisterDto[].class);
            if (registers == null) {
                throw new IllegalStateException("A API retornou uma lista de inscrições inválida.");
            }
            return List.copyOf(Arrays.asList(registers));
        });
    }

    public CompletableFuture<String> addRegister(String personCpf, Long courseId, String registerDate) {
        AddRegisterRequest request = new AddRegisterRequest(
                personCpf, courseId, registerDate);
        return apiClient.post("/api/register", gson.toJson(request));
    }

    public CompletableFuture<String> cancelRegister(RegisterDto register) {
        SearchRegisterRequest request = new SearchRegisterRequest(
                register.person().cpf(), register.courseOfInterest().id());
        return apiClient.delete("/api/register", gson.toJson(request));
    }

    public CompletableFuture<String> reactiveRegister(RegisterDto register) {
        SearchRegisterRequest request = new SearchRegisterRequest(
                register.person().cpf(), register.courseOfInterest().id());
        return apiClient.patch("/api/register/active", gson.toJson(request));
    }

    public CompletableFuture<List<CourseDto>> findCourses() {
        return apiClient.get("/api/course").thenApply(json -> {
            CourseDto[] courses = gson.fromJson(json, CourseDto[].class);
            if (courses == null) {
                throw new IllegalStateException("A API retornou uma lista de cursos inválida.");
            }
            return List.copyOf(Arrays.asList(courses));
        });
    }

    public void downloadPdf(String endpoint, String filename, Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar PDF");
        chooser.setInitialFileName(filename);
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        if (downloads.isDirectory()) {
            chooser.setInitialDirectory(downloads);
        }
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivo PDF", "*.pdf")
        );

        File file = chooser.showSaveDialog(owner);

        if (file == null) {
            return;
        }


        apiClient.getPdf(endpoint)
                .thenAcceptAsync(bytes -> {
                    try {
                        Files.write(file.toPath(), bytes);
                    } catch (IOException exception) {
                        throw new CompletionException(exception);
                    }
                })
                .whenComplete((result, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        Throwable cause = error;

                        while (cause instanceof CompletionException
                                && cause.getCause() != null) {
                            cause = cause.getCause();
                        }
                        createPdfErrorAlert(owner).showAndWait();

                        return;
                    }
                    createPdfSuccessAlert(owner).showAndWait();

                }));
    }

    private Alert createPdfSuccessAlert(Window owner) {
        return createPdfAlert(owner, "PDF salvo com sucesso.", false);
    }

    private Alert createPdfErrorAlert(Window owner) {
        return createPdfAlert(owner, "Falha ao salvar o PDF.", true);
    }

    private Alert createPdfAlert(Window owner, String text, boolean error) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.initOwner(owner);
        alert.setTitle("Qualifica+");

        Image logo = loadPdfLogo(error);

        DialogPane pane = new DialogPane() {
            @Override
            protected Node createButtonBar() {
                ImageView brand = new ImageView(logo);
                brand.setFitWidth(52);
                brand.setFitHeight(52);
                brand.setPreserveRatio(true);
                brand.setSmooth(true);
                brand.setAccessibleText("Qualifica+ Ijuí");
                brand.getStyleClass().add("pdf-brand-logo");

                Label message = new Label(text);
                message.setMinWidth(Region.USE_PREF_SIZE);

                ButtonBar buttons = (ButtonBar) super.createButtonBar();
                buttons.setButtonMinWidth(76);

                HBox row = new HBox(20, brand, message, buttons);
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(message, Priority.ALWAYS);
                return row;
            }
        };
        pane.getStylesheets().add(Objects.requireNonNull(
                RegisterService.class.getResource("/css/application.css")).toExternalForm());
        pane.getStylesheets().add(Objects.requireNonNull(
                RegisterService.class.getResource("/css/pdf-dialog.css")).toExternalForm());
        pane.getStyleClass().addAll("pdf-alert", "pdf-branded-alert",
                error ? "pdf-error-alert" : "pdf-success-alert");
        pane.setContent(new Region());

        double[] dragOffset = new double[2];
        pane.addEventFilter(MouseEvent.MOUSE_PRESSED, (event)-> {
            Window window = pane.getScene().getWindow();
            dragOffset[0] = (event.getScreenX() - window.getX());
            dragOffset[1] = (event.getScreenY() - window.getY());
        });

        pane.addEventFilter(MouseEvent.MOUSE_DRAGGED, (event) -> {
            Window window = pane.getScene().getWindow();
            window.setX(event.getScreenX() - dragOffset[0]);
            window.setY(event.getScreenY() - dragOffset[1]);
        });

        alert.initStyle(StageStyle.UNDECORATED);
        alert.setDialogPane(pane);
        alert.getButtonTypes().add(ButtonType.OK);
        alert.setOnShown(event -> {
            if (pane.getScene().getWindow() instanceof Stage stage) {
                stage.getIcons().setAll(logo);
            }
        });
        return alert;
    }

    private Image loadPdfLogo(boolean error) {
        Image logo = new Image(Objects.requireNonNull(RegisterService.class
                .getResource("/images/logo-qualifica-atalho.png")).toExternalForm(), 64, 64, true, true);
        if (!error) {
            return logo;
        }

        ImageView redLogo = new ImageView(logo);
        Blend tint = new Blend(BlendMode.SRC_ATOP);
        tint.setTopInput(new ColorInput(0, 0, logo.getWidth(), logo.getHeight(), Color.web("#ef4444")));
        redLogo.setEffect(tint);

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return redLogo.snapshot(parameters, null);
    }
}
