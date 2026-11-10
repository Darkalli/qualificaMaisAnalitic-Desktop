package com.qualificamaisanaliticinterface.features.courses;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseDtoTest {
    @Test
    void toStringUsesCourseName() {
        CourseDto course = new CourseDto(1L, "Curso de teste", null, null, null, null, null);
        assertEquals("Curso de teste", course.toString());
    }

    @Test
    void toStringHandlesMissingName() {
        CourseDto course = new CourseDto(1L, null, null, null, null, null, null);
        assertEquals("Curso sem nome", course.toString());
    }
}
