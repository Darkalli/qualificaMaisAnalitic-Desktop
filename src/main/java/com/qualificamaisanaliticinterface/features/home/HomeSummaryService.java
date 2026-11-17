package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.features.classes.ClassService;
import com.qualificamaisanaliticinterface.features.courses.CourseService;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.people.PersonService;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;

import java.time.LocalDate;
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
        return classes.findAll().thenApply(items -> items.stream()
                .filter(item -> item.statusClass() == StatusClass.ACTIVE)
                .filter(item -> today.toString().equals(item.day())).count());
    }
}
