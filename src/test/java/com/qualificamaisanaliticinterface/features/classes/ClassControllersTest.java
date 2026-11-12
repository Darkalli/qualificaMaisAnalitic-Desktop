package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.classes.dto.PresenceDto;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.PresenceStatus;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.converter.LocalDateStringConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.qualificamaisanaliticinterface.features.classes.ClassTestSupport.*;

public class ClassControllersTest {
    @BeforeAll
    static void startJavaFx() {
        startFx();
    }

    @Test
    void shouldLoadOnlyPeopleWithActiveRegistrationForCourse() {
        CourseDto courseA = new CourseDto(10L, "Curso A", null, null, null, null, null);
        CourseDto courseB = new CourseDto(20L, "Curso B", null, null, null, null, null);

        PersonDto a = person(1L, "a");
        PersonDto b = person(2L, "b");
        PersonDto c = person(3L, "c");
        PersonDto d = person(4L, "d");

        List<RegisterDto> registers = List.of(
                new RegisterDto(1L, a, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(2L, b, courseA, null, StatusRegister.CANCELED),
                new RegisterDto(3L, c, courseB, null, StatusRegister.ACTIVE),
                new RegisterDto(4L, d, courseA, null, StatusRegister.ACTIVE)
        );
        RegisterService fakeService = new RegisterService() {
            @Override
            public CompletableFuture<List<RegisterDto>> findAll() {
                return CompletableFuture.completedFuture(registers);
            }
        };
        ClassInfoController controller = new ClassInfoController(fakeService, new ClassService());
        Assertions.assertEquals(List.of(a, d), controller.getPeople(10L).join());
    }

    @Test
    void shouldRestorePreviouslySavedPresences() throws Exception {
        CourseDto courseA = new CourseDto(10L, "Curso A", null, null, null, null, null);

        PersonDto a = person(1L, "a");
        PersonDto b = person(2L, "b");
        PersonDto c = person(3L, "c");
        PersonDto d = person(4L, "d");

        List<RegisterDto> registers = List.of(
                new RegisterDto(1L, a, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(2L, b, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(3L, c, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(4L, d, courseA, null, StatusRegister.ACTIVE)
        );

        ClassDto classDto = new ClassDto(4L, "2026-01-28", Period.AFTERNOON, "20:00", "22:30", StatusClass.ACTIVE, courseA);

        List<PresenceDto> presences = List.of(
                new PresenceDto(4L, a, PresenceStatus.PRESENT),
                new PresenceDto(4L, b, PresenceStatus.ABSENT),
                new PresenceDto(4L, c, PresenceStatus.PRESENT)
        );

        RegisterService fakeService = new RegisterService() {
            @Override
            public CompletableFuture<List<RegisterDto>> findAll() {
                return CompletableFuture.completedFuture(registers);
            }
        };

        ClassService fakeClassService = new ClassService() {
            @Override
            public CompletableFuture<List<PresenceDto>> findAllPresences(Long courseId, Long courseClassId) {
                return CompletableFuture.completedFuture(presences);
            }
        };
        ClassInfoController controller = new ClassInfoController(fakeService, fakeClassService);

        Stage stage = openStage("class-info.fxml", controller,
                () -> controller.setClass(classDto));

        try {
            fx(() -> {
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                Assertions.assertEquals(
                        List.of(a, b, c, d),
                        controller.peopleTable.getItems()
                );
                Assertions.assertFalse(controller.peopleTable.isDisabled());

                assertPresence(controller.peopleTable, a, PresenceStatus.PRESENT);
                assertPresence(controller.peopleTable, b, PresenceStatus.ABSENT);
                assertPresence(controller.peopleTable, c, PresenceStatus.PRESENT);
                assertPresence(controller.peopleTable, d, null);

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
    void shouldRequireAtLeastOnePresenceBeforeSaving() throws Exception {
        CourseDto courseA = new CourseDto(10L, "Curso A", null, null, null, null, null);

        PersonDto a = person(1L, "a");
        PersonDto b = person(2L, "b");
        PersonDto c = person(3L, "c");
        PersonDto d = person(4L, "d");

        List<RegisterDto> registers = List.of(
                new RegisterDto(1L, a, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(2L, b, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(3L, c, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(4L, d, courseA, null, StatusRegister.ACTIVE)
        );

        ClassDto classDto = new ClassDto(4L, "2026-01-28", Period.AFTERNOON, "20:00", "22:30", StatusClass.ACTIVE, courseA);

        RegisterService fakeService = new RegisterService() {
            @Override
            public CompletableFuture<List<RegisterDto>> findAll() {
                return CompletableFuture.completedFuture(registers);
            }
        };
        ClassService fakeClassService = new ClassService() {
            @Override
            public CompletableFuture<List<PresenceDto>> findAllPresences(Long courseId, Long courseClassId) {
                return CompletableFuture.completedFuture(List.of());
            }
        };

        ClassInfoController controller = new ClassInfoController(fakeService, fakeClassService);

        Stage stage = openStage("class-info.fxml", controller,
                () -> controller.setClass(classDto));

        try {
            fx(() -> {
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();

                controller.confirm.fire();
                Assertions.assertEquals("Marque pelo menos uma presença", controller.info.getText());
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
    void shouldAddNewPresencesAndUpdateChangedOnesAndSkipUnchangedPresences() throws Exception {
        CourseDto courseA = new CourseDto(10L, "Curso A", null, null, null, null, null);

        PersonDto a = person(1L, "a");
        PersonDto b = person(2L, "b");
        PersonDto c = person(3L, "c");
        PersonDto d = person(4L, "d");

        List<RegisterDto> registers = List.of(
                new RegisterDto(1L, a, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(2L, b, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(3L, c, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(4L, d, courseA, null, StatusRegister.ACTIVE)
        );

        ClassDto classDto = new ClassDto(4L, "2026-01-28", Period.AFTERNOON, "20:00", "22:30", StatusClass.ACTIVE, courseA);

        List<PresenceDto> presences = new ArrayList<>(List.of(
                new PresenceDto(4L, a, PresenceStatus.PRESENT),
                new PresenceDto(4L, b, PresenceStatus.ABSENT),
                new PresenceDto(4L, c, PresenceStatus.PRESENT)
        ));

        RegisterService fakeService = new RegisterService() {
            @Override
            public CompletableFuture<List<RegisterDto>> findAll() {
                return CompletableFuture.completedFuture(registers);
            }
        };

        Map<Long, PresenceStatus> addedPresences = new HashMap<>();
        Map<Long, PresenceStatus> updatedPresences = new HashMap<>();
        ClassService fakeClassService = new ClassService() {
            @Override
            public CompletableFuture<List<PresenceDto>> findAllPresences(Long courseId, Long courseClassId) {
                return CompletableFuture.completedFuture(presences);
            }

            @Override
            public CompletableFuture<String> addPresence(Long personId, Long courseClassId, PresenceStatus status) {
                Assertions.assertEquals(classDto.id(), courseClassId);
                addedPresences.put(personId, status);
                return CompletableFuture.completedFuture("sucesso");
            }

            @Override
            public CompletableFuture<String> updatePresence(Long personId, Long courseClassId, PresenceStatus status) {
                Assertions.assertEquals(classDto.id(), courseClassId);
                updatedPresences.put(personId, status);
                return CompletableFuture.completedFuture("sucesso");
            }
        };

        ClassInfoController controller = new ClassInfoController(fakeService, fakeClassService);

        Stage stage = openStage("class-info.fxml", controller,
                () -> controller.setClass(classDto));

        try {
            fx(() -> {
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();

                Assertions.assertEquals(
                        List.of(a, b, c, d),
                        controller.peopleTable.getItems()
                );
                Assertions.assertFalse(controller.peopleTable.isDisabled());

                assertPresence(controller.peopleTable, a, PresenceStatus.PRESENT);
                assertPresence(controller.peopleTable, b, PresenceStatus.ABSENT);
                assertPresence(controller.peopleTable, c, PresenceStatus.PRESENT);
                assertPresence(controller.peopleTable, d, null);

                Assertions.assertFalse(controller.peopleTable.isDisabled());
                TableRow<?> rowB = getTableRow(b, controller.peopleTable);
                TableRow<?> rowC = getTableRow(c, controller.peopleTable);
                TableRow<?> rowD = getTableRow(d, controller.peopleTable);

                RadioButton radioB = (RadioButton) rowB.lookup("#" + "justified");
                RadioButton radioC = (RadioButton) rowC.lookup("#" + "absent");
                RadioButton radioD = (RadioButton) rowD.lookup("#" + "present");
                radioB.fire();
                radioC.fire();
                radioD.fire();
                controller.confirm.fire();
                Assertions.assertEquals(Map.of(4L, PresenceStatus.PRESENT), addedPresences);
                Assertions.assertEquals(Map.of(2L, PresenceStatus.JUSTIFIED, 3L, PresenceStatus.ABSENT), updatedPresences);
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
    void shouldKeepPendingChoicesAfterPartialFailure() throws Exception {
        CourseDto courseA = new CourseDto(10L, "Curso A", null, null, null, null, null);

        PersonDto a = person(1L, "a");
        PersonDto b = person(2L, "b");
        PersonDto c = person(3L, "c");

        List<RegisterDto> registers = List.of(
                new RegisterDto(1L, a, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(2L, b, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(3L, c, courseA, null, StatusRegister.ACTIVE)
        );

        ClassDto classDto = new ClassDto(4L, "2026-01-28", Period.AFTERNOON, "20:00", "22:30", StatusClass.ACTIVE, courseA);

        List<PresenceDto> presences = new ArrayList<>(List.of());

        RegisterService fakeService = new RegisterService() {
            @Override
            public CompletableFuture<List<RegisterDto>> findAll() {
                return CompletableFuture.completedFuture(registers);
            }
        };
        Map<Long, PresenceStatus> addedPresences = new HashMap<>();
        Map<Long, Integer> attempts = new HashMap<>();
        List<Long> failedIds = new ArrayList<>();
        ClassService fakeClassService = new ClassService() {
            private int calls = 0;

            @Override
            public CompletableFuture<List<PresenceDto>> findAllPresences(Long courseId, Long courseClassId) {
                return CompletableFuture.completedFuture(presences);
            }

            @Override
            public CompletableFuture<String> addPresence(Long personId, Long courseClassId, PresenceStatus status) {
                Assertions.assertEquals(classDto.id(), courseClassId);
                calls++;
                attempts.merge(personId, 1, Integer::sum);
                if (calls == 2) {
                    failedIds.add(personId);
                    return CompletableFuture.failedFuture(new RuntimeException("Falha simulada"));
                }
                addedPresences.put(personId, status);
                return CompletableFuture.completedFuture("sucesso");
            }
        };

        ClassInfoController controller = new ClassInfoController(fakeService, fakeClassService);
        Stage stage = openStage("class-info.fxml", controller,
                () -> controller.setClass(classDto));

        try {
            fx(() -> {
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();

                Assertions.assertEquals(List.of(a, b, c), controller.peopleTable.getItems());
                Assertions.assertFalse(controller.peopleTable.isDisabled());

                TableRow<?> rowA = getTableRow(a, controller.peopleTable);
                TableRow<?> rowB = getTableRow(b, controller.peopleTable);
                TableRow<?> rowC = getTableRow(c, controller.peopleTable);

                ((RadioButton) rowA.lookup("#present")).fire();
                ((RadioButton) rowB.lookup("#absent")).fire();
                ((RadioButton) rowC.lookup("#present")).fire();
                controller.confirm.fire();
                return null;
            });
            fx(() -> {
                Assertions.assertEquals(1, addedPresences.size());
                Assertions.assertEquals(1, failedIds.size());
                Assertions.assertEquals(2, attempts.values().stream().mapToInt(Integer::intValue).sum());

                Assertions.assertTrue(stage.isShowing());
                Assertions.assertFalse(
                        controller.infoDialogPane.getContent().isDisabled()
                );

                Assertions.assertTrue(
                        controller.info.getText().contains("Falha simulada")
                );
                Assertions.assertTrue(
                        controller.info.getText().contains("Envios confirmados: 1")
                );

                controller.peopleTable.applyCss();
                controller.peopleTable.layout();

                assertPresence(controller.peopleTable, a, PresenceStatus.PRESENT);
                assertPresence(controller.peopleTable, b, PresenceStatus.ABSENT);
                assertPresence(controller.peopleTable, c, PresenceStatus.PRESENT);
                controller.confirm.fire();

                return null;
            });
            fx(() -> {
                Assertions.assertEquals(
                        Map.of(a.id(), PresenceStatus.PRESENT,
                                b.id(), PresenceStatus.ABSENT,
                                c.id(), PresenceStatus.PRESENT),
                        addedPresences
                );

                Map<Long, Integer> expectedAttempts = new HashMap<>(
                        Map.of(a.id(), 1, b.id(), 1, c.id(), 1)
                );
                expectedAttempts.put(failedIds.getFirst(), 2);
                Assertions.assertEquals(expectedAttempts, attempts);
                Assertions.assertFalse(stage.isShowing());

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
    void shouldRemovePresenceWhenSelectedOptionIsClickedAgain() throws Exception {
        CourseDto courseA = new CourseDto(10L, "Curso A", null, null, null, null, null);

        PersonDto a = person(1L, "a");
        PersonDto b = person(2L, "b");
        PersonDto c = person(3L, "c");

        List<RegisterDto> registers = List.of(
                new RegisterDto(1L, a, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(2L, b, courseA, null, StatusRegister.ACTIVE),
                new RegisterDto(3L, c, courseA, null, StatusRegister.ACTIVE)
        );

        ClassDto classDto = new ClassDto(4L, "2026-01-28", Period.AFTERNOON, "20:00", "22:30", StatusClass.ACTIVE, courseA);

        RegisterService fakeService = new RegisterService() {
            @Override
            public CompletableFuture<List<RegisterDto>> findAll() {
                return CompletableFuture.completedFuture(registers);
            }
        };

        ClassService fakeClassService = new ClassService() {
            @Override
            public CompletableFuture<List<PresenceDto>> findAllPresences(Long courseId, Long courseClassId) {
                return CompletableFuture.completedFuture(List.of());
            }
        };
        ClassInfoController controller = new ClassInfoController(fakeService, fakeClassService);

        Stage stage = openStage("class-info.fxml", controller,
                () -> controller.setClass(classDto));

        try {
            fx(() -> {
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();

                Assertions.assertFalse(controller.peopleTable.isDisabled());

                assertPresence(controller.peopleTable, a, null);

                TableRow<?> row = getTableRow(a, controller.peopleTable);

                RadioButton radio = (RadioButton) row.lookup("#justified");

                radio.fire();
                Assertions.assertTrue(radio.isSelected());
                assertPresence(controller.peopleTable, a, PresenceStatus.JUSTIFIED);

                radio.fireEvent(new MouseEvent(
                        MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0,
                        MouseButton.PRIMARY, 1,
                        false, false, false, false,
                        true, false, false, false, false, true, null
                ));
                Assertions.assertFalse(radio.isSelected());
                Assertions.assertFalse(radio.isDisabled());
                assertPresence(controller.peopleTable, a, null);

                controller.peopleTable.refresh();
                controller.peopleTable.applyCss();
                controller.peopleTable.layout();
                assertPresence(controller.peopleTable, a, null);

                controller.confirm.fire();
                Assertions.assertEquals("Marque pelo menos uma presença", controller.info.getText());
                Assertions.assertTrue(stage.isShowing());
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
            public CompletableFuture<String> addClass(
                    String day, Period period, String start,
                    String finish, StatusClass status, Long courseId
            ) {
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
                classes.put(id,new ClassDto(id++, day, period, start, finish, status, course));
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

            ClassFormController controller =
                    new ClassFormController(fakeRegisterService, fakeClassService);

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

    @Test
    void shouldCreateClassesForSelectedWeekdaysWithinDateRange(){

    }

    private static void assertPresence(TableView<PersonDto> table, PersonDto person, PresenceStatus presence) {

        table.scrollTo(person);
        table.applyCss();
        table.layout();

        TableRow<?> row = table.lookupAll(".table-row-cell").stream()
                .filter(node -> node instanceof TableRow<?> candidate
                        && person.equals(candidate.getItem()))
                .map(node -> (TableRow<?>) node)
                .findFirst()
                .orElseThrow();

        for (String id : List.of("present", "absent", "justified")) {
            RadioButton radio = (RadioButton) row.lookup("#" + id);
            Assertions.assertNotNull(radio);
            Assertions.assertEquals(presence == radio.getUserData(), radio.isSelected());
        }
    }

    private static TableRow<?> getTableRow(PersonDto dto, TableView<?> table) {
        return table.lookupAll(".table-row-cell").stream()
                .filter(node -> node instanceof TableRow<?> candidate
                        && dto.equals(candidate.getItem()))
                .map(node -> (TableRow<?>) node)
                .findFirst()
                .orElseThrow();
    }
}
