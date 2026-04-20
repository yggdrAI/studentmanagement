package com.sms.service;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

public final class StudentFieldDerivationUtils {

    private static final String DEFAULT_COLLEGE = "Bennett University";

    private static final Set<String> MALE_NAMES = Set.of(
        "aarav", "aakash", "aditya", "amit", "anand", "ankit", "arjun", "arnav",
        "ashish", "ashok", "atul", "bhavesh", "chirag", "deepak", "gautam",
        "gopal", "hari", "harish", "imran", "jai", "karthik", "kunal", "manish",
        "mohit", "naveen", "nikhil", "nitin", "om", "parth", "piyush", "rahul",
        "rajesh", "rakesh", "rohan", "sachin", "sameer", "sanjay", "shubham",
        "sumit", "sunil", "tarun", "varun", "vishal", "yash"
    );

    private static final Set<String> FEMALE_NAMES = Set.of(
        "aanya", "aarti", "aditi", "ananya", "anjali", "bhavya", "deepa", "diya",
        "gauri", "isha", "jyoti", "kavya", "kriti", "laxmi", "meera", "neha",
        "neelam", "nidhi", "nisha", "pooja", "priya", "rashi", "rhea", "ritika",
        "sakshi", "shruti", "simran", "sonali", "swati", "tanya", "tanvi", "twinkle",
        "urvashi", "vani", "yamini", "zoya"
    );

    private StudentFieldDerivationUtils() {
    }

    public static String inferGender(String fullName, String explicitGender) {
        String normalizedExplicit = normalizeGender(explicitGender);
        if (hasText(normalizedExplicit)) {
            return normalizedExplicit;
        }

        String firstName = normalizeToken(firstToken(fullName));
        if (!hasText(firstName)) {
            return null;
        }

        if (MALE_NAMES.contains(firstName)) {
            return "Male";
        }
        if (FEMALE_NAMES.contains(firstName)) {
            return "Female";
        }

        if (firstName.endsWith("sha") || firstName.endsWith("iya") || firstName.endsWith("ita") || firstName.endsWith("ini")) {
            return "Female";
        }
        if (firstName.endsWith("an") || firstName.endsWith("esh") || firstName.endsWith("sh") || firstName.endsWith("it")) {
            return "Male";
        }

        return "Other";
    }

    public static String resolveCollegeName(String college, String course) {
        return DEFAULT_COLLEGE;
    }

    public static String resolveHouse(String house, String legacyFoundationClassroom) {
        return clean(house);
    }

    public static Integer derivePassingYear(String course, Integer admissionYear, Integer existingPassingYear) {
        Integer duration = resolveCourseDuration(course);
        if (admissionYear != null && duration != null) {
            return admissionYear + duration;
        }
        if (existingPassingYear != null) {
            return existingPassingYear;
        }
        if (admissionYear != null) {
            return admissionYear + (duration != null ? duration : 4);
        }
        return null;
    }

    public static LocalDate deriveValidUpto(String course, Integer admissionYear, Integer passingYear, LocalDate existingValidUpto) {
        if (existingValidUpto != null) {
            return existingValidUpto;
        }

        Integer resolvedPassingYear = derivePassingYear(course, admissionYear, passingYear);
        if (resolvedPassingYear != null) {
            return LocalDate.of(resolvedPassingYear, 6, 30);
        }

        Integer duration = resolveCourseDuration(course);
        if (admissionYear != null) {
            return LocalDate.of(admissionYear + (duration != null ? duration : 4), 6, 30);
        }

        return LocalDate.now().plusYears(duration != null ? duration : 4);
    }

    public static Integer resolveCourseDuration(String course) {
        String normalizedCourse = normalizeCourse(course);
        if (!hasText(normalizedCourse)) {
            return null;
        }

        if (normalizedCourse.contains("bachelor of technology") || normalizedCourse.contains("b tech") || normalizedCourse.contains("engineering")) {
            return 4;
        }
        if (normalizedCourse.contains("master of technology") || normalizedCourse.contains("m tech") || normalizedCourse.contains("mtech")) {
            return 2;
        }
        if (normalizedCourse.contains("bachelor of business administration") || normalizedCourse.contains("bba")) {
            return 3;
        }
        if (normalizedCourse.contains("master of business administration") || normalizedCourse.contains("mba")) {
            return 2;
        }
        if (normalizedCourse.contains("bachelor of computer applications") || normalizedCourse.contains("bca")) {
            return 3;
        }
        if (normalizedCourse.contains("master of computer applications") || normalizedCourse.contains("mca")) {
            return 2;
        }
        if (normalizedCourse.contains("bachelor of commerce") || normalizedCourse.contains("b com") || normalizedCourse.contains("commerce")) {
            return 3;
        }
        if (normalizedCourse.contains("bachelor of arts") || normalizedCourse.startsWith("ba ") || normalizedCourse.endsWith(" ba") || normalizedCourse.contains(" arts")) {
            return 3;
        }

        return null;
    }

    private static String normalizeGender(String gender) {
        if (!hasText(gender)) {
            return null;
        }

        String normalized = clean(gender).toLowerCase(Locale.ROOT);
        if (normalized.equals("m") || normalized.equals("male") || normalized.equals("man") || normalized.equals("boy")) {
            return "Male";
        }
        if (normalized.equals("f") || normalized.equals("female") || normalized.equals("woman") || normalized.equals("girl")) {
            return "Female";
        }
        if (normalized.equals("other") || normalized.equals("non-binary") || normalized.equals("nonbinary") || normalized.equals("prefer not to say")) {
            return "Other";
        }
        return clean(gender);
    }

    private static String normalizeCourse(String course) {
        if (!hasText(course)) {
            return null;
        }
        return clean(course).toLowerCase(Locale.ROOT)
            .replace('&', ' ')
            .replaceAll("[^a-z0-9]+", " ")
            .trim()
            .replaceAll("\\s+", " ");
    }

    private static boolean looksLikeCourse(String value) {
        String normalized = normalizeCourse(value);
        if (!hasText(normalized)) {
            return false;
        }
        return normalized.contains("b tech")
            || normalized.contains("m tech")
            || normalized.contains("bachelor of")
            || normalized.contains("master of")
            || normalized.contains("engineering")
            || normalized.contains("computer applications")
            || normalized.contains("business administration")
            || normalized.contains("commerce")
            || normalized.contains("arts");
    }

    private static boolean isTooGenericCollege(String value) {
        String normalized = clean(value).toLowerCase(Locale.ROOT);
        return normalized.contains("course")
            || normalized.contains("branch")
            || normalized.contains("program")
            || normalized.contains("b tech")
            || normalized.contains("m tech")
            || normalized.contains("mba")
            || normalized.contains("bca")
            || normalized.contains("mca")
            || normalized.contains("bba")
            || normalized.contains("ba")
            || normalized.contains("b com");
    }

    private static String firstToken(String value) {
        if (!hasText(value)) {
            return null;
        }
        String cleaned = clean(value).toLowerCase(Locale.ROOT);
        cleaned = cleaned.replaceAll("^[^a-z]+", "");
        if (!hasText(cleaned)) {
            return null;
        }
        String[] tokens = cleaned.split("[\\s\\-']+");
        for (String token : tokens) {
            if (hasText(token)) {
                String trimmed = token.replaceAll("[^a-z]", "").trim();
                if (hasText(trimmed) && !Set.of("mr", "mrs", "ms", "miss", "sir", "madam", "dr", "prof").contains(trimmed)) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private static String normalizeToken(String value) {
        if (!hasText(value)) {
            return null;
        }
        return clean(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    private static String clean(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}