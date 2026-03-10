package com.rentalService.dto;

public class RegisterVendorDto {

    private String name;
    private String email;
    private String mobile;
    private String address;
    private String city;
    private String dob;

    public RegisterVendorDto() {
    }

    public RegisterVendorDto(String name, String email, String mobile,
                             String address, String city, String dob) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.address = address;
        this.city = city;
        this.dob = dob;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
}
