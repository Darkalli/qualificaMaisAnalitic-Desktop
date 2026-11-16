package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDate;

import static com.qualificamaisanaliticinterface.features.classes.ClassTestSupport.*;

class ClassFormControllerTest {
    @BeforeAll
    static void startJavaFx() {
        startFx();
    }

    @Test
    void shouldRejectClassCreationWithoutSelectedCourse() throws Exception {
        CourseDto course = new CourseDto(10L, "Curso A", null, null, null, null, null);
        AtomicInteger requests = new AtomicInteger();
        RegisterService registers = new RegisterService() {
            @Override public CompletableFuture<List<CourseDto>> findCourses() {
                return CompletableFuture.completedFuture(List.of(course));
            }
        };
        ClassService classes = new ClassService() {
            @Override public CompletableFuture<String> addClass(String day, Period period, String start,
                                                                 String finish, StatusClass status, Long courseId) {
                requests.incrementAndGet();
                return CompletableFuture.completedFuture("ok");
            }
        };
        ClassFormController controller = new ClassFormController(registers, classes);
        Stage stage = openStage("class-form.fxml", controller);
        try {
            fx(() -> {
                Assertions.assertNull(controller.courseField.getValue());
                controller.dateField.getEditor().setText(
                        controller.dateField.getConverter().toString(LocalDate.of(2026, 11, 22)));
                controller.startField.setText("20:00");
                controller.finishField.setText("22:30");
                Assertions.assertDoesNotThrow(controller.add::fire);
                Assertions.assertEquals("Informe um curso.", controller.info.getText());
                Assertions.assertFalse(controller.formDialogPane.getContent().isDisabled());
                Assertions.assertTrue(stage.isShowing());
                Assertions.assertEquals(0, requests.get());
                return null;
            });
        } finally {
            fx(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void shouldRejectInvalidDateAndTimeBeforeCreatingClass() throws Exception {

        CourseDto course = new CourseDto(10L, "Curso A", null, null, null, null, null);

        RegisterService fakeRegisterService = new RegisterService() {
            @Override
            public CompletableFuture<List<CourseDto>> findCourses() {
                return CompletableFuture.completedFuture(List.of(course));
            }
        };

        ClassService fakeClassService = new ClassService() {
            @Override
            public CompletableFuture<String> addClass(String day, Period period, String start, String finish, StatusClass status, Long courseId) {
                return Assertions.fail(
                        "Não deveria tentar salvar com data ou horário inválido."
                );
            }
        };

        ClassFormController controller = new ClassFormController(fakeRegisterService, fakeClassService);

        Stage stage = openStage("class-form.fxml", controller);

        try {
            fx(() -> {
                controller.courseField.setValue(course);
                controller.dateField.getEditor().setText("33/33/2026");
                controller.startField.setText("20:00");
                controller.finishField.setText("22:30");
                controller.add.fire();

                Assertions.assertEquals("Informe uma data válida.", controller.info.getText());
                return null;
            });
            fx(() -> {
                controller.courseField.setValue(course);
                controller.dateField.getEditor().setText("22/11/2026");
                controller.startField.setText("22:00");
                controller.finishField.setText("25:30");
                controller.add.fire();

                Assertions.assertEquals("Informe uma hora válida.", controller.info.getText());
                return null;
            });
        } finally {
            fx(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void shouldDerivePeriodFromStartTime() throws Exception {
        CourseDto course = new CourseDto(10L, "Curso A", null, null, null, null, null);

        RegisterService fakeRegisterService = new RegisterService() {
            @Override
            public CompletableFuture<List<CourseDto>> findCourses() {
                return CompletableFuture.completedFuture(List.of(course));
            }
        };

        Map<Long, ClassDto> classes = new HashMap<>(Map.of());

        ClassService fakeClassService = new ClassService() {
            long id = 0L;

            @Override
            public CompletableFuture<String> addClass(String day, Period period, String start, String finish, StatusClass status, Long courseId) {
                classes.put(id, new ClassDto(id++, day, period, start, finish, status, course));
                return CompletableFuture.completedFuture("teste");
            }
        };

        List<String> starts = List.of(
                "08:00", "11:59", "12:00", "13:00",
                "17:59", "18:00", "20:00"
        );

        List<Period> expectedPeriods = List.of(
                Period.MORNING, Period.MORNING,
                Period.AFTERNOON, Period.AFTERNOON,
                Period.AFTERNOON, Period.NIGHT, Period.NIGHT
        );

        for (int i = 0; i < starts.size(); i++) {
            String start = starts.get(i);
            Period expected = expectedPeriods.get(i);
            long expectedId = classes.size();

            ClassFormController controller = new ClassFormController(fakeRegisterService, fakeClassService);

            Stage stage = openStage("class-form.fxml", controller);

            try {
                fx(() -> {
                    controller.courseField.setValue(course);
                    controller.dateField.getEditor().setText("22/11/2026");
                    controller.startField.setText(start);
                    controller.finishField.setText("23:00");

                    Assertions.assertFalse(controller.add.isDisabled());
                    controller.add.fire();

                    ClassDto saved = classes.get(expectedId);
                    Assertions.assertNotNull(
                            saved, "Não cadastrou a aula com início " + start
                    );
                    Assertions.assertEquals(expected, saved.session());
                    return null;
                });
            } finally {
                fx(() -> {
                    stage.close();
                    return null;
                });
            }
        }
    }

}
