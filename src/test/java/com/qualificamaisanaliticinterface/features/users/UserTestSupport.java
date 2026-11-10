package com.qualificamaisanaliticinterface.features.users;

import com.qualificamaisanaliticinterface.features.enuns.UserRole;
import com.qualificamaisanaliticinterface.features.users.dto.UserDto;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

final class UserTestSupport {
    private UserTestSupport() {}

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

    static Parent load(String fxml, Object controller) throws IOException {
        FXMLLoader loader = new FXMLLoader(UserTestSupport.class.getResource("/views/pages/users/" + fxml));
        loader.setControllerFactory(type -> controller);
        return loader.load();
    }

    static UserDto user() {
        return new UserDto(7L, "ana", UserRole.ADMIN);
    }
}
