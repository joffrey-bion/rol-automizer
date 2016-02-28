package org.hildan.bots.riseoflords.sequencing;

public class LoginException extends Exception {

    private final String username;

    private final String password;

    public LoginException(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
