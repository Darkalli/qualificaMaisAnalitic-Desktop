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
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

        Stage stage = fx(() -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/classes/class-info.fxml")
            );
            loader.setControllerFactory(type -> controller);

            DialogPane pane = loader.load();
            Stage window = new Stage();
            window.setScene(new Scene(pane));
            window.show();


            controller.setClass(classDto);
            return window;
        });

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

        Stage stage = fx(() -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/classes/class-info.fxml")
            );
            loader.setControllerFactory(type -> controller);

            DialogPane pane = loader.load();
            Stage window = new Stage();
            window.setScene(new Scene(pane));
            window.show();


            controller.setClass(classDto);
            return window;
        });

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

        Stage stage = fx(() -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/classes/class-info.fxml")
            );
            loader.setControllerFactory(type -> controller);

            DialogPane pane = loader.load();
            Stage window = new Stage();
            window.setScene(new Scene(pane));
            window.show();


            controller.setClass(classDto);
            return window;
        });

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
        Stage stage = fx(() -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/classes/class-info.fxml")
            );
            loader.setControllerFactory(type -> controller);

            DialogPane pane = loader.load();
            Stage window = new Stage();
            window.setScene(new Scene(pane));
            window.show();


            controller.setClass(classDto);
            return window;
        });

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
