package com.qualificamaisanaliticinterface;

import com.google.gson.Gson;
import com.qualificamaisanaliticinterface.features.login.dto.LoginResponse;
import com.qualificamaisanaliticinterface.session.TokenStore;
import javafx.application.Platform;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

public final class AuthTestSupport {
    public static final String BASE_URL = "https://qualifica.test";
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
    public static final Gson GSON = new Gson();

    private AuthTestSupport() {
    }

    public static LoginResponse session(String token) {
        return new LoginResponse(token, "Bearer", "2026-11-17T12:00:00Z",
                new LoginResponse.UserInfo(7L, "ana", "ADMIN"));
    }

    public static Throwable failure(CompletableFuture<?> future) {
        Throwable cause = Assertions.assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS)).getCause();
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    public static void startFx() {
        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException _) {
        }
    }

    public static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }

    public static final class MemoryTokenStore implements TokenStore {
        public String saved;
        public int saves;
        public int loads;
        public int clears;
        public IOException saveFailure;
        public IOException loadFailure;
        public IOException clearFailure;

        @Override
        public void save(String value) throws IOException {
            saves++;
            if (saveFailure != null) {
                throw saveFailure;
            }
            saved = value;
        }

        @Override
        public Optional<String> load() throws IOException {
            loads++;
            if (loadFailure != null) {
                throw loadFailure;
            }
            return Optional.ofNullable(saved);
        }

        @Override
        public void clear() throws IOException {
            clears++;
            if (clearFailure != null) {
                throw clearFailure;
            }
            saved = null;
        }
    }

    public static final class StubHttpClient extends HttpClient {
        public final List<HttpRequest> requests = new ArrayList<>();
        private int status = 200;
        private String body = "{}";
        private Throwable failure;

        public void respond(int status, String body) {
            this.status = status;
            this.body = body;
            this.failure = null;
        }

        public void fail(Throwable failure) {
            this.failure = failure;
        }

        public HttpRequest lastRequest() {
            return requests.getLast();
        }

        public String requestBody() throws Exception {
            var subscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
            lastRequest().bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }

                @Override
                public void onNext(ByteBuffer item) {
                    subscriber.onNext(List.of(item));
                }

                @Override
                public void onError(Throwable failure) {
                    subscriber.onError(failure);
                }

                @Override
                public void onComplete() {
                    subscriber.onComplete();
                }
            });
            return subscriber.getBody().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            requests.add(request);
            if (failure != null) {
                return CompletableFuture.failedFuture(failure);
            }
            HttpHeaders headers = HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (a, b) -> true);
            var subscriber = handler.apply(new HttpResponse.ResponseInfo() {
                @Override
                public int statusCode() {
                    return status;
                }

                @Override
                public HttpHeaders headers() {
                    return headers;
                }

                @Override
                public Version version() {
                    return Version.HTTP_1_1;
                }
            });
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
            subscriber.onNext(List.of(ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8))));
            subscriber.onComplete();
            int responseStatus = status;
            return subscriber.getBody().toCompletableFuture()
                    .thenApply(value -> new Response<>(responseStatus, request, headers, value));
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                HttpResponse.BodyHandler<T> handler, HttpResponse.PushPromiseHandler<T> pushHandler) {
            return sendAsync(request, handler);
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            throw new UnsupportedOperationException("These services must use asynchronous HTTP.");
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            try {
                return SSLContext.getDefault();
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }
    }

    private record Response<T>(int statusCode, HttpRequest request, HttpHeaders headers, T body)
            implements HttpResponse<T> {
        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
