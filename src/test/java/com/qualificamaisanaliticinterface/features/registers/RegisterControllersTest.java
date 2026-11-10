package com.qualificamaisanaliticinterface.features.registers;

import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.people.PersonService;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.qualificamaisanaliticinterface.features.registers.RegisterTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class RegisterControllersTest {
    @BeforeAll static void startJavaFx() { startFx(); }

    @Test
    void newRegisterLoadsPersonAndCourseOptions() throws Exception {
        RegisterFormController controller = fx(() -> loadForm(List.of(person()), List.of(course())));

        fx(() -> {
            assertEquals(LocalDate.now(), controller.dateField.getValue());
            assertEquals(List.of(person()), List.copyOf(controller.personField.getItems()));
            assertEquals(List.of(course()), List.copyOf(controller.courseField.getItems()));
            assertEquals("Ana Silva — 12345678901", controller.personField.getConverter().toString(person()));
            assertFalse(controller.add.isDisabled());
            assertEquals("", controller.info.getText());
            return null;
        });
    }

    @Test
    void newRegisterRequiresPersonAndCourse() throws Exception {
        RegisterFormController controller = fx(() -> loadForm(List.of(person()), List.of(course())));

        fx(() -> {
            controller.add.fire();
            assertEquals("Selecione a pessoa e o curso para continuar.", controller.info.getText());
            assertFalse(controller.formDialogPane.getContent().isDisable());
            return null;
        });
    }

    @Test
    void newRegisterRejectsInvalidDateBeforeCallingApi() throws Exception {
        RegisterFormController controller = fx(() -> loadForm(List.of(person()), List.of(course())));

        fx(() -> {
            controller.personField.setValue(person());
            controller.courseField.setValue(course());
            controller.dateField.getEditor().setText("data inválida");
            controller.add.fire();
            assertEquals("Informe uma data de inscrição válida.", controller.info.getText());
            assertFalse(controller.formDialogPane.getContent().isDisable());
            return null;
        });
    }

    @Test
    void newRegisterNeedsAvailableCourses() throws Exception {
        RegisterFormController controller = fx(() -> loadForm(List.of(person()), List.of()));

        fx(() -> {
            assertTrue(controller.add.isDisabled());
            assertEquals("Cadastre um curso antes de criar uma inscrição.", controller.info.getText());
            return null;
        });
    }

    @Test
    void registerInfoShowsValuesAndFormattedDate() throws Exception {
        fx(() -> {
            RegisterInfoController controller = load("register-info.fxml");
            controller.setRegister(register(StatusRegister.ACTIVE));
            assertEquals("Ana Silva", controller.personName.getText());
            assertEquals("12345678901", controller.personCpf.getText());
            assertEquals("Curso A", controller.courseName.getText());
            assertEquals("Descrição A", controller.courseDescription.getText());
            assertEquals("10/11/2026", controller.dateField.getText());
            assertEquals("Ativa", controller.statusField.getText());
            assertEquals("11", controller.numberField.getText());
            return null;
        });
    }

    @Test
    void registerInfoShowsFallbacksForMissingValues() throws Exception {
        fx(() -> {
            RegisterInfoController controller = load("register-info.fxml");
            CourseDto sparseCourse = new CourseDto(3L, "Curso A", null, null, null, null, null);
            controller.setRegister(new RegisterDto(null, person(), sparseCourse, null, null));
            assertEquals("Não informado", controller.courseDescription.getText());
            assertEquals("Não informada", controller.dateField.getText());
            assertEquals("Não informada", controller.statusField.getText());
            assertEquals("Não informado", controller.numberField.getText());
            return null;
        });
    }

    @Test
    void activeRegisterOffersCancellation() throws Exception {
        fx(() -> {
            RegisterStatusController controller = load("register-status.fxml");
            controller.setRegister(register(StatusRegister.ACTIVE));
            assertEquals("Cancelar inscrição?", controller.title.getText());
            assertEquals("Cancelar inscrição", controller.confirm.getText());
            assertFalse(controller.confirm.isDisabled());
            assertTrue(controller.statusDialogPane.getStyleClass().contains("delete-dialog"));
            return null;
        });
    }

    @Test
    void canceledRegisterOffersReactivation() throws Exception {
        fx(() -> {
            RegisterStatusController controller = load("register-status.fxml");
            controller.setRegister(register(StatusRegister.CANCELED));
            assertEquals("Reativar inscrição?", controller.title.getText());
            assertEquals("Reativar inscrição", controller.confirm.getText());
            assertFalse(controller.confirm.isDisabled());
            return null;
        });
    }

    @Test
    void missingStatusDisablesConfirmation() throws Exception {
        fx(() -> {
            RegisterStatusController controller = load("register-status.fxml");
            controller.setRegister(register(null));
            assertTrue(controller.confirm.isDisabled());
            return null;
        });
    }

    private static RegisterFormController loadForm(List<PersonDto> people, List<CourseDto> courses) throws Exception {
        PersonService personService = new PersonService() {
            @Override public CompletableFuture<List<PersonDto>> findAll() { return CompletableFuture.completedFuture(people); }
        };
        RegisterService registerService = new RegisterService() {
            @Override public CompletableFuture<List<CourseDto>> findCourses() { return CompletableFuture.completedFuture(courses); }
        };
        FXMLLoader loader = new FXMLLoader(RegisterControllersTest.class.getResource("/views/pages/registers/register-form.fxml"));
        loader.setControllerFactory(type -> new RegisterFormController(personService, registerService));
        loader.load();
        return loader.getController();
    }
}
