@Controller
public class StudentController {

    @GetMapping("/student/dashboard")
    public String studentDashboard() {
        return "student-dashboard";
    }
}