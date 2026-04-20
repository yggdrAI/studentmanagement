package com.sms.service;

public interface OtpDeliveryService {

    boolean sendEmailOtp(String email, String otpCode);

    boolean sendSmsOtp(String phoneNumber, String otpCode);
}
