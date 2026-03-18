package com.sms.gui;

import model.Course;
import model.Student;
import service.FileService;
import service.StudentService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {

    private final StudentService service     = new StudentService();
    private final FileService    fileService = new FileService();

    // Student table
    private final DefaultTableModel studentModel;
    private final JTable            studentTable;

    // Course detail table
    private final DefaultTableModel courseModel;
    private final JTable            courseTable;

    // Status bar
    private final JLabel statusLabel;

    public MainFrame() {
        setTitle("Student Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(0, 0));

        // ── Load saved data ──────────────────────────────────────────────────────
        service.setStudentMap(fileService.load());

        // ── Title bar strip ──────────────────────────────────────────────────────
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(UITheme.BG_PANEL);
        titleBar.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel titleLbl = new JLabel("🎓  Student Management System");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        titleBar.add(titleLbl, BorderLayout.WEST);

        JLabel savedBadge = new JLabel("Auto-Save OFF");
        savedBadge.setFont(UITheme.FONT_SMALL);
        savedBadge.setForeground(UITheme.TEXT_MUTED);
        titleBar.add(savedBadge, BorderLayout.EAST);

        add(titleBar, BorderLayout.NORTH);

        // ── Centre split ─────────────────────────────────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(560);
        split.setDividerSize(4);
        split.setBackground(UITheme.BG_DARK);
        split.setBorder(null);

        // ── LEFT: Student table ──────────────────────────────────────────────────
        String[] studentCols = {"ID", "Name", "Courses", "Average"};
        studentModel = new DefaultTableModel(studentCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        studentTable = buildTable(studentModel);

        // Colour-code average column
        studentTable.getColumnModel().getColumn(3)
            .setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                        JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                    super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                    setHorizontalAlignment(CENTER);
                    if (v instanceof Double) {
                        double avg = (Double) v;
                        setForeground(avg >= 75 ? UITheme.SUCCESS
                                    : avg >= 40 ? new Color(250, 200, 70)
                                    : UITheme.DANGER);
                    }
                    if (!sel) setBackground(r % 2 == 0 ? UITheme.BG_DARK : UITheme.TABLE_ALT);
                    return this;
                }
            });

        // Column widths
        studentTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        studentTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        studentTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        studentTable.getColumnModel().getColumn(3).setPreferredWidth(80);

        JScrollPane studentScroll = styledScroll(studentTable);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 0));
        leftPanel.setBackground(UITheme.BG_DARK);
        leftPanel.add(sectionHeader("Students"), BorderLayout.NORTH);
        leftPanel.add(studentScroll,             BorderLayout.CENTER);
        split.setLeftComponent(leftPanel);

        // ── RIGHT: Course detail ─────────────────────────────────────────────────
        String[] courseCols = {"Course", "Marks", "Grade"};
        courseModel = new DefaultTableModel(courseCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        courseTable = buildTable(courseModel);
        courseTable.getColumnModel().getColumn(2)
            .setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                        JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                    super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                    setHorizontalAlignment(CENTER);
                    if ("A".equals(v) || "A+".equals(v))   setForeground(UITheme.SUCCESS);
                    else if ("F".equals(v))                 setForeground(UITheme.DANGER);
                    else                                    setForeground(UITheme.TEXT_PRIMARY);
                    if (!sel) setBackground(r % 2 == 0 ? UITheme.BG_DARK : UITheme.TABLE_ALT);
                    return this;
                }
            });

        JScrollPane courseScroll = styledScroll(courseTable);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(UITheme.BG_DARK);
        rightPanel.add(sectionHeader("Course Details"), BorderLayout.NORTH);
        rightPanel.add(courseScroll,                     BorderLayout.CENTER);
        split.setRightComponent(rightPanel);

        add(split, BorderLayout.CENTER);

        // ── Toolbar ──────────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(UITheme.BG_PANEL);
        toolbar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));

        JButton btnAdd    = UITheme.accentButton("+ Add");
        JButton btnEdit   = UITheme.ghostButton("✏ Edit");
        JButton btnDelete = UITheme.dangerButton("🗑 Delete");
        JButton btnSearch = UITheme.ghostButton("🔍 Search");

        JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
        sep1.setPreferredSize(new Dimension(1, 28));
        sep1.setForeground(UITheme.BORDER);

        JButton btnSortName  = UITheme.ghostButton("Sort: Name");
        JButton btnSortId    = UITheme.ghostButton("Sort: ID");
        JButton btnSortMarks = UITheme.ghostButton("Sort: Marks ↓");

        JSeparator sep2 = new JSeparator(SwingConstants.VERTICAL);
        sep2.setPreferredSize(new Dimension(1, 28));
        sep2.setForeground(UITheme.BORDER);

        JButton btnSave    = UITheme.ghostButton("💾 Save");
        JButton btnRefresh = UITheme.ghostButton("↻ Refresh");

        toolbar.add(btnAdd);
        toolbar.add(btnEdit);
        toolbar.add(btnDelete);
        toolbar.add(btnSearch);
        toolbar.add(sep1);
        toolbar.add(btnSortName);
        toolbar.add(btnSortId);
        toolbar.add(btnSortMarks);
        toolbar.add(sep2);
        toolbar.add(btnSave);
        toolbar.add(btnRefresh);

        add(toolbar, BorderLayout.SOUTH);

        // ── Status bar ───────────────────────────────────────────────────────────
        statusLabel = new JLabel(" Ready");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        statusLabel.setBorder(new EmptyBorder(2, 8, 2, 8));

        // Push status above toolbar
        JPanel southWrapper = new JPanel(new BorderLayout());
        southWrapper.setBackground(UITheme.BG_PANEL);
        southWrapper.add(statusLabel, BorderLayout.WEST);
        southWrapper.add(toolbar,     BorderLayout.CENTER);
        remove(toolbar);
        add(southWrapper, BorderLayout.SOUTH);

        // ── Wire up selection → course detail ────────────────────────────────────
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showCourseDetail();
        });

        // ── Button actions ────────────────────────────────────────────────────────
        btnAdd.addActionListener(e -> addStudent());
        btnEdit.addActionListener(e -> editStudent());
        btnDelete.addActionListener(e -> deleteStudent());
        btnSearch.addActionListener(e -> searchStudent());
        btnSortName.addActionListener(e -> { service.sortByName();  refreshTable(); setStatus("Sorted by name"); });
        btnSortId.addActionListener(e   -> { service.sortById();    refreshTable(); setStatus("Sorted by ID"); });
        btnSortMarks.addActionListener(e -> { service.sortByMarks(); refreshTable(); setStatus("Sorted by marks (desc)"); });
        btnSave.addActionListener(e -> saveData());
        btnRefresh.addActionListener(e -> { refreshTable(); setStatus("Refreshed"); });

        refreshTable();
        setVisible(true);
    }

    // ── Actions ────────────────────────────────────────────────────────────────
    private void addStudent() {
        StudentDialog dlg = new StudentDialog(this, null);
        dlg.setVisible(true);
        Student s = dlg.getResult();
        if (s != null) {
            if (service.searchById(s.getId()) != null) {
                showError("A student with ID '" + s.getId() + "' already exists.");
                return;
            }
            service.addStudent(s);
            refreshTable();
            setStatus("Added: " + s.getName());
        }
    }

    private void editStudent() {
        Student sel = getSelectedStudent();
        if (sel == null) { showError("Select a student to edit."); return; }
        StudentDialog dlg = new StudentDialog(this, sel);
        dlg.setVisible(true);
        Student updated = dlg.getResult();
        if (updated != null) {
            service.addStudent(updated); // overwrites same ID
            refreshTable();
            setStatus("Updated: " + updated.getName());
        }
    }

    private void deleteStudent() {
        Student sel = getSelectedStudent();
        if (sel == null) { showError("Select a student to delete."); return; }
        int ok = JOptionPane.showConfirmDialog(this,
            "Delete '" + sel.getName() + "' (" + sel.getId() + ")?\nThis cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            service.deleteStudent(sel.getId());
            courseModel.setRowCount(0);
            refreshTable();
            setStatus("Deleted: " + sel.getName());
        }
    }

    private void searchStudent() {
        String id = JOptionPane.showInputDialog(this, "Enter Student ID to search:", "Search", JOptionPane.QUESTION_MESSAGE);
        if (id == null) return;
        Student s = service.searchById(id.trim());
        if (s == null) {
            JOptionPane.showMessageDialog(this, "No student found with ID: " + id);
        } else {
            // Highlight in table
            for (int r = 0; r < studentTable.getRowCount(); r++) {
                if (studentTable.getValueAt(r, 0).equals(s.getId())) {
                    studentTable.setRowSelectionInterval(r, r);
                    studentTable.scrollRectToVisible(studentTable.getCellRect(r, 0, true));
                    break;
                }
            }
            setStatus("Found: " + s.getName());
        }
    }

    private void saveData() {
        fileService.save(service.getStudentMap());
        setStatus("Data saved to disk ✓");
        JOptionPane.showMessageDialog(this, "Data saved successfully!", "Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Table helpers ──────────────────────────────────────────────────────────
    private void refreshTable() {
        studentModel.setRowCount(0);
        List<Student> list = service.getAllStudents();
        for (Student s : list) {
            studentModel.addRow(new Object[]{
                s.getId(),
                s.getName(),
                s.getCourses().size(),
                Math.round(s.calculateAverage() * 100.0) / 100.0
            });
        }
        setStatus(list.size() + " student(s) loaded");
    }

    private void showCourseDetail() {
        courseModel.setRowCount(0);
        Student s = getSelectedStudent();
        if (s == null) return;
        for (Course c : s.getCourses()) {
            courseModel.addRow(new Object[]{
                c.getCourseName(),
                c.getMarks(),
                letterGrade(c.getMarks())
            });
        }
    }

    private Student getSelectedStudent() {
        int row = studentTable.getSelectedRow();
        if (row < 0) return null;
        String id = (String) studentTable.getValueAt(row, 0);
        return service.searchById(id);
    }

    private String letterGrade(double marks) {
        if (marks >= 90) return "A+";
        if (marks >= 80) return "A";
        if (marks >= 70) return "B";
        if (marks >= 60) return "C";
        if (marks >= 40) return "D";
        return "F";
    }

    // ── UI utilities ───────────────────────────────────────────────────────────
    private JTable buildTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override public Component prepareRenderer(
                    javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? UITheme.BG_DARK : UITheme.TABLE_ALT);
                    c.setForeground(UITheme.TEXT_PRIMARY);
                }
                return c;
            }
        };
        table.setFont(UITheme.FONT_BODY);
        table.setRowHeight(32);
        table.setBackground(UITheme.BG_DARK);
        table.setForeground(UITheme.TEXT_PRIMARY);
        table.setGridColor(UITheme.BORDER);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(UITheme.ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setFocusable(false);
        table.getTableHeader().setFont(UITheme.FONT_HEADER);
        table.getTableHeader().setBackground(UITheme.BG_CARD);
        table.getTableHeader().setForeground(UITheme.TEXT_MUTED);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER));
        return table;
    }

    private JScrollPane styledScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(UITheme.BG_DARK);
        sp.getViewport().setBackground(UITheme.BG_DARK);
        sp.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, UITheme.BORDER));
        return sp;
    }

    private JPanel sectionHeader(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.BG_CARD);
        p.setBorder(new EmptyBorder(8, 14, 8, 14));
        JLabel lbl = new JLabel(title);
        lbl.setFont(UITheme.FONT_HEADER);
        lbl.setForeground(UITheme.ACCENT);
        p.add(lbl);
        return p;
    }

    private void setStatus(String msg) {
        statusLabel.setText("  " + msg);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
