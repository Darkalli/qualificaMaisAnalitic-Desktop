package com.qualificamaisanaliticinterface.features.registers;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

final class RegisterTestSupport {
    private RegisterTestSupport() {}

    static void startFx() {
        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException alreadyStarted) {
            // Another test class already started JavaFX.
        }
    }

    static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }

    static <T> T load(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(RegisterTestSupport.class.getResource("/views/pages/registers/" + fxml));
        loader.load();
        return loader.getController();
    }

    static PersonDto person() {
        return new PersonDto(7L, "Ana Silva", null, "12345678901", null, null,
                false, null, null, null, null, null, null);
    }

    static CourseDto course() {
        return new CourseDto(3L, "Curso A", "Descrição A", "2026-11-10", "2026-11-20",
                StatusCourse.ACTIVE, Period.AFTERNOON);
    }

    static RegisterDto register(StatusRegister status) {
        return new RegisterDto(11L, person(), course(), "2026-11-10", status);
    }
}
