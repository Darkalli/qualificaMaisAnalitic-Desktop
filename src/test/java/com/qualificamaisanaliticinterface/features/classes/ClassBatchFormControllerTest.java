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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDate;

import static com.qualificamaisanaliticinterface.features.classes.ClassTestSupport.*;

class ClassBatchFormControllerTest {
    @BeforeAll
    static void startJavaFx() {
        startFx();
    }

    @Test
    void shouldRejectBatchWithEndDateBeforeStartDate() throws Exception {
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
        ClassBatchFormController controller = new ClassBatchFormController(registers, classes);
        Stage stage = openStage("class-batch-form.fxml", controller);
        try {
            fx(() -> {
                controller.courseField.setValue(course);
                controller.startDateField.getEditor().setText("22/11/2026");
                controller.finishDateField.getEditor().setText("12/11/2026");
                controller.monday.setSelected(true);
                controller.startField.setText("20:00");
                controller.finishField.setText("22:30");
                Assertions.assertDoesNotThrow(controller.add::fire);
                Assertions.assertEquals("A data de começo deve ser anterior a de final.", controller.info.getText());
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
    void shouldRejectInvalidBatchTimes() throws Exception {
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
        ClassBatchFormController controller = new ClassBatchFormController(registers, classes);
        Stage stage = openStage("class-batch-form.fxml", controller);
        try {
            fx(() -> {
                controller.courseField.setValue(course);
                controller.startDateField.getEditor().setText("12/11/2026");
                controller.finishDateField.getEditor().setText("22/11/2026");
                controller.monday.setSelected(true);
                controller.startField.setText("25:00");
                controller.finishField.setText("22:30");
                Assertions.assertDoesNotThrow(controller.add::fire);
                Assertions.assertEquals("Informe uma hora válida.", controller.info.getText());
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
    void shouldCreateClassesForSelectedWeekdaysWithinDateRange() throws Exception {
        CourseDto course = new CourseDto(10L, "Curso A", null, null, null, null, null);

        RegisterService fakeRegisterService = new RegisterService() {
            @Override
            public CompletableFuture<List<CourseDto>> findCourses() {
                return CompletableFuture.completedFuture(List.of(course));
            }
        };

        Map<Long, ClassDto> classes = new HashMap<>();

        ClassService fakeClassService = new ClassService() {
            long id = 0L;

            @Override
            public CompletableFuture<String> addClass(String day, Period period, String start, String finish, StatusClass status, Long courseId) {
                Assertions.assertEquals(course.id(), courseId);
                classes.put(id, new ClassDto(id++, day, period, start, finish, status, course));
                return CompletableFuture.completedFuture("Teste de adição de classe");
            }
        };

        ClassBatchFormController controller = new ClassBatchFormController(fakeRegisterService, fakeClassService);

        Stage stage = openStage("class-batch-form.fxml", controller);

        try {
            fx(() -> {
                controller.courseField.setValue(course);
                controller.startDateField.getEditor().setText("12/11/2026");
                controller.finishDateField.getEditor().setText("22/11/2026");
                controller.monday.fire();
                controller.tuesday.fire();
                controller.wednesday.fire();
                controller.thursday.fire();
                controller.friday.fire();
                controller.startField.setText("20:00");
                controller.finishField.setText("22:30");
                Assertions.assertFalse(controller.add.isDisabled());
                controller.add.fire();

                Assertions.assertEquals(7, classes.size());

                List<String> expectedDays = List.of(
                        "2026-11-12", "2026-11-13", "2026-11-16", "2026-11-17",
                        "2026-11-18", "2026-11-19", "2026-11-20"
                );
                for (int i = 0; i < expectedDays.size(); i++) {
                    ClassDto saved = classes.get((long) i);
                    Assertions.assertNotNull(saved);
                    Assertions.assertEquals(expectedDays.get(i), saved.day());
                    Assertions.assertEquals(Period.NIGHT, saved.session());
                    Assertions.assertEquals("20:00", saved.start());
                    Assertions.assertEquals("22:30", saved.finish());
                    Assertions.assertEquals(StatusClass.ACTIVE, saved.statusClass());
                }


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
    void shouldRejectBatchWithoutCourseOrSelectedWeekdays() throws Exception {
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
                return CompletableFuture.completedFuture("Teste de adição de classe");
            }
        };

        ClassBatchFormController controller = new ClassBatchFormController(fakeRegisterService, fakeClassService);

        Stage stage = openStage("class-batch-form.fxml", controller);

        try {
            fx(() -> {
                controller.startDateField.getEditor().setText("12/11/2026");
                controller.finishDateField.getEditor().setText("22/11/2026");
                controller.monday.setSelected(true);
                controller.tuesday.setSelected(true);
                controller.wednesday.setSelected(true);
                controller.thursday.setSelected(true);
                controller.friday.setSelected(true);
                controller.startField.setText("20:00");
                controller.finishField.setText("22:30");
                Assertions.assertFalse(controller.add.isDisabled());
                controller.add.fire();

                Assertions.assertEquals("Informe o curso", controller.info.getText());
                return null;
            });
            fx(() -> {
                controller.courseField.setValue(course);
                controller.startDateField.getEditor().setText("12/11/2026");
                controller.finishDateField.getEditor().setText("22/11/2026");
                controller.monday.setSelected(false);
                controller.tuesday.setSelected(false);
                controller.wednesday.setSelected(false);
                controller.thursday.setSelected(false);
                controller.friday.setSelected(false);
                controller.startField.setText("20:00");
                controller.finishField.setText("22:30");
                Assertions.assertFalse(controller.add.isDisabled());
                controller.add.fire();

                Assertions.assertEquals("Selecione pelo menos um dia da semana.", controller.info.getText());
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
    void shouldKeepBatchFormOpenAfterPartialFailure() throws Exception {
        CourseDto course = new CourseDto(10L, "Curso A", null, null, null, null, null);

        RegisterService fakeRegisterService = new RegisterService() {
            @Override
            public CompletableFuture<List<CourseDto>> findCourses() {
                return CompletableFuture.completedFuture(List.of(course));
            }
        };
        Map<Long, ClassDto> addedClasses = new HashMap<>();
        Map<Long, Integer> attempts = new HashMap<>();
        List<Long> failedIds = new ArrayList<>();

        ClassService fakeClassService = new ClassService() {
            private int calls = 0;
            long id;
            @Override
            public CompletableFuture<String> addClass(String day, Period period, String start, String finish, StatusClass status, Long courseId) {
                Assertions.assertEquals(course.id(), courseId);
                calls++;
                attempts.merge(id, 1, Integer::sum);
                if (calls == 2) {
                    failedIds.add(id);
                    return CompletableFuture.failedFuture(new RuntimeException("Falha simulada"));
                }
                addedClasses.put(id,new ClassDto(id++, day, period, start, finish, status, course));
                return CompletableFuture.completedFuture("sucesso");
            }
        };

        ClassBatchFormController controller = new ClassBatchFormController(fakeRegisterService, fakeClassService);

        Stage stage = openStage("class-batch-form.fxml", controller);

        try {
            fx(() -> {
                controller.courseField.setValue(course);
                controller.startDateField.getEditor().setText("12/11/2026");
                controller.finishDateField.getEditor().setText("22/11/2026");
                controller.monday.setSelected(true);
                controller.tuesday.setSelected(true);
                controller.wednesday.setSelected(true);
                controller.thursday.setSelected(true);
                controller.friday.setSelected(true);
                controller.startField.setText("20:00");
                controller.finishField.setText("22:30");
                Assertions.assertFalse(controller.add.isDisabled());
                controller.add.fire();
                return null;
            });
            fx(() -> {
                Assertions.assertEquals(6, addedClasses.size());
                Assertions.assertEquals(1, failedIds.size());
                Assertions.assertEquals("Falha simulada", controller.info.getText());
                Assertions.assertTrue(stage.isShowing());
                Assertions.assertFalse(controller.add.isDisabled());
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
