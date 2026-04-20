package com.sms.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DefaultOtpDeliveryService implements OtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DefaultOtpDeliveryService.class);

    private final JavaMailSender javaMailSender;
    private final RestTemplate restTemplate;

    @Value("${app.auth.otp.mail-from:no-reply@student-management.local}")
    private String mailFrom;

    @Value("${app.auth.otp.sms.provider:log}")
    private String smsProvider;

    @Value("${app.auth.otp.expiry-minutes:5}")
    private long otpExpiryMinutes;

    @Value("${app.auth.otp.sms.fast2sms.api-key:}")
    private String fast2SmsApiKey;

    @Value("${app.auth.otp.sms.fast2sms.url:https://www.fast2sms.com/dev/bulkV2}")
    private String fast2SmsUrl;

    @Value("${app.auth.otp.sms.fast2sms.route:q}")
    private String fast2SmsRoute;

    public DefaultOtpDeliveryService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean sendEmailOtp(String email, String otpCode) {
        if (!StringUtils.hasText(email)) {
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("Password reset OTP");
            message.setText("Your OTP for password reset is " + otpCode + ". It expires in " + otpExpiryMinutes + " minutes.");
            javaMailSender.send(message);
            return true;
        } catch (MailException | IllegalArgumentException ex) {
            log.warn("Unable to send OTP email to {}: {}", email, ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendSmsOtp(String phoneNumber, String otpCode) {
        if (!StringUtils.hasText(phoneNumber)) {
            return false;
        }

        String provider = smsProvider == null ? "log" : smsProvider.trim().toLowerCase();
        if ("fast2sms".equals(provider)) {
            return sendViaFast2Sms(phoneNumber, otpCode);
        }

        log.info("OTP SMS delivery simulated via provider [{}] to [{}]", provider, phoneNumber);
        return true;
    }

    private boolean sendViaFast2Sms(String phoneNumber, String otpCode) {
        if (!StringUtils.hasText(fast2SmsApiKey)) {
            log.warn("Fast2SMS API key is not configured");
            return false;
        }

        try {
            String message = "Your OTP is " + otpCode + ". Valid for " + otpExpiryMinutes + " minutes.";
            String requestUrl = UriComponentsBuilder.fromHttpUrl(fast2SmsUrl)
                    .queryParam("route", fast2SmsRoute)
                    .queryParam("message", message)
                    .queryParam("language", "english")
                    .queryParam("flash", "0")
                    .queryParam("numbers", phoneNumber)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", fast2SmsApiKey);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    }
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException | IllegalArgumentException ex) {
            log.warn("Unable to send OTP SMS to {}: {}", phoneNumber, ex.getMessage());
            return false;
        }
    }
}
