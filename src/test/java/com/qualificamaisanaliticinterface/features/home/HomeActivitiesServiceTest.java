package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.classes.dto.PresenceDto;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.PresenceStatus;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.LongStream;

class HomeActivitiesServiceTest {
    private final CourseDto course = new CourseDto(10L, "Curso A", null, null, null, null, null);
    private final LocalDateTime now = LocalDateTime.of(2026, 11, 18, 12, 0);

    @Test
    void shouldReturnFiveLatestRegistrationsByDateAndIdIncludingCanceled() {
        FakeService service = new FakeService();
        service.registers = List.of(
                register(1L, "2026-11-16", StatusRegister.ACTIVE),
                register(2L, "2026-11-18", StatusRegister.CANCELED),
                register(3L, "2026-11-18", StatusRegister.ACTIVE),
                register(4L, "2026-11-17", StatusRegister.ACTIVE),
                register(5L, "2026-11-15", StatusRegister.ACTIVE),
                register(6L, "2026-11-14", StatusRegister.ACTIVE),
                register(7L, null, StatusRegister.ACTIVE),
                register(8L, "invalid", StatusRegister.ACTIVE));

        Assertions.assertEquals(List.of(3L, 2L, 4L, 1L, 5L),
                service.recentRegisters().join().stream().map(RegisterDto::id).toList());
    }

    @Test
    void shouldReturnEmptyListsWithoutCheckingPresencesWhenThereAreNoRecords() {
        FakeService service = new FakeService();

        Assertions.assertTrue(service.recentRegisters().join().isEmpty());
        Assertions.assertTrue(service.pendingPresences(now).join().isEmpty());
        Assertions.assertTrue(service.checked.isEmpty());
    }

    @Test
    void shouldOnlyCheckEndedActiveClassesAndOrderPendingFromOldest() {
        FakeService service = new FakeService();
        service.classes = List.of(
                lesson(7L, "2026-11-18", "10:00", "12:00", StatusClass.ACTIVE),
                lesson(2L, "2026-11-18", "09:00", "10:00", StatusClass.ACTIVE),
                lesson(1L, "2026-11-17", "08:00", "09:00", StatusClass.ACTIVE),
                lesson(3L, "2026-11-18", "11:00", "13:00", StatusClass.ACTIVE),
                lesson(4L, "2026-11-19", "08:00", "09:00", StatusClass.ACTIVE),
                lesson(5L, "2026-11-17", "08:00", "09:00", StatusClass.CANCELED),
                lesson(6L, "2026-11-17", "08:00", "09:00", StatusClass.POSTPONED),
                lesson(8L, "2026-11-17", "23:00", "01:00", StatusClass.ACTIVE),
                lesson(9L, "2026-11-18", "23:00", "01:00", StatusClass.ACTIVE),
                lesson(10L, "2026-11-17", "08:00", null, StatusClass.ACTIVE),
                lesson(11L, "invalid", "08:00", "09:00", StatusClass.ACTIVE),
                new ClassDto(12L, "2026-11-17", null, "08:00", "09:00", StatusClass.ACTIVE, null));
        service.presences.put(2L, CompletableFuture.completedFuture(List.of(new PresenceDto(1L, null, PresenceStatus.ABSENT))));

        var pending = service.pendingPresences(now).join();

        Assertions.assertEquals(List.of(1L, 8L, 7L), pending.stream().map(ClassDto::id).toList());
        Assertions.assertEquals(List.of(1L, 8L, 2L, 7L), service.checked);
    }

    @Test
    void shouldRecognizePresentAbsentAndJustifiedRecordsAsFilledCalls() {
        FakeService service = new FakeService();
        service.classes = LongStream.rangeClosed(1, 4)
                .mapToObj(id -> lesson(id, "2026-11-17", "08:00", "09:00", StatusClass.ACTIVE)).toList();
        service.presences.put(1L, CompletableFuture.completedFuture(List.of(new PresenceDto(1L, null, PresenceStatus.PRESENT))));
        service.presences.put(2L, CompletableFuture.completedFuture(List.of(new PresenceDto(2L, null, PresenceStatus.ABSENT))));
        service.presences.put(3L, CompletableFuture.completedFuture(List.of(new PresenceDto(3L, null, PresenceStatus.JUSTIFIED))));
        service.presences.put(4L, CompletableFuture.completedFuture(List.of(new PresenceDto(4L, null, null))));

        Assertions.assertEquals(List.of(4L), service.pendingPresences(now).join().stream().map(ClassDto::id).toList());
    }

    @Test
    void shouldFailVerificationWhenPresencesCannotBeLoadedInsteadOfReportingAnEmptyCall() {
        FakeService service = new FakeService();
        service.classes = List.of(lesson(1L, "2026-11-17", "08:00", "09:00", StatusClass.ACTIVE));
        service.presences.put(1L, CompletableFuture.failedFuture(new IOException("offline")));

        var failure = Assertions.assertThrows(CompletionException.class, () -> service.pendingPresences(now).join());

        Assertions.assertInstanceOf(IOException.class, failure.getCause());
    }

    @Test
    void shouldCheckAtMostFourClassesAtOnceWithoutDroppingOlderPendingClasses() {
        FakeService service = new FakeService();
        service.classes = LongStream.rangeClosed(1, 9)
                .mapToObj(id -> lesson(id, "2026-11-17", "08:00", "09:00", StatusClass.ACTIVE)).toList();
        service.classes.forEach(item -> service.presences.put(item.id(), new CompletableFuture<>()));

        var pending = service.pendingPresences(now);
        Assertions.assertEquals(List.of(1L, 2L, 3L, 4L), service.checked);
        Assertions.assertFalse(pending.isDone());
        for (long id = 1; id <= 4; id++) {
            service.presences.get(id).complete(List.of());
        }
        Assertions.assertEquals(8, service.checked.size());
        for (long id = 5; id <= 8; id++) {
            service.presences.get(id).complete(List.of());
        }
        Assertions.assertEquals(9, service.checked.size());
        service.presences.get(9L).complete(List.of());
        Assertions.assertEquals(9, pending.join().size());
    }

    private RegisterDto register(Long id, String date, StatusRegister status) {
        return new RegisterDto(id, null, course, date, status);
    }

    private ClassDto lesson(Long id, String day, String start, String finish, StatusClass status) {
        return new ClassDto(id, day, null, start, finish, status, course);
    }

    private static class FakeService extends HomeSummaryService {
        List<RegisterDto> registers = List.of();
        List<ClassDto> classes = List.of();
        final Map<Long, CompletableFuture<List<PresenceDto>>> presences = new HashMap<>();
        final List<Long> checked = new ArrayList<>();

        FakeService() {
            super(null, null, null, null);
        }

        @Override
        CompletableFuture<List<RegisterDto>> loadRegisters() {
            return CompletableFuture.completedFuture(registers);
        }

        @Override
        CompletableFuture<List<ClassDto>> loadClasses() {
            return CompletableFuture.completedFuture(classes);
        }

        @Override
        CompletableFuture<List<PresenceDto>> loadPresences(ClassDto courseClass) {
            checked.add(courseClass.id());
            return presences.getOrDefault(courseClass.id(), CompletableFuture.completedFuture(List.of()));
        }
    }
}
