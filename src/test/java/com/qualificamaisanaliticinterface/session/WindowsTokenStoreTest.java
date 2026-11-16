package com.qualificamaisanaliticinterface.session;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@EnabledOnOs(OS.WINDOWS)
class WindowsTokenStoreTest {
    @TempDir
    Path directory;
    private Path file;
    private WindowsTokenStore store;

    @BeforeEach
    void setUp() {
        file = directory.resolve("nested/session.dat");
        store = new WindowsTokenStore(file);
    }

    @Test
    void shouldReturnEmptyWhenFileDoesNotExist() throws Exception {
        Assertions.assertTrue(store.load().isEmpty());
        Assertions.assertFalse(Files.exists(file));
    }

    @Test
    void shouldEncryptAndRestoreSessionIncludingUnicode() throws Exception {
        String session = "{\"token\":\"test-only-token\",\"username\":\"João\"}";
        store.save(session);
        Assertions.assertTrue(Files.exists(file));
        Assertions.assertFalse(java.util.Arrays.equals(session.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file)));
        Assertions.assertFalse(new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1).contains("test-only-token"));
        Assertions.assertEquals(session, new WindowsTokenStore(file).load().orElseThrow());
    }

    @Test
    void shouldReplacePreviouslySavedSession() throws Exception {
        store.save("first-test-token");
        store.save("second-test-token");
        Assertions.assertEquals("second-test-token", store.load().orElseThrow());
    }

    @Test
    void shouldRejectMissingOrBlankValueWithoutCreatingFile() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> store.save(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> store.save(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> store.save("  "));
        Assertions.assertFalse(Files.exists(file));
    }

    @Test
    void shouldRemoveFileAndAllowRepeatedClear() throws Exception {
        store.save("test-token");
        store.clear();
        Assertions.assertFalse(Files.exists(file));
        Assertions.assertTrue(store.load().isEmpty());
        Assertions.assertDoesNotThrow(store::clear);
    }

    @Test
    void shouldReportCorruptEncryptedFile() throws Exception {
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] {1, 2, 3, 4});
        var error = Assertions.assertThrows(RuntimeException.class, store::load);
        Assertions.assertEquals("com.sun.jna.platform.win32.Win32Exception", error.getClass().getName());
        Assertions.assertTrue(Files.exists(file));
    }

    @Test
    void shouldPropagateFailureWhenParentPathIsAFile() throws Exception {
        Files.writeString(file.getParent(), "blocks directory creation");
        Assertions.assertThrows(java.io.IOException.class, () -> store.save("test-token"));
        Assertions.assertFalse(Files.exists(file));
    }
}
