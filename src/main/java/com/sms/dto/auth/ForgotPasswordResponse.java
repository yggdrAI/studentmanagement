package com.sms.dto.auth;

public class ForgotPasswordResponse {

    private final boolean delivered;
    private final String channel;
    private final String otpPreview;

    public ForgotPasswordResponse(boolean delivered, String channel, String otpPreview) {
        this.delivered = delivered;
        this.channel = channel;
        this.otpPreview = otpPreview;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public String getChannel() {
        return channel;
    }

    public String getOtpPreview() {
        return otpPreview;
    }
}
