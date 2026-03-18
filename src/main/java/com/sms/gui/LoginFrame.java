package com.sms.gui;

import service.AuthenticationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final JTextField      idField;
    private final JPasswordField  passField;
    private final AuthenticationService auth = new AuthenticationService();

    public LoginFrame() {
        setTitle("SMS – Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setSize(420, 340);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout());

        // ── Header ──────────────────────────────────────────────────────────────
        JPanel header = new JPanel();
        header.setBackground(UITheme.BG_PANEL);
        header.setBorder(new EmptyBorder(20, 30, 20, 30));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel icon  = new JLabel("🎓", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Student Management System");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Sign in to continue");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(UITheme.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(icon);
        header.add(Box.createVerticalStrut(8));
        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(sub);

        // ── Form ────────────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_DARK);
        form.setBorder(new EmptyBorder(24, 40, 24, 40));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4);
        g.fill   = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        form.add(UITheme.styledLabel("Admin ID"), g);
        g.gridx = 1; g.weightx = 1;
        idField = UITheme.styledField(18);
        form.add(idField, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        form.add(UITheme.styledLabel("Password"), g);
        g.gridx = 1; g.weightx = 1;
        passField = UITheme.styledPassword(18);
        form.add(passField, g);

        g.gridx = 0; g.gridy = 2; g.gridwidth = 2; g.insets = new Insets(16, 4, 0, 4);
        JButton loginBtn = UITheme.accentButton("Sign In");
        loginBtn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 38));
        form.add(loginBtn, g);

        // ── Hint ────────────────────────────────────────────────────────────────
        JLabel hint = new JLabel("Default: admin / 1234", SwingConstants.CENTER);
        hint.setFont(UITheme.FONT_SMALL);
        hint.setForeground(UITheme.TEXT_MUTED);
        hint.setBorder(new EmptyBorder(0, 0, 12, 0));

        add(header, BorderLayout.NORTH);
        add(form,   BorderLayout.CENTER);
        add(hint,   BorderLayout.SOUTH);

        // ── Events ──────────────────────────────────────────────────────────────
        loginBtn.addActionListener(e -> attemptLogin());
        passField.addActionListener(e -> attemptLogin()); // enter key
        getRootPane().setDefaultButton(loginBtn);

        setVisible(true);
    }

    private void attemptLogin() {
        String id   = idField.getText().trim();
        String pass = new String(passField.getPassword());

        if (id.isEmpty() || pass.isEmpty()) {
            shake();
            return;
        }

        if (auth.login(id, pass)) {
            dispose();
            new MainFrame();
        } else {
            passField.setText("");
            JOptionPane.showMessageDialog(this,
                "Invalid credentials. Please try again.",
                "Login Failed", JOptionPane.ERROR_MESSAGE);
            shake();
        }
    }

    /** Brief horizontal shake animation to signal invalid login */
    private void shake() {
        Point orig = getLocation();
        Timer t = new Timer(30, null);
        int[] count = {0};
        int[] offsets = {-10, 10, -8, 8, -4, 4, 0};
        t.addActionListener(e -> {
            if (count[0] < offsets.length) {
                setLocation(orig.x + offsets[count[0]++], orig.y);
            } else {
                setLocation(orig);
                ((Timer) e.getSource()).stop();
            }
        });
        t.start();
    }
}
