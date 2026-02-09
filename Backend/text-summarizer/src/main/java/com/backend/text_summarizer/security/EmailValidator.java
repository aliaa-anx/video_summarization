package com.backend.text_summarizer.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class EmailValidator {
    private static final String EMAIL_REGEX ="^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    private static final Pattern PATTERN=Pattern.compile(EMAIL_REGEX);
    public static boolean isValid(String email){
        if(email==null){
            return false;
        }
        return PATTERN.matcher(email).matches();
    }
}
