package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.classes.dto.UpdateClassDto;
import com.qualificamaisanaliticinterface.features.courses.CourseService;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static com.qualificamaisanaliticinterface.features.classes.ClassTestSupport.*;

class ClassUpdateControllerTest {
    @BeforeAll
    static void startJavaFx() {
        startFx();
    }

    @Test
    void shouldKeepUpdateFormOpenWhenUpdateFails() throws Exception {
        CourseDto course = new CourseDto(10L, "Curso A", null, null, null, null, null);
        ClassDto courseClass = new ClassDto(1L, "2026-11-20", Period.NIGHT,
                "20:30", "21:30", StatusClass.ACTIVE, course);
        AtomicInteger notifications = new AtomicInteger();
        CourseService courses = new CourseService() {
            @Override public CompletableFuture<List<CourseDto>> findAll() {
                return CompletableFuture.completedFuture(List.of(course));
            }
        };
        ClassService classes = new ClassService() {
            @Override public CompletableFuture<String> updateClass(Long id, String day, Period session,
                                                                     String start, String finish,
                                                                     StatusClass status, Long courseId) {
                Assertions.assertEquals(courseClass.id(), id);
                return CompletableFuture.failedFuture(new IllegalStateException("Falha simulada"));
            }
        };
        ClassUpdateController controller = new ClassUpdateController(courses, classes);
        controller.setOnChanged(notifications::incrementAndGet);
        Stage stage = openStage("class-update.fxml", controller, () -> controller.setClass(courseClass));
        try {
            fx(() -> {
                controller.startField.setText("19:10");
                controller.finishField.setText("22:30");
                controller.add.fire();
                return null;
            });
            fx(() -> {
                Assertions.assertEquals("Falha simulada", controller.info.getText());
                Assertions.assertTrue(stage.isShowing());
                Assertions.assertFalse(controller.formDialogPane.getContent().isDisabled());
                Assertions.assertFalse(controller.add.isDisabled());
                Assertions.assertEquals(0, notifications.get());
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
    void shouldPopulateClassFieldsForEditing() throws Exception {
        CourseDto course = new CourseDto(10L, "Curso A", null, null, null, null, null);
        ClassDto classDto = new ClassDto(1L,"2026-11-20", Period.AFTERNOON, "20:30", "22:30", StatusClass.ACTIVE, course);

        CourseService fakeCourseService = new CourseService(){
            @Override
            public CompletableFuture<List<CourseDto>> findAll() {
                return CompletableFuture.completedFuture(List.of(course));
            }
        };

        ClassUpdateController controller = new ClassUpdateController(fakeCourseService, new ClassService());

        Stage stage = openStage("class-update.fxml", controller, () -> controller.setClass(classDto));

        try {
            fx(() -> {
                Assertions.assertEquals(course, controller.courseField.getValue());
                Assertions.assertEquals(LocalDate.of(2026, 11, 20), controller.dateField.getValue());
                Assertions.assertEquals("20:30", controller.startField.getText());
                Assertions.assertEquals("22:30", controller.finishField.getText());
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
    void shouldRejectEndTimeNotAfterStartTime() throws Exception {
        CourseDto course = new CourseDto(10L, "Curso A", null, null, null, null, null);
        ClassDto classDto = new ClassDto(1L,"2026-11-20", Period.AFTERNOON, "22:30", "20:30", StatusClass.ACTIVE, course);

        CourseService fakeCourseService = new CourseService(){
            @Override
            public CompletableFuture<List<CourseDto>> findAll() {
                return CompletableFuture.completedFuture(List.of(course));
            }
        };

        ClassService fakeClassService = new ClassService(){
            @Override
            public CompletableFuture<String> updateClass(Long id,String day, Period session, String start, String finish, StatusClass statusClass, Long courseId) {
                return CompletableFuture.completedFuture("Teste");
            }
        };

        ClassUpdateController controller = new ClassUpdateController(fakeCourseService, fakeClassService);

        Stage stage = openStage("class-update.fxml", controller, () -> controller.setClass(classDto));

        try {
            fx(() -> {
                Assertions.assertEquals(course, controller.courseField.getValue());
                Assertions.assertEquals(LocalDate.of(2026, 11, 20), controller.dateField.getValue());
                Assertions.assertEquals("22:30", controller.startField.getText());
                Assertions.assertEquals("20:30", controller.finishField.getText());
                controller.add.fire();
                Assertions.assertEquals("O início da aula deve ser anterior ao final.", controller.info.getText());

                controller.startField.setText("20:30");
                Assertions.assertEquals(course, controller.courseField.getValue());
                Assertions.assertEquals(LocalDate.of(2026, 11, 20), controller.dateField.getValue());
                Assertions.assertEquals("20:30", controller.startField.getText());
                Assertions.assertEquals("20:30", controller.finishField.getText());
                controller.add.fire();
                Assertions.assertEquals("O início da aula deve ser anterior ao final.", controller.info.getText());

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
    void shouldUpdateScheduleWithoutChangingClassStatus() throws Exception {
        CourseDto course = new CourseDto(10L, "Curso A", null, null, null, null, null);
        ClassDto classDto = new ClassDto(1L, "2026-11-20", Period.NIGHT, "20:30", "21:30", StatusClass.POSTPONED, course);
        List<UpdateClassDto> updates = new ArrayList<>();

        CourseService fakeCourseService = new CourseService() {
            @Override
            public CompletableFuture<List<CourseDto>> findAll() {
                return CompletableFuture.completedFuture(List.of(course));
            }
        };

        ClassService fakeClassService = new ClassService() {
            @Override
            public CompletableFuture<String> updateClass(Long id, String day, Period session, String start,
                                                         String finish, StatusClass statusClass, Long courseId) {
                updates.add(new UpdateClassDto(id, day, session, start, finish, statusClass,
                        new UpdateClassDto.CourseReference(courseId)));
                return CompletableFuture.completedFuture("Teste");
            }
        };

        ClassUpdateController controller = new ClassUpdateController(fakeCourseService, fakeClassService);

        Stage stage = openStage("class-update.fxml", controller, () -> controller.setClass(classDto));

        try {
            fx(() -> {
                Assertions.assertFalse(controller.add.isDisabled());
                controller.dateField.getEditor().setText(
                        controller.dateField.getConverter().toString(LocalDate.of(2026, 11, 21)));
                controller.startField.setText("19:10");
                controller.finishField.setText("22:30");
                controller.add.fire();
                return null;
            });

            Assertions.assertEquals(List.of(new UpdateClassDto(
                    1L, "2026-11-21", Period.NIGHT, "19:10", "22:30", null,
                    new UpdateClassDto.CourseReference(10L))), updates);
        } finally {
            fx(() -> {
                stage.close();
                return null;
            });
        }
    }

}
