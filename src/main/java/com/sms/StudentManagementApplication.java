package com.sms;

import com.sms.gui.LoginFrame;
import com.sms.gui.UITheme;

import javax.swing.SwingUtilities;

public class StudentManagementApplication {

    public static void main(String[] args) {
        UITheme.apply();
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}
