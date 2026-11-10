package com.qualificamaisanaliticinterface.features.people;

import com.qualificamaisanaliticinterface.features.enuns.Disabilities;
import com.qualificamaisanaliticinterface.features.enuns.Education;
import com.qualificamaisanaliticinterface.features.enuns.Gender;
import com.qualificamaisanaliticinterface.features.enuns.WorkState;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

final class PersonTestSupport {
    private PersonTestSupport() {}

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
        FXMLLoader loader = new FXMLLoader(PersonTestSupport.class.getResource("/views/pages/people/" + fxml));
        loader.load();
        return loader.getController();
    }

    static PersonDto person() {
        return new PersonDto(7L, "Ana Silva", "Ana", "12345678901", "ana@example.com",
                "11999999999", true, "11888888888",
                new PersonDto.AddressDto(42, "Rua das Flores", "Centro"),
                Gender.FEMALE, Education.HIGH_SCHOOL_COMPLETE, WorkState.ONLY_STUDYING,
                Set.of(Disabilities.HEARING));
    }
}
