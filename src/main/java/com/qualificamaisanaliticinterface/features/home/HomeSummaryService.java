package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.features.classes.ClassService;
import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.classes.dto.PresenceDto;
import com.qualificamaisanaliticinterface.features.courses.CourseService;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.people.PersonService;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HomeSummaryService {
    private final CourseService courses;
    private final RegisterService registers;
    private final PersonService people;
    private final ClassService classes;

    public HomeSummaryService() {
        this(new CourseService(), new RegisterService(), new PersonService(), new ClassService());
    }

    HomeSummaryService(CourseService courses, RegisterService registers,
                       PersonService people, ClassService classes) {
        this.courses = courses;
        this.registers = registers;
        this.people = people;
        this.classes = classes;
    }

    public CompletableFuture<Long> activeCourses() {
        return courses.findAll().thenApply(items -> items.stream()
                .filter(course -> course.statusCourse() == StatusCourse.ACTIVE).count());
    }

    public CompletableFuture<Long> activeRegisters() {
        return registers.findAll().thenApply(items -> items.stream()
                .filter(register -> register.status() == StatusRegister.ACTIVE).count());
    }

    public CompletableFuture<Long> registeredPeople() {
        return people.findAll().thenApply(items -> (long) items.size());
    }

    public CompletableFuture<Long> todaysClasses(LocalDate today) {
        return findTodaysClasses(today).thenApply(items -> items.stream()
                .filter(item -> item.statusClass() == StatusClass.ACTIVE).count());
    }

    public CompletableFuture<List<ClassDto>> findTodaysClasses(LocalDate today) {
        return classes.findAll().thenApply(items -> items.stream()
                .filter(item -> today.toString().equals(item.day()))
                .sorted(Comparator.comparing(ClassDto::start, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ClassDto::id, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList());
    }

    public CompletableFuture<List<RegisterDto>> recentRegisters() {
        return loadRegisters().thenApply(items -> items.stream()
                .sorted(Comparator.comparing((RegisterDto item) -> registerDate(item.registerDate()),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RegisterDto::id, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList());
    }

    public CompletableFuture<List<ClassDto>> pendingPresences(LocalDateTime now) {
        return loadClasses().thenCompose(items -> {
            List<ClassDto> ended = items.stream()
                    .filter(item -> item.statusClass() == StatusClass.ACTIVE)
                    .filter(item -> item.id() != null && item.course() != null && item.course().id() != null)
                    .filter(item -> classEnd(item) != null && !classEnd(item).isAfter(now))
                    .sorted(Comparator.comparing(this::classEnd).thenComparing(ClassDto::id))
                    .toList();
            CompletableFuture<List<ClassDto>> result = CompletableFuture.completedFuture(new ArrayList<>());
            for (int offset = 0; offset < ended.size(); offset += 4) {
                List<ClassDto> batch = ended.subList(offset, Math.min(offset + 4, ended.size()));
                result = result.thenCompose(pending -> {
                    var checks = batch.stream().map(item -> loadPresences(item)
                            .thenApply(presences -> presences.stream().noneMatch(presence -> presence.status() != null)
                                    ? item : null)).toList();
                    return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new))
                            .thenApply(ignored -> {
                                checks.stream().map(CompletableFuture::join)
                                        .filter(item -> item != null).forEach(pending::add);
                                return pending;
                            });
                });
            }
            return result.thenApply(List::copyOf);
        });
    }

    CompletableFuture<List<RegisterDto>> loadRegisters() {
        return registers.findAll();
    }

    CompletableFuture<List<ClassDto>> loadClasses() {
        return classes.findAll();
    }

    CompletableFuture<List<PresenceDto>> loadPresences(ClassDto courseClass) {
        return classes.findAllPresences(courseClass.course().id(), courseClass.id());
    }

    private LocalDate registerDate(String value) {
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private LocalDateTime classEnd(ClassDto item) {
        if (item.day() == null || item.finish() == null) {
            return null;
        }
        try {
            LocalTime finish = LocalTime.parse(item.finish());
            LocalDateTime end = LocalDate.parse(item.day()).atTime(finish);
            if (item.start() != null && finish.isBefore(LocalTime.parse(item.start()))) {
                end = end.plusDays(1);
            }
            return end;
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
