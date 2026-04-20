package com.sms.service;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class StudentFieldDerivationUtilsTest {

    @Test
    void infersGenderFromNameWhenNoExplicitValueIsProvided() {
        assertEquals("Female", StudentFieldDerivationUtils.inferGender("Bhavya Jain", null));
    }

    @Test
    void derivesPassingYearFromCourseDuration() {
        assertEquals(2029, StudentFieldDerivationUtils.derivePassingYear(
            "Bachelor of Technology (Computer Science and Engineering)",
            2025,
            null
        ));
    }

    @Test
    void resolvesCollegeNameAndHouseSeparately() {
        assertEquals("Bennett University", StudentFieldDerivationUtils.resolveCollegeName("B.Tech", "B.Tech"));
        assertNull(StudentFieldDerivationUtils.resolveHouse(null, "Cedar"));
    }

    @Test
    void derivesValidUptoFromPassingYearWhenExplicitDateIsMissing() {
        assertEquals(LocalDate.of(2029, 6, 30), StudentFieldDerivationUtils.deriveValidUpto(
            "Bachelor of Technology (Computer Science and Engineering)",
            2025,
            2029,
            null
        ));
    }
}