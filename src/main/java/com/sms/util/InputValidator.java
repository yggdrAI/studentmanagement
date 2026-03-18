package util;

public class InputValidator {

    public static void validateMarks(double marks) throws CustomException {
        if (marks < 0 || marks > 100)
            throw new CustomException("Marks must be between 0 and 100.");
    }

    public static void validateName(String name) throws CustomException {
        if (name == null || name.trim().isEmpty())
            throw new CustomException("Name cannot be empty.");
    }
}