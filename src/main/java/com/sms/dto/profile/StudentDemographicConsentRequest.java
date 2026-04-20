package com.sms.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StudentDemographicConsentRequest {

    @NotNull(message = "Consent confirmation is required")
    private Boolean consentGiven;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Religion is required")
    private String religion;

    @NotBlank(message = "Category is required")
    private String category;

    private String specificCaste;

    public Boolean getConsentGiven() { return consentGiven; }
    public void setConsentGiven(Boolean consentGiven) { this.consentGiven = consentGiven; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSpecificCaste() { return specificCaste; }
    public void setSpecificCaste(String specificCaste) { this.specificCaste = specificCaste; }
}