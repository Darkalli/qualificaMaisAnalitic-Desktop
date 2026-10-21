package com.qualificamaisanaliticinterface.session;

import com.sun.jna.platform.win32.Crypt32Util;
import com.sun.jna.platform.win32.WinCrypt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public final class WindowsTokenStore {
    private final Path file;

    public WindowsTokenStore() {
        String localAppData = System.getenv("LOCALAPPDATA");

        if (localAppData == null || localAppData.isBlank()) {
            throw new IllegalStateException("Armazenamento disponível somente no Windows.");
        }
        file = Path.of(localAppData, "QualificaMais", "session.dat");
    }

    public void save(String token) throws IOException {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token não informado");
        }
        byte[] plain = token.getBytes(StandardCharsets.UTF_8);

        try {
            byte[] encrypted = Crypt32Util.cryptProtectData(plain, WinCrypt.CRYPTPROTECT_UI_FORBIDDEN);

            Files.createDirectories(file.getParent());
            Files.write(file, encrypted);
        } finally {
            Arrays.fill(plain, (byte) 0);
        }
    }

    public Optional<String> load() throws IOException {
        if (Files.notExists(file)){
            return Optional.empty();
        }

        byte[] encrypted = Files.readAllBytes(file);
        byte[] plain = Crypt32Util.cryptUnprotectData(encrypted, WinCrypt.CRYPTPROTECT_UI_FORBIDDEN);

        try {
            return Optional.of(new String(plain, StandardCharsets.UTF_8));
        } finally {
            Arrays.fill(plain, (byte) 0);
        }
    }

    public void clear() throws IOException {
        Files.deleteIfExists(file);
    }
}
