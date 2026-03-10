package com.rentalService.dto;

import java.util.Set;
import com.rentalService.model.Interest;

public class RegisterCustomerDto {

    private String mobile;
    private String name;
    private String email;
    private String address;
    private String city;
    private String dob;              // ISO date "YYYY-MM-DD"
    private Set<Interest> interests;

    public RegisterCustomerDto() {
    }

    public RegisterCustomerDto(String mobile, String name, String email,
                               String address, String city, String dob,
                               Set<Interest> interests) {
        this.mobile = mobile;
        this.name = name;
        this.email = email;
        this.address = address;
        this.city = city;
        this.dob = dob;
        this.interests = interests;
    }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public Set<Interest> getInterests() { return interests; }
    public void setInterests(Set<Interest> interests) { this.interests = interests; }
}
