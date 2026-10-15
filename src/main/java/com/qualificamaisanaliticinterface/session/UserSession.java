package com.qualificamaisanaliticinterface.session;

import com.qualificamaisanaliticinterface.features.login.dto.LoginResponse;

public final class UserSession {

    private static LoginResponse current;

    private UserSession() {
    }

    public static void start(LoginResponse response) {
        current = response;
    }

    public static LoginResponse current() {
        return current;
    }

    public static void clear() {
        current = null;
    }
}
