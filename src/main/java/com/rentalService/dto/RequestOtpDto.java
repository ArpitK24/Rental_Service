package com.rentalService.dto;

public class RequestOtpDto {

    private String mobile;

    public RequestOtpDto() {
    }

    public RequestOtpDto(String mobile) {
        this.mobile = mobile;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
