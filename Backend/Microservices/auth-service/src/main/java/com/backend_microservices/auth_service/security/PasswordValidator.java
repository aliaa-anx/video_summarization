package com.backend_microservices.auth_service.security;

import java.util.regex.Pattern;

// this class is used to check if a password is strong enough before saving it in the database.
public class PasswordValidator {

    private static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])" +        // at least one number
                    "(?=.*[a-z])" +         // at least one lowercase letter
                    "(?=.*[A-Z])" +         // at least one uppercase letter
                    "(?=.*[@#$%^&+=!])" +   // at least one special character
                    "(?=\\S+$)" +           // no spaces
                    ".{8,}$";               // min length 8

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public static boolean isValid(String password) {
        return pattern.matcher(password).matches();
    }
}