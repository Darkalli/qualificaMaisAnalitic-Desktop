package com.qualificamaisanaliticinterface.features.courses;

import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CoursesControllerTest {
    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException alreadyStarted) {
            // JavaFX may already be running in another test class.
        }
    }

    @Test
    void loadsCoursesAndFormatsTableColumns() throws Exception {
        View view = onFxThread(() -> load(CompletableFuture.completedFuture(courses())));

        onFxThread(() -> {
            assertEquals("3", view.controller.totalCoursesLabel.getText());
            assertEquals(3, view.controller.coursesTable.getItems().size());
            assertEquals("Curso A", view.controller.namesColumn.getCellData(0));
            assertEquals("Tarde", view.controller.periodColumn.getCellData(0));
            assertEquals("10/11/2026", view.controller.startColumn.getCellData(0));
            assertEquals("20/11/2026", view.controller.finishColumn.getCellData(0));
            assertEquals("Ativo", view.controller.statusColumn.getCellData(0));
            assertTrue(view.controller.coursesTable.getSelectionModel().isCellSelectionEnabled());
            return null;
        });
    }

    @Test
    void filtersByNamePeriodStatusAndDatesAndClearsFilters() throws Exception {
        View view = onFxThread(() -> load(CompletableFuture.completedFuture(courses())));

        onFxThread(() -> {
            CoursesController controller = view.controller;
            TextField search = (TextField) view.root.lookup("#searchField");
            DatePicker start = (DatePicker) view.root.lookup("#startFilter");
            DatePicker finish = (DatePicker) view.root.lookup("#finishFilter");

            search.setText("  CURSO B  ");
            controller.onSearchClick();
            assertEquals(List.of(courses().get(1)), List.copyOf(controller.coursesTable.getItems()));

            search.clear();
            controller.periodFilter.setValue(Period.MORNING);
            controller.statusFilter.setValue(StatusCourse.ACTIVE);
            start.setValue(LocalDate.of(2026, 12, 1));
            finish.setValue(LocalDate.of(2026, 12, 15));
            controller.onSearchClick();
            assertEquals(List.of(courses().get(2)), List.copyOf(controller.coursesTable.getItems()));

            Hyperlink clear = (Hyperlink) view.root.lookup(".clear-filters");
            clear.fire();
            assertEquals(3, controller.coursesTable.getItems().size());
            assertTrue(search.getText().isEmpty());
            assertNull(controller.periodFilter.getValue());
            assertNull(controller.statusFilter.getValue());
            assertNull(start.getValue());
            assertNull(finish.getValue());
            return null;
        });
    }

    @Test
    void emptySearchShowsFilteredPlaceholder() throws Exception {
        View view = onFxThread(() -> load(CompletableFuture.completedFuture(courses())));

        onFxThread(() -> {
            ((TextField) view.root.lookup("#searchField")).setText("inexistente");
            view.controller.onSearchClick();
            assertTrue(view.controller.coursesTable.getItems().isEmpty());
            assertEquals("Nenhuma inscrição encontrada para esses filtros.", ((Label) view.controller.coursesTable.getPlaceholder()).getText());
            return null;
        });
    }

    @Test
    void noCoursesShowsEmptyPlaceholder() throws Exception {
        View view = onFxThread(() -> load(CompletableFuture.completedFuture(List.of())));

        onFxThread(() -> {
            assertEquals("0", view.controller.totalCoursesLabel.getText());
            assertEquals("Nenhuma inscrição cadastrada.", ((Label) view.controller.coursesTable.getPlaceholder()).getText());
            return null;
        });
    }

    @Test
    void expiredSessionShowsLoadError() throws Exception {
        View view = onFxThread(() -> load(CompletableFuture.failedFuture(new ApiException(401, "Expirada"))));

        onFxThread(() -> {
            assertEquals("—", view.controller.totalCoursesLabel.getText());
            assertTrue(view.controller.coursesTable.getItems().isEmpty());
            assertEquals("Sessão inválida ou expirada. Entre novamente.", ((Label) view.controller.coursesTable.getPlaceholder()).getText());
            return null;
        });
    }

    private static View load(CompletableFuture<List<CourseDto>> result) throws Exception {
        CourseService service = new CourseService() {
            @Override public CompletableFuture<List<CourseDto>> findAll() {
                return result;
            }
        };
        FXMLLoader loader = new FXMLLoader(CoursesControllerTest.class.getResource("/views/pages/courses/courses.fxml"));
        loader.setControllerFactory(type -> new CoursesController(service));
        Parent root = loader.load();
        return new View(loader.getController(), root);
    }

    private static List<CourseDto> courses() {
        return List.of(
                new CourseDto(1L, "Curso A", "Descrição A", "2026-11-10", "2026-11-20", StatusCourse.ACTIVE, Period.AFTERNOON),
                new CourseDto(2L, "Curso B", "Descrição B", "2026-11-11", "2026-11-25", StatusCourse.CANCELED, Period.MORNING),
                new CourseDto(3L, "Curso C", "Descrição C", "2026-12-01", "2026-12-15", StatusCourse.ACTIVE, Period.MORNING));
    }

    private static <T> T onFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }

    private record View(CoursesController controller, Parent root) {}
}
