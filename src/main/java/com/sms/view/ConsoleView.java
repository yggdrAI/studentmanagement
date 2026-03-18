package view;

import java.util.Scanner;

public class ConsoleView {

    private Scanner sc = new Scanner(System.in);

    public void showMenu() {
        System.out.println("\n===== STUDENT MANAGEMENT SYSTEM =====");
        System.out.println("1. Add Student");
        System.out.println("2. Delete Student");
        System.out.println("3. Search Student");
        System.out.println("4. Display All");
        System.out.println("5. Sort By Name");
        System.out.println("6. Sort By ID");
        System.out.println("7. Sort By Marks");
        System.out.println("8. Exit");
    }

    public int getChoice() {
        return sc.nextInt();
    }

    public Scanner getScanner() {
        return sc;
    }
}