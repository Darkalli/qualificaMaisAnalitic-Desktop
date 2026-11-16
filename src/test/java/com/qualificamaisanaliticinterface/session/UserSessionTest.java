package com.qualificamaisanaliticinterface.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.qualificamaisanaliticinterface.AuthTestSupport.session;

class UserSessionTest {
    @BeforeEach
    @AfterEach
    void clearSession() {
        UserSession.clear();
    }

    @Test
    void shouldStartWithoutSession() {
        Assertions.assertNull(UserSession.current());
    }

    @Test
    void shouldExposeTheStartedSession() {
        var response = session("first");
        UserSession.start(response);
        Assertions.assertSame(response, UserSession.current());
    }

    @Test
    void shouldReplacePreviousSession() {
        UserSession.start(session("first"));
        var replacement = session("second");
        UserSession.start(replacement);
        Assertions.assertSame(replacement, UserSession.current());
    }

    @Test
    void shouldClearSessionAndAllowRepeatedLogout() {
        UserSession.start(session("first"));
        UserSession.clear();
        Assertions.assertNull(UserSession.current());
        UserSession.clear();
        Assertions.assertNull(UserSession.current());
    }
}
