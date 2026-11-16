package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.classes.dto.PresenceDto;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.PresenceStatus;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.converter.LocalDateStringConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.qualificamaisanaliticinterface.features.classes.ClassTestSupport.*;

class ClassesControllerTest {
    @BeforeAll
    static void startJavaFx() {
        startFx();
    }

    @Test
    void shouldIgnoreOutdatedPresenceAverageAfterFilterChange() throws Exception {
        CourseDto course = new CourseDto(10L, "Curso A", null, null, null, null, null);
        ClassDto morning = new ClassDto(1L, "2026-11-16", Period.MORNING,
                "08:00", "10:00", StatusClass.ACTIVE, course);
        ClassDto afternoon = new ClassDto(2L, "2026-11-17", Period.AFTERNOON,
                "14:00", "16:00", StatusClass.ACTIVE, course);
        CompletableFuture<List<PresenceDto>> oldMorning = new CompletableFuture<>();
        CompletableFuture<List<PresenceDto>> oldAfternoon = new CompletableFuture<>();
        CompletableFuture<List<PresenceDto>> currentAfternoon = new CompletableFuture<>();
        AtomicInteger afternoonRequests = new AtomicInteger();
        RegisterService registers = new RegisterService() {
            @Override public CompletableFuture<List<CourseDto>> findCourses() {
                return CompletableFuture.completedFuture(List.of(course));
            }
        };
        ClassService classes = new ClassService() {
            @Override public CompletableFuture<List<ClassDto>> findAll() {
                return CompletableFuture.completedFuture(List.of(morning, afternoon));
            }

            @Override public CompletableFuture<List<PresenceDto>> findAllPresences(Long courseId, Long classId) {
                Assertions.assertEquals(course.id(), courseId);
                if (classId.equals(morning.id())) return oldMorning;
                if (classId.equals(afternoon.id())) {
                    return afternoonRequests.incrementAndGet() == 1 ? oldAfternoon : currentAfternoon;
                }
                return Assertions.fail("Aula inesperada: " + classId);
            }
        };
        ClassesController controller = new ClassesController(registers, classes);
        Stage stage = openStage("classes.fxml", controller);
        try {
            fx(() -> {
                Assertions.assertEquals(List.of(morning, afternoon),
                        new ArrayList<>(controller.classesTable.getItems()));
                controller.periodFilter.setValue(Period.AFTERNOON);
                controller.search.fire();
                Assertions.assertEquals(List.of(afternoon),
                        new ArrayList<>(controller.classesTable.getItems()));
                Assertions.assertEquals(2, afternoonRequests.get());
                currentAfternoon.complete(List.of(
                        new PresenceDto(2L, person(1L, "Ana"), PresenceStatus.PRESENT),
                        new PresenceDto(2L, person(2L, "Bia"), PresenceStatus.ABSENT)));
                return null;
            });
            String expected = new DecimalFormat("0.00").format(50.0) + "%";
            fx(() -> {
                Assertions.assertEquals(expected, controller.presence.getText());
                oldMorning.complete(List.of(
                        new PresenceDto(1L, person(3L, "Caio"), PresenceStatus.PRESENT)));
                oldAfternoon.complete(List.of(
                        new PresenceDto(2L, person(4L, "Davi"), PresenceStatus.PRESENT)));
                return null;
            });
            fx(() -> {
                Assertions.assertEquals(expected, controller.presence.getText());
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
    void shouldLoadAndDisplayClasses() throws Exception {

        List<CourseDto> courses = new ArrayList<>(List.of());
        CourseDto courseA = new CourseDto(10L, "Curso A", null, null, null, null, null);
        courses.add(courseA);

        List<ClassDto> classes = List.of(
                new ClassDto(1L, "2026-11-16", Period.MORNING, "08:00", "10:00", StatusClass.ACTIVE, courseA),
                new ClassDto(2L, "2026-11-17", Period.AFTERNOON, "14:00", "16:00", StatusClass.ACTIVE, courseA),
                new ClassDto(3L, "2026-11-18", Period.NIGHT, "19:00", "21:00", StatusClass.CANCELED, courseA),
                new ClassDto(4L, "2026-11-19", Period.NIGHT, "19:00", "21:30", StatusClass.POSTPONED, courseA)
        );

        RegisterService fakeService = new RegisterService() {
            @Override
            public CompletableFuture<List<CourseDto>> findCourses() {
                return CompletableFuture.completedFuture(courses);
            }
        };

        ClassService fakeClassService = new ClassService() {
            @Override
            public CompletableFuture<List<ClassDto>> findAll() {
                return CompletableFuture.completedFuture(classes);
            }

            @Override
            public CompletableFuture<List<PresenceDto>> findAllPresences(Long courseId, Long courseClassId) {
                return CompletableFuture.completedFuture(List.of());
            }
        };

        ClassesController controller = new ClassesController(fakeService, fakeClassService);

        Stage stage = openStage("classes.fxml", controller);

        try {
            fx(() -> {
                Assertions.assertEquals(4, controller.classesTable.getItems().size());

                List<ClassDto> classDtos = new ArrayList<ClassDto>(controller.classesTable.getItems());
                Assertions.assertEquals(classes, classDtos);
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
    void shouldFilterClassesByCoursePeriodAndDate() throws Exception {
        List<CourseDto> courses = new ArrayList<>(List.of());
        CourseDto courseA = new CourseDto(10L, "Curso A", null, null, null, null, null);
        CourseDto courseB = new CourseDto(11L, "Curso B", null, null, null, null, null);
        courses.add(courseA);
        courses.add(courseB);

        List<ClassDto> classes = List.of(
                new ClassDto(1L, "2026-11-16", Period.MORNING, "08:00", "10:00", StatusClass.ACTIVE, courseA),
                new ClassDto(2L, "2026-11-17", Period.AFTERNOON, "14:00", "16:00", StatusClass.ACTIVE, courseA),
                new ClassDto(3L, "2026-11-18", Period.NIGHT, "19:00", "21:00", StatusClass.CANCELED, courseB),
                new ClassDto(3L, "2026-11-18", Period.AFTERNOON, "19:00", "21:00", StatusClass.CANCELED, courseB),
                new ClassDto(4L, "2026-11-19", Period.NIGHT, "19:00", "21:30", StatusClass.POSTPONED, courseB)
        );

        RegisterService fakeService = new RegisterService() {
            @Override
            public CompletableFuture<List<CourseDto>> findCourses() {
                return CompletableFuture.completedFuture(courses);
            }
        };

        ClassService fakeClassService = new ClassService() {
            @Override
            public CompletableFuture<List<ClassDto>> findAll() {
                return CompletableFuture.completedFuture(classes);
            }

            @Override
            public CompletableFuture<List<PresenceDto>> findAllPresences(Long courseId, Long courseClassId) {
                return CompletableFuture.completedFuture(List.of());
            }
        };

        ClassesController controller = new ClassesController(fakeService, fakeClassService);

        Stage stage = openStage("classes.fxml", controller);

        try {
            fx(() -> {
                Assertions.assertEquals(5, controller.classesTable.getItems().size());

                controller.courseField.setValue(courseB);
                LocalDate date = new LocalDateStringConverter().fromString("18/11/2026");
                controller.dateFilter.setValue(date);
                controller.periodFilter.setValue(Period.NIGHT);
                controller.search.fire();
                Assertions.assertEquals(1, controller.classesTable.getItems().size());
                Assertions.assertEquals(new ClassDto(3L, "2026-11-18", Period.NIGHT, "19:00", "21:00", StatusClass.CANCELED, courseB),
                        controller.classesTable.getItems().getFirst());

                controller.courseField.setValue(courseA);
                date = new LocalDateStringConverter().fromString("16/11/2026");
                controller.dateFilter.setValue(date);
                controller.periodFilter.setValue(Period.MORNING);
                controller.search.fire();

                Assertions.assertEquals(1, controller.classesTable.getItems().size());
                Assertions.assertEquals(new ClassDto(1L, "2026-11-16", Period.MORNING, "08:00", "10:00", StatusClass.ACTIVE, courseA),
                        controller.classesTable.getItems().getFirst());
                controller.cleanFilter.fire();

                Assertions.assertEquals(5, controller.classesTable.getItems().size());
                List<ClassDto> classDtos = new ArrayList<ClassDto>(controller.classesTable.getItems());
                Assertions.assertEquals(classes, classDtos);

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
    void shouldCalculatePresencePercentage() throws Exception {
        CourseDto courseA = new CourseDto(10L, "Curso A", null, null, null, null, null);
        List<ClassDto> classes = List.of(
                new ClassDto(1L, "2026-11-16", Period.MORNING, "08:00", "10:00", StatusClass.ACTIVE, courseA),
                new ClassDto(2L, "2026-11-17", Period.AFTERNOON, "14:00", "16:00", StatusClass.ACTIVE, courseA)
        );
        PersonDto a = person(1L, "a");
        PersonDto b = person(2L, "b");
        PersonDto c = person(3L, "c");
        PersonDto d = person(4L, "d");

        CompletableFuture<List<PresenceDto>> firstClassPresences = new CompletableFuture<>();
        CompletableFuture<List<PresenceDto>> secondClassPresences = new CompletableFuture<>();

        RegisterService fakeRegisterService = new RegisterService() {
            @Override
            public CompletableFuture<List<CourseDto>> findCourses() {
                return CompletableFuture.completedFuture(List.of(courseA));
            }
        };
        ClassService fakeClassService = new ClassService() {
            @Override
            public CompletableFuture<List<ClassDto>> findAll() {
                return CompletableFuture.completedFuture(classes);
            }

            @Override
            public CompletableFuture<List<PresenceDto>> findAllPresences(Long courseId, Long courseClassId) {
                Assertions.assertEquals(courseA.id(), courseId);
                return switch (courseClassId.intValue()) {
                    case 1 -> firstClassPresences;
                    case 2 -> secondClassPresences;
                    default -> throw new AssertionError("Unexpected class: " + courseClassId);
                };
            }
        };
        ClassesController controller = new ClassesController(fakeRegisterService, fakeClassService);

        Stage stage = openStage("classes.fxml", controller);

        try {
            fx(() -> {
                Assertions.assertEquals(classes, new ArrayList<>(controller.classesTable.getItems()));
                firstClassPresences.complete(List.of(
                        new PresenceDto(1L, a, PresenceStatus.PRESENT)
                ));
                secondClassPresences.complete(List.of(
                        new PresenceDto(2L, a, PresenceStatus.PRESENT),
                        new PresenceDto(2L, b, PresenceStatus.ABSENT),
                        new PresenceDto(2L, c, PresenceStatus.JUSTIFIED),
                        new PresenceDto(2L, d, PresenceStatus.ABSENT)
                ));
                return null;
            });

            fx(() -> {
                String expected = new DecimalFormat("0.00").format(40.0) + "%";
                Assertions.assertEquals(expected, controller.presence.getText());
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
    void shouldHandleClassesWithoutPresences() throws Exception {
        CourseDto courseA = new CourseDto(10L, "Curso A", null, null, null, null, null);
        List<ClassDto> classes = List.of(
                new ClassDto(1L, "2026-11-16", Period.MORNING, "08:00", "10:00", StatusClass.ACTIVE, courseA),
                new ClassDto(2L, "2026-11-17", Period.AFTERNOON, "14:00", "16:00", StatusClass.ACTIVE, courseA)
        );
        PersonDto a = person(1L, "a");
        PersonDto b = person(2L, "b");
        PersonDto c = person(3L, "c");
        PersonDto d = person(4L, "d");

        RegisterService fakeRegisterService = new RegisterService() {
            @Override
            public CompletableFuture<List<CourseDto>> findCourses() {
                return CompletableFuture.completedFuture(List.of(courseA));
            }
        };
        ClassService fakeClassService = new ClassService() {
            @Override
            public CompletableFuture<List<ClassDto>> findAll() {
                return CompletableFuture.completedFuture(classes);
            }

            @Override
            public CompletableFuture<List<PresenceDto>> findAllPresences(Long courseId, Long courseClassId) {
                Assertions.assertEquals(courseA.id(), courseId);

                return CompletableFuture.completedFuture(List.of());
            }
        };
        ClassesController controller = new ClassesController(fakeRegisterService, fakeClassService);

        Stage stage = openStage("classes.fxml", controller);

        try {
            fx(() -> {
                return null;
            });
            fx(() -> {
                Assertions.assertEquals("-", controller.presence.getText());
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
    void shouldShowErrorWhenClassesOrPresencesFailToLoad() throws Exception {
        AtomicBoolean fail = new AtomicBoolean(true);

        RegisterService fakeRegisterService = new RegisterService() {
            @Override
            public CompletableFuture<List<RegisterDto>> findAll() {
                if (fail.get()) {
                    return CompletableFuture.failedFuture(
                            new RuntimeException("Falha simulada de pessoas")
                    );
                }
                return CompletableFuture.completedFuture(List.of());
            }

            @Override
            public CompletableFuture<List<CourseDto>> findCourses() {
                return CompletableFuture.failedFuture(new RuntimeException("Falha simulada de cursos"));
            }
        };

        ClassService fakeClassService = new ClassService() {
            @Override
            public CompletableFuture<List<ClassDto>> findAll() {
                return CompletableFuture.failedFuture(new RuntimeException("Falha simulada de aulas"));
            }

            @Override
            public CompletableFuture<List<PresenceDto>> findAllPresences(Long courseId, Long courseClassId) {
                if (!fail.get()) {
                    return CompletableFuture.failedFuture(new RuntimeException("Falha simulada de presenças"));
                }
                return CompletableFuture.completedFuture(List.of());
            }
        };

        ClassInfoController controllerInfo = new ClassInfoController(fakeRegisterService, fakeClassService);
        ClassesController controller = new ClassesController(fakeRegisterService, fakeClassService);

        Stage stage = openStage("classes.fxml", controller);

        try {
            fx(() -> {
                Label label = (Label) controller.classesTable.getPlaceholder();
                Assertions.assertEquals("Não foi possível carregar as aulas.", label.getText());
                return null;
            });
        } finally {
            fx(() -> {
                stage.close();
                return null;
            });
        }

        ClassDto classDto = new ClassDto(4L, "2026-01-28", Period.AFTERNOON, "14:00", "16:00", StatusClass.ACTIVE, course());

        Stage stageInfo = openStage("class-info.fxml", controllerInfo, () -> controllerInfo.setClass(classDto));

        try {
            fx(() -> {
                Assertions.assertEquals("Não foi possível carregar as pessoas.", controllerInfo.info.getText());

                return null;
            });

            fx(() -> {
                fail.set(false);
                controllerInfo.setClass(classDto);
                return null;
            });

            fx(() -> {
                Assertions.assertEquals("Não foi possível carregar as presenças.", controllerInfo.info.getText());
                return null;
            });
        } finally {
            fx(() -> {
                stageInfo.close();
                return null;
            });
        }
    }

}
