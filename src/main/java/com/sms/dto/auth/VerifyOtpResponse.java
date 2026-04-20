package com.sms.dto.auth;

public class VerifyOtpResponse {

    private final String resetToken;

    public VerifyOtpResponse(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getResetToken() {
        return resetToken;
    }
}
