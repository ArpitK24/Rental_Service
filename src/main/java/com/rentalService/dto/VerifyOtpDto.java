package com.rentalService.dto;

public class VerifyOtpDto {

    private String mobile;
    private String code;

    public VerifyOtpDto() {
    }

    public VerifyOtpDto(String mobile, String code) {
        this.mobile = mobile;
        this.code = code;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
