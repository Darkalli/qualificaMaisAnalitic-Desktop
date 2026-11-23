package com.qualificamaisanaliticinterface.session;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

public final class SupersetSession extends CookieHandler {
    private static final System.Logger LOG = System.getLogger(SupersetSession.class.getName());
    private static final String COOKIE_NAME = "session";
    private static final Duration MAX_REMEMBER_TIME = Duration.ofDays(7);
    private static SupersetSession installed;
    private static final Set<WebEngine> ENGINES = Collections.newSetFromMap(new WeakHashMap<>());

    private final CookieHandler delegate;
    private final URI server;
    private final TokenStore storage;
    private final Clock clock;
    private Properties saved;
    private boolean enabled = true;

    public static synchronized void install(WebEngine engine, String supersetUrl) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Instale a sessão na thread do JavaFX.");
        }
        ENGINES.add(Objects.requireNonNull(engine));
        URI server = origin(URI.create(supersetUrl));
        if (installed != null && installed.server.equals(server)
                && CookieHandler.getDefault() == installed) {
            installed.enable();
            return;
        }
        CookieHandler handler = CookieHandler.getDefault();
        if (handler instanceof SupersetSession previous) {
            previous.disableInMemory();
            handler = previous.delegate;
        }
        if (handler == null) {
            handler = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        }
        installed = new SupersetSession(handler, server, defaultStore(), Clock.systemUTC());
        CookieHandler.setDefault(installed);
    }

    public static synchronized void clearSavedSession() throws IOException {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Encerre a sessão na thread do JavaFX.");
        }
        try {
            if (installed != null) {
                installed.clear();
            } else {
                defaultStore().clear();
            }
        } finally {
            for (WebEngine engine : List.copyOf(ENGINES)) {
                engine.getLoadWorker().cancel();
                engine.load("about:blank");
            }
            ENGINES.clear();
        }
    }

    private static TokenStore defaultStore() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isBlank()) {
            throw new IllegalStateException("A sessão persistente do Superset requer Windows nesta implementação.");
        }
        return new WindowsTokenStore(Path.of(localAppData, "QualificaMais", "superset-session.dat"));
    }

    SupersetSession(CookieHandler delegate, URI server, TokenStore storage, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate);
        this.server = origin(server);
        this.storage = Objects.requireNonNull(storage);
        this.clock = Objects.requireNonNull(clock);
        restore();
    }

    @Override
    public synchronized Map<String, List<String>> get(URI uri, Map<String, List<String>> headers)
            throws IOException {
        if (!enabled && belongsToServer(uri)) {
            return Map.of();
        }
        return delegate.get(uri, headers);
    }

    @Override
    public synchronized void put(URI uri, Map<String, List<String>> headers) throws IOException {
        if (!enabled && belongsToServer(uri)) {
            return;
        }
        delegate.put(uri, headers);
        if (!sameOrigin(uri)) {
            return;
        }
        for (var entry : headers.entrySet()) {
            if (!"Set-Cookie".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            for (String header : entry.getValue()) {
                remember(uri, header);
            }
        }
    }

    private void remember(URI source, String header) {
        try {
            List<HttpCookie> cookies = HttpCookie.parse(header);
            if (cookies.size() != 1 || !COOKIE_NAME.equals(cookies.getFirst().getName())) {
                return;
            }
            HttpCookie cookie = cookies.getFirst();
            if (cookie.hasExpired() || cookie.getValue().isEmpty()) {
                saved = null;
                storage.clear();
                return;
            }
            long seconds = MAX_REMEMBER_TIME.toSeconds();
            if (cookie.getMaxAge() >= 0) {
                seconds = Math.min(seconds, cookie.getMaxAge());
            }
            Properties record = new Properties();
            record.setProperty("origin", server.toString());
            record.setProperty("source", source.toString());
            record.setProperty("header", header);
            record.setProperty("expires", Long.toString(clock.instant().plusSeconds(seconds).toEpochMilli()));
            saved = record;
            StringWriter text = new StringWriter();
            record.store(text, "Superset session - protected with Windows DPAPI");
            storage.save(text.toString());
        } catch (IOException | RuntimeException failure) {
            LOG.log(System.Logger.Level.WARNING, "Não foi possível salvar a sessão do Superset neste computador.");
        }
    }

    private void restore() {
        try {
            var text = storage.load();
            if (text.isEmpty()) {
                return;
            }
            Properties record = new Properties();
            record.load(new StringReader(text.get()));
            URI source = URI.create(record.getProperty("source"));
            String header = record.getProperty("header");
            long remainingMillis = Long.parseLong(record.getProperty("expires")) - clock.millis();
            List<HttpCookie> cookies = HttpCookie.parse(header);
            if (!server.toString().equals(record.getProperty("origin")) || !sameOrigin(source)
                    || remainingMillis <= 0 || remainingMillis > MAX_REMEMBER_TIME.toMillis()
                    || cookies.size() != 1 || !COOKIE_NAME.equals(cookies.getFirst().getName())
                    || cookies.getFirst().getValue().isEmpty()) {
                storage.clear();
                return;
            }
            long remainingSeconds = Math.max(1, (remainingMillis + 999) / 1000);
            String restoredHeader = header.replaceAll("(?i);\\s*max-age\\s*=\\s*[^;]*", "")
                    + "; Max-Age=" + remainingSeconds;
            delegate.put(source, Map.of("Set-Cookie", List.of(restoredHeader)));
            saved = record;
        } catch (IOException | RuntimeException failure) {
            try {
                storage.clear();
            } catch (IOException ignored) {
                // An unavailable store must not prevent an ordinary manual login.
            }
            LOG.log(System.Logger.Level.WARNING, "A sessão salva do Superset não pôde ser restaurada; entre novamente.");
        }
    }

    synchronized void clear() throws IOException {
        enabled = false;
        IOException failure = null;
        try {
            expireInMemory();
        } catch (IOException error) {
            failure = error;
        }
        storage.clear();
        if (failure != null) {
            throw failure;
        }
        saved = null;
    }

    private synchronized void enable() {
        if (!enabled) {
            try {
                expireInMemory();
            } catch (IOException error) {
                throw new IllegalStateException("Não foi possível limpar a sessão anterior do Superset.", error);
            }
            enabled = true;
        }
    }

    private synchronized void disableInMemory() {
        enabled = false;
        try {
            expireInMemory();
        } catch (IOException error) {
            throw new IllegalStateException("Não foi possível limpar a sessão anterior do Superset.", error);
        }
    }

    private void expireInMemory() throws IOException {
        if (saved == null) {
            return;
        }
        HttpCookie cookie = HttpCookie.parse(saved.getProperty("header")).getFirst();
        URI source = URI.create(saved.getProperty("source"));
        String path = cookie.getPath();
        if (path == null || path.isBlank()) {
            String sourcePath = source.getPath();
            int separator = sourcePath.lastIndexOf('/');
            path = separator <= 0 ? "/" : sourcePath.substring(0, separator);
        }
        String deleted = COOKIE_NAME + "=; Max-Age=0; Path=" + path;
        if (cookie.getDomain() != null) {
            deleted += "; Domain=" + cookie.getDomain();
        }
        if (cookie.isHttpOnly()) {
            deleted += "; HttpOnly";
        }
        if (cookie.getSecure()) {
            deleted += "; Secure";
        }
        delegate.put(source, Map.of("Set-Cookie", List.of(deleted)));
    }

    private boolean sameOrigin(URI uri) {
        String scheme = uri.getScheme();
        return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && server.equals(origin(uri));
    }

    private boolean belongsToServer(URI uri) {
        String scheme = uri.getScheme();
        if ("javascript".equalsIgnoreCase(scheme)) {
            return sameOrigin(URI.create("http" + uri.toString().substring(scheme.length())));
        }
        if ("javascripts".equalsIgnoreCase(scheme)) {
            return sameOrigin(URI.create("https" + uri.toString().substring(scheme.length())));
        }
        return sameOrigin(uri);
    }

    private static URI origin(URI uri) {
        String scheme = uri.getScheme();
        if (uri.getHost() == null || uri.getUserInfo() != null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("Informe uma URL HTTP ou HTTPS válida para o Superset.");
        }
        int port = uri.getPort() >= 0 ? uri.getPort() : ("https".equalsIgnoreCase(scheme) ? 443 : 80);
        String host = uri.getHost();
        if (host.contains(":") && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        return URI.create(scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT) + ":" + port + "/");
    }
}
