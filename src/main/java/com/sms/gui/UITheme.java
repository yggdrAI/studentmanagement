package com.sms.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class UITheme {

    // ── Palette ────────────────────────────────────────────────────────────────
    public static final Color BG_DARK      = new Color(18, 18, 30);
    public static final Color BG_PANEL     = new Color(28, 28, 45);
    public static final Color BG_CARD      = new Color(38, 38, 58);
    public static final Color ACCENT       = new Color(99, 102, 241);   // indigo
    public static final Color ACCENT_HOVER = new Color(79,  82, 221);
    public static final Color SUCCESS      = new Color(52, 211, 153);
    public static final Color DANGER       = new Color(239, 68,  68);
    public static final Color TEXT_PRIMARY = new Color(240, 240, 255);
    public static final Color TEXT_MUTED   = new Color(148, 148, 180);
    public static final Color TABLE_ALT    = new Color(33, 33, 52);
    public static final Color SELECTION    = new Color(99, 102, 241, 120);
    public static final Color BORDER       = new Color(55, 55, 80);

    // ── Fonts ──────────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  20);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO   = new Font("Consolas",  Font.PLAIN, 13);

    // ── Apply global L&F overrides ─────────────────────────────────────────────
    public static void apply() {
        UIManager.put("Panel.background",             BG_DARK);
        UIManager.put("OptionPane.background",        BG_PANEL);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("Button.background",            ACCENT);
        UIManager.put("Button.foreground",            Color.WHITE);
        UIManager.put("Button.focus",                 new Color(0,0,0,0));
        UIManager.put("TextField.background",         BG_CARD);
        UIManager.put("TextField.foreground",         TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground",    TEXT_PRIMARY);
        UIManager.put("PasswordField.background",     BG_CARD);
        UIManager.put("PasswordField.foreground",     TEXT_PRIMARY);
        UIManager.put("PasswordField.caretForeground",TEXT_PRIMARY);
        UIManager.put("Label.foreground",             TEXT_PRIMARY);
        UIManager.put("Table.background",             BG_DARK);
        UIManager.put("Table.foreground",             TEXT_PRIMARY);
        UIManager.put("Table.selectionBackground",    SELECTION);
        UIManager.put("Table.selectionForeground",    TEXT_PRIMARY);
        UIManager.put("Table.gridColor",              BORDER);
        UIManager.put("TableHeader.background",       BG_CARD);
        UIManager.put("TableHeader.foreground",       TEXT_PRIMARY);
        UIManager.put("ScrollPane.background",        BG_DARK);
        UIManager.put("ScrollBar.background",         BG_PANEL);
        UIManager.put("ScrollBar.thumb",              new Color(70,70,100));
        UIManager.put("TabbedPane.background",        BG_PANEL);
        UIManager.put("TabbedPane.foreground",        TEXT_PRIMARY);
        UIManager.put("TabbedPane.selected",          BG_DARK);
        UIManager.put("TabbedPane.contentAreaColor",  BG_DARK);
        UIManager.put("ComboBox.background",          BG_CARD);
        UIManager.put("ComboBox.foreground",          TEXT_PRIMARY);
        UIManager.put("Spinner.background",           BG_CARD);
        UIManager.put("Spinner.foreground",           TEXT_PRIMARY);
    }

    // ── Button factories ───────────────────────────────────────────────────────
    public static JButton accentButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_HEADER);
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(ACCENT_HOVER); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(ACCENT); }
        });
        return btn;
    }

    public static JButton dangerButton(String text) {
        JButton btn = accentButton(text);
        btn.setBackground(DANGER);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(DANGER.darker()); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(DANGER); }
        });
        return btn;
    }

    public static JButton ghostButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY);
        btn.setBackground(BG_CARD);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(BG_PANEL); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(BG_CARD); }
        });
        return btn;
    }

    // ── Styled text field ──────────────────────────────────────────────────────
    public static JTextField styledField(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(FONT_BODY);
        f.setBackground(BG_CARD);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(TEXT_PRIMARY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(6, 10, 6, 10)));
        return f;
    }

    public static JPasswordField styledPassword(int cols) {
        JPasswordField f = new JPasswordField(cols);
        f.setFont(FONT_BODY);
        f.setBackground(BG_CARD);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(TEXT_PRIMARY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(6, 10, 6, 10)));
        return f;
    }

    public static JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BODY);
        l.setForeground(TEXT_MUTED);
        return l;
    }
}
