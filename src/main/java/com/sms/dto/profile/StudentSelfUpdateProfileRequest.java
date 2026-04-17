package com.sms.dto.profile;

public class StudentSelfUpdateProfileRequest {
    private String phone;
    private String address;
    private String profileImage;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
}
