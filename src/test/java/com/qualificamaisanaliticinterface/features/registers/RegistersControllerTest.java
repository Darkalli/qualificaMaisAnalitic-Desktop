package com.qualificamaisanaliticinterface.features.registers;

import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.qualificamaisanaliticinterface.features.registers.RegisterTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class RegistersControllerTest {
    @BeforeAll static void startJavaFx() { startFx(); }

    @Test
    void loadsRegistrationsAndFormatsColumns() throws Exception {
        RegistersController controller = fx(() -> load(CompletableFuture.completedFuture(registers())));

        fx(() -> {
            assertEquals("3", controller.totalRegistersLabel.getText());
            assertEquals(3, controller.registersTable.getItems().size());
            assertEquals("12345678901", controller.cpfsColumn.getCellData(0));
            assertEquals("Ana Silva", controller.namesColumn.getCellData(0));
            assertEquals("Curso A", controller.courseColumn.getCellData(0));
            assertEquals("10/11/2026", controller.dateColumn.getCellData(0));
            assertEquals("Ativa", controller.statusColumn.getCellData(0));
            assertEquals(2, controller.courseFilter.getItems().size());
            assertTrue(controller.registersTable.getSelectionModel().isCellSelectionEnabled());
            return null;
        });
    }

    @Test
    void filtersByNameCpfCourseAndStatusAndClearsFilters() throws Exception {
        RegistersController controller = fx(() -> load(CompletableFuture.completedFuture(registers())));

        fx(() -> {
            controller.searchField.setText("  ANA  ");
            controller.onSearchClick();
            assertEquals(List.of(registers().getFirst()), List.copyOf(controller.registersTable.getItems()));

            controller.searchField.setText("987.654.321-00");
            controller.onSearchClick();
            assertEquals(List.of(registers().get(1)), List.copyOf(controller.registersTable.getItems()));

            controller.searchField.clear();
            controller.courseFilter.setValue(controller.courseFilter.getItems().stream()
                    .filter(course -> course.id() == 4L).findFirst().orElseThrow());
            controller.statusFilter.setValue(StatusRegister.ACTIVE);
            controller.onSearchClick();
            assertEquals(List.of(registers().get(2)), List.copyOf(controller.registersTable.getItems()));

            controller.onClearFiltersClick();
            assertEquals(3, controller.registersTable.getItems().size());
            assertTrue(controller.searchField.getText().isEmpty());
            assertNull(controller.courseFilter.getValue());
            assertNull(controller.statusFilter.getValue());
            return null;
        });
    }

    @Test
    void noMatchesShowsFilterPlaceholder() throws Exception {
        RegistersController controller = fx(() -> load(CompletableFuture.completedFuture(registers())));

        fx(() -> {
            controller.searchField.setText("inexistente");
            controller.onSearchClick();
            assertTrue(controller.registersTable.getItems().isEmpty());
            assertEquals("Nenhuma inscrição encontrada para esses filtros.", ((Label) controller.registersTable.getPlaceholder()).getText());
            return null;
        });
    }

    @Test
    void expiredSessionShowsLoadError() throws Exception {
        RegistersController controller = fx(() -> load(CompletableFuture.failedFuture(new ApiException(401, "Expirada"))));

        fx(() -> {
            assertEquals("—", controller.totalRegistersLabel.getText());
            assertTrue(controller.registersTable.getItems().isEmpty());
            assertEquals("Sessão inválida ou expirada. Entre novamente.", ((Label) controller.registersTable.getPlaceholder()).getText());
            return null;
        });
    }

    private static RegistersController load(CompletableFuture<List<RegisterDto>> result) throws Exception {
        RegisterService service = new RegisterService() {
            @Override public CompletableFuture<List<RegisterDto>> findAll() { return result; }
        };
        FXMLLoader loader = new FXMLLoader(RegistersControllerTest.class.getResource("/views/pages/registers/registers.fxml"));
        loader.setControllerFactory(type -> new RegistersController(service));
        Parent root = loader.load();
        assertNotNull(root);
        return loader.getController();
    }

    private static List<RegisterDto> registers() {
        CourseDto courseB = new CourseDto(4L, "Curso B", null, null, null, null, null);
        PersonDto bruno = new PersonDto(8L, "Bruno Lima", null, "98765432100", null,
                null, false, null, null, null, null, null, null);
        PersonDto carla = new PersonDto(9L, "Carla Souza", null, "11122233344", null,
                null, false, null, null, null, null, null, null);
        return List.of(register(StatusRegister.ACTIVE),
                new RegisterDto(12L, bruno, course(), "2026-11-11", StatusRegister.CANCELED),
                new RegisterDto(13L, carla, courseB, "2026-11-12", StatusRegister.ACTIVE));
    }
}
