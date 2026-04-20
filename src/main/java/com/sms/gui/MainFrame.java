package com.sms.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

public class MainFrame extends JFrame {
    public MainFrame() {
        UITheme.install();
        setTitle("Student Management System");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1320, 860));
        setLocationByPlatform(true);
        setContentPane(buildContent());
        getContentPane().setBackground(UITheme.BG_PRIMARY);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBorder(new EmptyBorder(24, 24, 24, 24));
        root.setBackground(UITheme.BG_PRIMARY);
        root.add(buildHero(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHero() {
        JPanel hero = new JPanel(new BorderLayout(18, 18));
        hero.setBackground(UITheme.BG_SECONDARY);
        hero.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
            new EmptyBorder(24, 24, 24, 24)
        ));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));

        JLabel eyebrow = UITheme.createLabel("SMART CAMPUS OPERATIONS", UITheme.FONT_BODY_SEMIBOLD, UITheme.ACCENT_STRONG);
        eyebrow.setAlignmentX(LEFT_ALIGNMENT);
        copy.add(eyebrow);
        copy.add(Box.createVerticalStrut(8));

        JLabel title = UITheme.createLabel("Student Management Command Center", UITheme.FONT_TITLE, UITheme.TEXT_PRIMARY);
        title.setAlignmentX(LEFT_ALIGNMENT);
        copy.add(title);
        copy.add(Box.createVerticalStrut(10));

        JTextArea description = new JTextArea(
            "A light, high-contrast desktop surface for enrollment, attendance, analytics, and operations. " +
            "This layout mirrors the web palette so the product feels like one system across platforms."
        );
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setEditable(false);
        description.setFocusable(false);
        description.setOpaque(false);
        description.setForeground(UITheme.TEXT_SECONDARY);
        description.setFont(UITheme.FONT_BODY);
        description.setAlignmentX(LEFT_ALIGNMENT);
        copy.add(description);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        JButton primary = createActionButton("Open Dashboard", true);
        JButton secondary = createActionButton("View Analytics", false);
        actions.add(primary);
        actions.add(secondary);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        copy.add(Box.createVerticalStrut(18));
        copy.add(actions);

        JPanel status = createStatusPanel();
        hero.add(copy, BorderLayout.CENTER);
        hero.add(status, BorderLayout.EAST);
        return hero;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(buildMetrics(), BorderLayout.NORTH);
        body.add(buildContentGrid(), BorderLayout.CENTER);
        return body;
    }

    private JPanel buildMetrics() {
        JPanel metrics = new JPanel(new GridLayout(1, 4, 14, 14));
        metrics.setOpaque(false);
        metrics.add(createMetricCard("Total Students", "4,520", "+128 this week", UITheme.ACCENT));
        metrics.add(createMetricCard("Active Today", "3,884", "86% engagement", UITheme.SUCCESS));
        metrics.add(createMetricCard("High Performers", "918", "Top quartile", UITheme.ACCENT_ALT));
        metrics.add(createMetricCard("At Risk", "164", "Needs attention", UITheme.DANGER));
        return metrics;
    }

    private JPanel buildContentGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 2, 16, 16));
        grid.setOpaque(false);
        grid.add(createOverviewCard());
        grid.add(createLiveFeedCard());
        return grid;
    }

    private JPanel createOverviewCard() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 16));

        JLabel heading = UITheme.createLabel("Operational Summary", UITheme.FONT_SECTION, UITheme.TEXT_PRIMARY);
        card.add(heading, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.add(createSummaryRow("Growth lead", "Week 4 reached 4,520 students", UITheme.ACCENT));
        list.add(Box.createVerticalStrut(12));
        list.add(createSummaryRow("Attendance trend", "Latest session at 90%", UITheme.SUCCESS));
        list.add(Box.createVerticalStrut(12));
        list.add(createSummaryRow("Scheduling", "Throughput remains balanced", UITheme.ACCENT_ALT));
        list.add(Box.createVerticalStrut(12));
        list.add(createSummaryRow("Theme", "Light mode is system-wide and persisted", UITheme.WARNING));

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JPanel createLiveFeedCard() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 16));

        JLabel heading = UITheme.createLabel("Realtime Activity", UITheme.FONT_SECTION, UITheme.TEXT_PRIMARY);
        card.add(heading, BorderLayout.NORTH);

        JPanel feed = new JPanel();
        feed.setOpaque(false);
        feed.setLayout(new BoxLayout(feed, BoxLayout.Y_AXIS));
        feed.add(createFeedItem("Student intake refreshed", "Enrollment data synchronized from the backend live snapshot.", UITheme.ACCENT));
        feed.add(Box.createVerticalStrut(10));
        feed.add(createFeedItem("Attendance spike detected", "Friday attendance crossed the weekly baseline.", UITheme.SUCCESS));
        feed.add(Box.createVerticalStrut(10));
        feed.add(createFeedItem("Theme system ready", "The desktop frame uses the same palette and accent language as the web UI.", UITheme.ACCENT_ALT));

        JScrollPane scrollPane = new JScrollPane(feed);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.BG_SECONDARY);
        scrollPane.setOpaque(false);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private JPanel createStatusPanel() {
        JPanel status = UITheme.createCardPanel();
        status.setPreferredSize(new Dimension(270, 0));
        status.setLayout(new BorderLayout(0, 12));

        JLabel label = UITheme.createLabel("Live Window", UITheme.FONT_BODY_SEMIBOLD, UITheme.TEXT_MUTED);
        JLabel value = UITheme.createLabel("Connected and polished", UITheme.FONT_SECTION, UITheme.TEXT_PRIMARY);
        JLabel source = UITheme.createLabel("Spring backend + shared theme tokens", UITheme.FONT_BODY, UITheme.TEXT_SECONDARY);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        label.setAlignmentX(LEFT_ALIGNMENT);
        value.setAlignmentX(LEFT_ALIGNMENT);
        source.setAlignmentX(LEFT_ALIGNMENT);
        stack.add(label);
        stack.add(Box.createVerticalStrut(8));
        stack.add(value);
        stack.add(Box.createVerticalStrut(8));
        stack.add(source);

        JPanel swatchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        swatchRow.setOpaque(false);
        swatchRow.add(createSwatch(UITheme.ACCENT));
        swatchRow.add(createSwatch(UITheme.ACCENT_ALT));
        swatchRow.add(createSwatch(UITheme.SUCCESS));
        swatchRow.add(createSwatch(UITheme.WARNING));

        status.add(stack, BorderLayout.NORTH);
        status.add(swatchRow, BorderLayout.SOUTH);
        return status;
    }

    private JPanel createMetricCard(String label, String value, String note, Color accent) {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 12));

        JLabel title = UITheme.createLabel(label, UITheme.FONT_BODY_SEMIBOLD, UITheme.TEXT_SECONDARY);
        JLabel number = UITheme.createLabel(value, UITheme.FONT_METRIC, UITheme.TEXT_PRIMARY);
        JLabel footer = UITheme.createLabel(note, UITheme.FONT_BODY, UITheme.TEXT_SECONDARY);

        JPanel line = new JPanel();
        line.setOpaque(false);
        line.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, accent));

        card.add(title, BorderLayout.NORTH);
        card.add(number, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        card.add(line, BorderLayout.EAST);
        card.setPreferredSize(new Dimension(0, 112));
        return card;
    }

    private JPanel createSummaryRow(String title, String description, Color accent) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        JPanel swatch = createSwatch(accent);
        row.add(swatch, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel titleLabel = UITheme.createLabel(title, UITheme.FONT_BODY_SEMIBOLD, UITheme.TEXT_PRIMARY);
        JLabel descriptionLabel = UITheme.createLabel(description, UITheme.FONT_BODY, UITheme.TEXT_SECONDARY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        descriptionLabel.setAlignmentX(LEFT_ALIGNMENT);
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(4));
        text.add(descriptionLabel);
        row.add(text, BorderLayout.CENTER);
        return row;
    }

    private JPanel createFeedItem(String title, String detail, Color accent) {
        JPanel item = new JPanel(new BorderLayout(12, 0));
        item.setOpaque(false);
        item.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
            new EmptyBorder(12, 12, 12, 12)
        ));

        item.add(createSwatch(accent), BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel titleLabel = UITheme.createLabel(title, UITheme.FONT_BODY_SEMIBOLD, UITheme.TEXT_PRIMARY);
        JLabel detailLabel = UITheme.createLabel(detail, UITheme.FONT_BODY, UITheme.TEXT_SECONDARY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        detailLabel.setAlignmentX(LEFT_ALIGNMENT);
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(4));
        text.add(detailLabel);
        item.add(text, BorderLayout.CENTER);
        return item;
    }

    private JButton createActionButton(String text, boolean filled) {
        JButton button = new JButton(text);
        button.setFont(UITheme.FONT_BODY_SEMIBOLD);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(filled ? UITheme.ACCENT : UITheme.BORDER, 1, true),
            new EmptyBorder(10, 16, 10, 16)
        ));
        if (filled) {
            button.setBackground(UITheme.ACCENT);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(UITheme.BG_SECONDARY);
            button.setForeground(UITheme.TEXT_PRIMARY);
        }
        return button;
    }

    private JPanel createSwatch(Color color) {
        JPanel swatch = new JPanel();
        swatch.setPreferredSize(new Dimension(12, 12));
        swatch.setMinimumSize(new Dimension(12, 12));
        swatch.setMaximumSize(new Dimension(12, 12));
        swatch.setBackground(color);
        swatch.setBorder(BorderFactory.createLineBorder(color.darker(), 1, true));
        return swatch;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
