@Controller
public class TeacherController {

    @GetMapping("/teacher/dashboard")
    public String teacherDashboard() {
        return "teacher-dashboard";
    }
}