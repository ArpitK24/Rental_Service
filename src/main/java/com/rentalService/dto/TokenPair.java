package com.rentalService.dto;

public class TokenPair {

    private String accessToken;
    private String refreshToken;
    private boolean userExists;

    public TokenPair() {
    }

    public TokenPair(String accessToken, String refreshToken, boolean userExists) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userExists = userExists;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public boolean isUserExists() {
        return userExists;
    }

    public void setUserExists(boolean userExists) {
        this.userExists = userExists;
    }
}
