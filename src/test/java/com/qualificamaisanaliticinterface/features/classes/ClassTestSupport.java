package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

final class ClassTestSupport {

    private ClassTestSupport() {}

    static void startFx() {
        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException alreadyStarted) {
        }
    }

    static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }

    static <T> T load(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(ClassTestSupport.class.getResource("/views/pages/classes/" + fxml));
        loader.load();
        return loader.getController();
    }

    static Stage openStage(String fxml, Object controller) throws Exception {
        return openStage(fxml, controller, () -> {});
    }

    static Stage openStage(String fxml, Object controller, Runnable afterShow) throws Exception {
        return fx(() -> {
            FXMLLoader loader = new FXMLLoader(
                    ClassTestSupport.class.getResource(
                            "/views/pages/classes/" + fxml
                    )
            );
            loader.setControllerFactory(type -> controller);

            Parent pane = loader.load();
            Stage window = new Stage();
            window.setScene(new Scene(pane));
            window.show();
            afterShow.run();
            return window;
        });
    }

    static PersonDto person(Long id, String name) {
        return new PersonDto(id, name, null, "12345678901", null, null,
                false, null, null, null, null, null, null);
    }

    static CourseDto course() {
        return new CourseDto(3L, "Curso A", "Descrição A", "2026-11-10", "2026-11-20",
                StatusCourse.ACTIVE, Period.AFTERNOON);
    }

    static RegisterDto register(StatusRegister status, Long personId, String personName) {
        return new RegisterDto(11L, person(personId, personName), course(), "2026-11-10", status);
    }
}
