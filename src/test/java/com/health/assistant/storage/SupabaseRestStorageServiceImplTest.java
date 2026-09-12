package com.health.assistant.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.common.exception.ThirdPartyException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupabaseRestStorageServiceImplTest {

    private MockWebServer server;
    private SupabaseRestStorageServiceImpl storage;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        StorageProperties.Supabase config = new StorageProperties.Supabase();
        config.setUrl(server.url("/").toString());
        config.setServiceRoleKey("test-service-role-key");
        storage = new SupabaseRestStorageServiceImpl(config, new ObjectMapper());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldUploadWithCorrectRequest() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"Key\":\"health-reports/reports/1.pdf\"}"));
        byte[] data = "pdf-bytes".getBytes(StandardCharsets.UTF_8);

        storage.upload("health-reports", "reports/1.pdf", new ByteArrayInputStream(data), "application/pdf", data.length);

        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/storage/v1/object/health-reports/reports/1.pdf");
        assertThat(request.getHeader("apikey")).isEqualTo("test-service-role-key");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-service-role-key");
        assertThat(request.getBody().readByteArray()).isEqualTo(data);
    }

    @Test
    void shouldThrowThirdPartyExceptionWhenUploadFails() {
        server.enqueue(new MockResponse().setResponseCode(400)
                .setBody("{\"message\":\"The resource already exists\"}"));
        byte[] data = "pdf-bytes".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> storage.upload("health-reports", "reports/1.pdf",
                new ByteArrayInputStream(data), "application/pdf", data.length))
                .isInstanceOfSatisfying(ThirdPartyException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.STORAGE_ERROR.getCode());
                    assertThat(e.getMessage()).contains("The resource already exists");
                });
    }

    @Test
    void shouldDownloadBytes() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("file-content"));

        try (InputStream in = storage.download("health-reports", "reports/1.pdf")) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("file-content");
        }
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/storage/v1/object/health-reports/reports/1.pdf");
    }

    @Test
    void shouldCreatePresignedUrl() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"signedURL\":\"/object/sign/health-reports/reports/1.pdf?token=abc\"}"));

        String url = storage.presignedUrl("health-reports", "reports/1.pdf", Duration.ofMinutes(60));

        assertThat(url).isEqualTo(server.url("/") + "storage/v1/object/sign/health-reports/reports/1.pdf?token=abc");
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/storage/v1/object/sign/health-reports/reports/1.pdf");
        assertThat(request.getBody().readUtf8()).contains("\"expiresIn\":3600");
    }

    @Test
    void shouldThrowWhenPresignedUrlMissingSignedUrl() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        assertThatThrownBy(() -> storage.presignedUrl("health-reports", "reports/1.pdf", Duration.ofMinutes(60)))
                .isInstanceOf(ThirdPartyException.class);
    }

    @Test
    void shouldDeleteObject() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"message\":\"Successfully deleted\"}"));

        storage.delete("health-reports", "reports/1.pdf");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/storage/v1/object/health-reports/reports/1.pdf");
    }

    @Test
    void shouldFailFastWhenConfigMissing() {
        StorageProperties.Supabase config = new StorageProperties.Supabase();
        config.setUrl("");
        config.setServiceRoleKey("");

        assertThatThrownBy(() -> new SupabaseRestStorageServiceImpl(config, new ObjectMapper()))
                .isInstanceOf(IllegalStateException.class);
    }
}
