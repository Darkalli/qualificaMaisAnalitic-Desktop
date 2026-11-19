package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.qualificamaisanaliticinterface.AuthTestSupport.*;

class HomeNavigationTest {
    @BeforeAll
    static void startJavaFx() {
        startFx();
    }

    @Test
    void shouldOpenSidebarPagesAndReturnToSummaryWhenMenuButtonIsClicked() throws Exception {
        fx(() -> {
            HomeSummaryService summary = new HomeSummaryService(null, null, null, null) {
                @Override
                public CompletableFuture<Long> activeCourses() { return CompletableFuture.completedFuture(0L); }

                @Override
                public CompletableFuture<Long> activeRegisters() { return CompletableFuture.completedFuture(0L); }

                @Override
                public CompletableFuture<Long> registeredPeople() { return CompletableFuture.completedFuture(0L); }

                @Override
                public CompletableFuture<List<ClassDto>> findTodaysClasses(LocalDate date) {
                    return CompletableFuture.completedFuture(List.of());
                }

                @Override
                public CompletableFuture<List<RegisterDto>> recentRegisters() {
                    return CompletableFuture.completedFuture(List.of());
                }

                @Override
                public CompletableFuture<List<ClassDto>> pendingPresences(LocalDateTime now) {
                    return CompletableFuture.completedFuture(List.of());
                }
            };
            HomeController controller = new HomeController(null, null, summary, CLOCK);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/home.fxml"));
            loader.setControllerFactory(type -> controller);
            Parent home = loader.load();
            new Scene(home, 1380, 810);
            var homeContent = controller.workspace.getCenter();
            Label title = (Label) loader.getNamespace().get("pageTitle");
            var sideButtons = home.lookupAll(".side-button").stream().map(Button.class::cast).toList();
            Button menuButton = sideButtons.stream().filter(button -> "Menu".equals(button.getText()))
                    .findFirst().orElseThrow();
            var buttons = sideButtons.stream().filter(button -> button.getUserData() != null).toList();
            Assertions.assertEquals(6, buttons.size());

            for (Button button : buttons) {
                String originalPath = (String) button.getUserData();
                Assertions.assertNotNull(getClass().getResource(originalPath), originalPath);
                button.setUserData("/views/navigation-test-page.fxml");
                button.fire();

                Assertions.assertNotSame(homeContent, controller.workspace.getCenter());
                Assertions.assertEquals("Página de teste", ((Label) controller.workspace.getCenter()).getText());
                Assertions.assertEquals(button.getText(), title.getText());

                menuButton.fire();

                Assertions.assertSame(homeContent, controller.workspace.getCenter());
                Assertions.assertEquals("Início", title.getText());
                button.setUserData(originalPath);
            }
            return null;
        });
    }
}
