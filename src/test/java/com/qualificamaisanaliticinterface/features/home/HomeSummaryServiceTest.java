package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.features.classes.ClassService;
import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.courses.CourseService;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.people.PersonService;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class HomeSummaryServiceTest {
    @Test
    void shouldCountActiveRecordsAndOnlyActiveClassesOnRequestedDay() {
        CourseDto active = new CourseDto(1L, "Ativo", null, null, null, StatusCourse.ACTIVE, null);
        CourseDto closed = new CourseDto(2L, "Encerrado", null, null, null, StatusCourse.CLOSED, null);
        CourseDto canceled = new CourseDto(3L, "Cancelado", null, null, null, StatusCourse.CANCELED, null);
        CourseService courses = new CourseService() {
            @Override
            public CompletableFuture<List<CourseDto>> findAll() {
                return CompletableFuture.completedFuture(List.of(active, closed, canceled));
            }
        };
        RegisterService registers = new RegisterService() {
            @Override
            public CompletableFuture<List<RegisterDto>> findAll() {
                return CompletableFuture.completedFuture(List.of(
                        new RegisterDto(1L, null, active, null, StatusRegister.ACTIVE),
                        new RegisterDto(2L, null, active, null, StatusRegister.CANCELED)));
            }
        };
        PersonService people = new PersonService() {
            @Override
            public CompletableFuture<List<PersonDto>> findAll() {
                return CompletableFuture.completedFuture(List.of());
            }
        };
        ClassService classes = new ClassService() {
            @Override
            public CompletableFuture<List<ClassDto>> findAll() {
                return CompletableFuture.completedFuture(List.of(
                        new ClassDto(1L, "2026-11-17", null, null, null, StatusClass.ACTIVE, active),
                        new ClassDto(2L, "2026-11-17", null, null, null, StatusClass.CANCELED, active),
                        new ClassDto(3L, "2026-11-17", null, null, null, StatusClass.POSTPONED, active),
                        new ClassDto(4L, "2026-11-18", null, null, null, StatusClass.ACTIVE, active)));
            }
        };
        HomeSummaryService service = new HomeSummaryService(courses, registers, people, classes);

        Assertions.assertEquals(1L, service.activeCourses().join());
        Assertions.assertEquals(1L, service.activeRegisters().join());
        Assertions.assertEquals(0L, service.registeredPeople().join());
        Assertions.assertEquals(1L, service.todaysClasses(LocalDate.of(2026, 11, 17)).join());
    }
}
