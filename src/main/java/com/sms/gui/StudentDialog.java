package com.sms.gui;

import model.Course;
import model.Student;
import util.CustomException;
import util.InputValidator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal dialog for adding or editing a Student.
 * Accepts variable-length course rows.
 */
public class StudentDialog extends JDialog {

    private final JTextField idField;
    private final JTextField nameField;
    private final JPanel     coursePanel;
    private final List<JTextField> courseNames  = new ArrayList<>();
    private final List<JTextField> courseMarks  = new ArrayList<>();

    private Student result = null; // null means cancelled

    public StudentDialog(Frame owner, Student existing) {
        super(owner, existing == null ? "Add Student" : "Edit Student", true);
        setSize(480, 520);
        setLocationRelativeTo(owner);
        setResizable(false);
        getContentPane().setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(0, 0));

        // ── Main form panel ─────────────────────────────────────────────────────
        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(UITheme.BG_DARK);
        mainPanel.setBorder(new EmptyBorder(20, 24, 12, 24));

        // ID + Name
        JPanel topForm = new JPanel(new GridBagLayout());
        topForm.setBackground(UITheme.BG_DARK);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        topForm.add(UITheme.styledLabel("Student ID"), g);
        g.gridx = 1; g.weightx = 1;
        idField = UITheme.styledField(20);
        topForm.add(idField, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        topForm.add(UITheme.styledLabel("Full Name"), g);
        g.gridx = 1; g.weightx = 1;
        nameField = UITheme.styledField(20);
        topForm.add(nameField, g);

        mainPanel.add(topForm, BorderLayout.NORTH);

        // Course rows
        coursePanel = new JPanel();
        coursePanel.setLayout(new BoxLayout(coursePanel, BoxLayout.Y_AXIS));
        coursePanel.setBackground(UITheme.BG_DARK);

        JScrollPane courseScroll = new JScrollPane(coursePanel);
        courseScroll.setBackground(UITheme.BG_DARK);
        courseScroll.getViewport().setBackground(UITheme.BG_DARK);
        courseScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UITheme.BORDER),
            " Courses ",
            TitledBorder.LEFT, TitledBorder.TOP,
            UITheme.FONT_HEADER, UITheme.TEXT_MUTED));
        mainPanel.add(courseScroll, BorderLayout.CENTER);

        JButton addCourseBtn = UITheme.ghostButton("+ Add Course");
        addCourseBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addCourseBtn.addActionListener(e -> addCourseRow("", ""));
        mainPanel.add(addCourseBtn, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // ── Buttons ─────────────────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnRow.setBackground(UITheme.BG_DARK);
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));

        JButton cancel = UITheme.ghostButton("Cancel");
        JButton save   = UITheme.accentButton(existing == null ? "Add Student" : "Save Changes");

        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> onSave());

        btnRow.add(cancel);
        btnRow.add(save);
        add(btnRow, BorderLayout.SOUTH);

        // Pre-populate if editing
        if (existing != null) {
            idField.setText(existing.getId());
            idField.setEditable(false);
            nameField.setText(existing.getName());
            for (Course c : existing.getCourses()) {
                addCourseRow(c.getCourseName(), String.valueOf(c.getMarks()));
            }
        }

        // Always start with at least one course row
        if (courseNames.isEmpty()) addCourseRow("", "");
    }

    private void addCourseRow(String name, String marks) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(UITheme.BG_DARK);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        row.setBorder(new EmptyBorder(4, 4, 4, 4));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.fill   = GridBagConstraints.HORIZONTAL;

        JTextField nameField  = UITheme.styledField(14);
        nameField.setText(name);
        nameField.setToolTipText("Course name");

        JTextField marksField = UITheme.styledField(6);
        marksField.setText(marks);
        marksField.setToolTipText("Marks (0-100)");

        JButton remove = new JButton("✕");
        remove.setFont(UITheme.FONT_SMALL);
        remove.setForeground(UITheme.DANGER);
        remove.setBackground(UITheme.BG_DARK);
        remove.setBorderPainted(false);
        remove.setFocusPainted(false);
        remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        int idx = courseNames.size();
        courseNames.add(nameField);
        courseMarks.add(marksField);

        remove.addActionListener(e -> {
            courseNames.remove(nameField);
            courseMarks.remove(marksField);
            coursePanel.remove(row);
            coursePanel.revalidate();
            coursePanel.repaint();
        });

        g.gridx = 0; g.weightx = 2;
        row.add(nameField, g);
        g.gridx = 1; g.weightx = 1;
        row.add(marksField, g);
        g.gridx = 2; g.weightx = 0;
        row.add(remove, g);

        coursePanel.add(row);
        coursePanel.revalidate();
        coursePanel.repaint();
    }

    private void onSave() {
        String id   = idField.getText().trim();
        String name = nameField.getText().trim();

        if (id.isEmpty()) {
            showError("Student ID cannot be empty.");
            return;
        }
        try {
            InputValidator.validateName(name);
        } catch (CustomException ex) {
            showError(ex.getMessage());
            return;
        }

        Student s = new Student(id, name);

        for (int i = 0; i < courseNames.size(); i++) {
            String cname = courseNames.get(i).getText().trim();
            String mText = courseMarks.get(i).getText().trim();

            if (cname.isEmpty() && mText.isEmpty()) continue; // skip blank rows

            if (cname.isEmpty()) { showError("Course name missing for row " + (i + 1)); return; }
            double mVal;
            try {
                mVal = Double.parseDouble(mText);
                InputValidator.validateMarks(mVal);
            } catch (NumberFormatException ex) {
                showError("Marks must be a number for course: " + cname);
                return;
            } catch (CustomException ex) {
                showError(ex.getMessage() + " (course: " + cname + ")");
                return;
            }
            s.addCourse(new Course(cname, mVal));
        }

        result = s;
        dispose();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }

    /** @return the Student built from the dialog, or null if cancelled */
    public Student getResult() { return result; }
}
