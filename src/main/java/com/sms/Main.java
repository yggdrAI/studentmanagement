import model.*;
import service.*;
import util.*;
import view.ConsoleView;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        StudentService service = new StudentService();
        AuthenticationService auth = new AuthenticationService();
        FileService fileService = new FileService();
        ConsoleView view = new ConsoleView();
        Scanner sc = view.getScanner();

        service.setStudentMap(fileService.load());

        System.out.println("Login Required");
        System.out.print("Enter ID: ");
        String id = sc.next();
        System.out.print("Enter Password: ");
        String pass = sc.next();

        if (!auth.login(id, pass)) {
            System.out.println("Invalid Credentials!");
            return;
        }

        int choice;

        do {
            view.showMenu();
            choice = view.getChoice();

            switch (choice) {

                case 1:
                    System.out.print("Enter ID: ");
                    String sid = sc.next();
                    sc.nextLine();
                    System.out.print("Enter Name: ");
                    String name = sc.nextLine();

                    Student student = new Student(sid, name);

                    System.out.print("Number of courses: ");
                    int n = sc.nextInt();

                    for (int i = 0; i < n; i++) {
                        sc.nextLine();
                        System.out.print("Course name: ");
                        String cname = sc.nextLine();
                        System.out.print("Marks: ");
                        double marks = sc.nextDouble();

                        try {
                            InputValidator.validateMarks(marks);
                            student.addCourse(new Course(cname, marks));
                        } catch (CustomException e) {
                            System.out.println(e.getMessage());
                        }
                    }

                    service.addStudent(student);
                    break;

                case 2:
                    System.out.print("Enter ID to delete: ");
                    service.deleteStudent(sc.next());
                    break;

                case 3:
                    System.out.print("Enter ID: ");
                    Student s = service.searchById(sc.next());
                    System.out.println(s);
                    break;

                case 4:
                    service.getAllStudents().forEach(System.out::println);
                    break;

                case 5:
                    service.sortByName();
                    break;

                case 6:
                    service.sortById();
                    break;

                case 7:
                    service.sortByMarks();
                    break;

                case 8:
                    fileService.save(service.getStudentMap());
                    System.out.println("Data Saved. Exiting...");
                    break;

                default:
                    System.out.println("Invalid choice");
            }

        } while (choice != 8);
    }
}