package com.sms.gui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

public final class UITheme {

    public static final Color BG_PRIMARY = new Color(0xF8FAFC);
    public static final Color BG_SECONDARY = new Color(0xFFFFFF);
    public static final Color BORDER = new Color(0xD9E2EE);

    public static final Color TEXT_PRIMARY = new Color(0x0F172A);
    public static final Color TEXT_SECONDARY = new Color(0x475569);
    public static final Color TEXT_MUTED = new Color(0x64748B);

    public static final Color ACCENT = new Color(0x2563EB);
    public static final Color ACCENT_STRONG = new Color(0x1D4ED8);
    public static final Color ACCENT_ALT = new Color(0x0EA5E9);
    public static final Color SUCCESS = new Color(0x16A34A);
    public static final Color WARNING = new Color(0xD97706);
    public static final Color DANGER = new Color(0xDC2626);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 30);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_METRIC = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BODY_SEMIBOLD = new Font("Segoe UI", Font.BOLD, 14);

    private UITheme() {
    }

    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
            // Keep default LAF if system LAF cannot be applied.
        }

        UIManager.put("Panel.background", BG_PRIMARY);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Button.font", FONT_BODY_SEMIBOLD);
        UIManager.put("Button.background", BG_SECONDARY);
        UIManager.put("Button.foreground", TEXT_PRIMARY);
    }

    public static JLabel createLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_SECONDARY);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));
        panel.setOpaque(true);
        return panel;
    }

    public static void applyCard(JComponent component) {
        component.setOpaque(true);
        component.setBackground(BG_SECONDARY);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));
    }
}
