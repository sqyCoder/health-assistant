package com.health.assistant.storage;

import com.health.assistant.common.exception.ThirdPartyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDiskStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private LocalDiskStorageServiceImpl storage;

    @BeforeEach
    void setUp() {
        StorageProperties.Local config = new StorageProperties.Local();
        config.setRootDir(tempDir.toString());
        storage = new LocalDiskStorageServiceImpl(config);
    }

    @Test
    void shouldUploadDownloadAndDelete() throws Exception {
        byte[] data = "hello-health".getBytes(StandardCharsets.UTF_8);
        String objectKey = "reports/1/2/202609/abcdef.pdf";

        storage.upload("health-reports", objectKey, new ByteArrayInputStream(data), "application/pdf", data.length);

        try (InputStream in = storage.download("health-reports", objectKey)) {
            assertThat(in.readAllBytes()).isEqualTo(data);
        }

        storage.delete("health-reports", objectKey);
        assertThatThrownBy(() -> storage.download("health-reports", objectKey))
                .isInstanceOf(ThirdPartyException.class);
    }

    @Test
    void shouldReturnLocalPresignedUrl() {
        String url = storage.presignedUrl("health-reports", "reports/1/2/202609/abcdef.pdf", Duration.ofMinutes(60));
        assertThat(url).isEqualTo("/api/files/local/health-reports/reports/1/2/202609/abcdef.pdf");
    }

    @Test
    void shouldRejectPathTraversal() {
        byte[] data = "x".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> storage.upload("health-reports", "../../evil.txt",
                new ByteArrayInputStream(data), "text/plain", data.length))
                .isInstanceOf(ThirdPartyException.class);
    }
}
