package com.qualificamaisanaliticinterface.features.home;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static com.qualificamaisanaliticinterface.AuthTestSupport.*;

class HomeNavigationTest {
    @BeforeAll
    static void startJavaFx() {
        startFx();
    }

    @Test
    void shouldOpenSidebarPagesAndReturnToSummaryWhenLogoIsClicked() throws Exception {
        fx(() -> {
            HomeSummaryService summary = new HomeSummaryService(null, null, null, null) {
                @Override
                public CompletableFuture<Long> activeCourses() { return CompletableFuture.completedFuture(0L); }

                @Override
                public CompletableFuture<Long> activeRegisters() { return CompletableFuture.completedFuture(0L); }

                @Override
                public CompletableFuture<Long> registeredPeople() { return CompletableFuture.completedFuture(0L); }

                @Override
                public CompletableFuture<Long> todaysClasses(LocalDate date) { return CompletableFuture.completedFuture(0L); }
            };
            HomeController controller = new HomeController(null, null, summary, CLOCK);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/home.fxml"));
            loader.setControllerFactory(type -> controller);
            Parent home = loader.load();
            new Scene(home, 1380, 810);
            var homeContent = controller.workspace.getCenter();
            Label title = (Label) loader.getNamespace().get("pageTitle");
            var logo = home.lookup(".logo-container");
            var buttons = home.lookupAll(".side-button");
            Assertions.assertEquals(6, buttons.size());

            for (var node : buttons) {
                Button button = (Button) node;
                String originalPath = (String) button.getUserData();
                Assertions.assertNotNull(getClass().getResource(originalPath), originalPath);
                button.setUserData("/views/navigation-test-page.fxml");
                button.fire();

                Assertions.assertNotSame(homeContent, controller.workspace.getCenter());
                Assertions.assertEquals("Página de teste", ((Label) controller.workspace.getCenter()).getText());
                Assertions.assertEquals(button.getText(), title.getText());

                logo.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED,
                        0, 0, 0, 0, MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, false, null));

                Assertions.assertSame(homeContent, controller.workspace.getCenter());
                Assertions.assertEquals("Início", title.getText());
                button.setUserData(originalPath);
            }
            return null;
        });
    }
}
