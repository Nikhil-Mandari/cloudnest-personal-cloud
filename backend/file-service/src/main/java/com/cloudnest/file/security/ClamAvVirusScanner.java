package com.cloudnest.file.security;

import com.cloudnest.file.config.VirusScannerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Virus scanner that talks to a ClamAV daemon (clamd) over TCP using the
 * INSTREAM protocol.
 * <p>
 * Activated with {@code virus-scanner.provider=clamav}. The content is
 * streamed in length-prefixed chunks and the daemon replies with
 * {@code stream: OK} (clean) or {@code stream: <signature> FOUND} (infected).
 * A connectivity failure is reported as {@link ScanOutcome#ERROR} (fail open,
 * clearly logged) rather than crashing the upload.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "virus-scanner.provider", havingValue = "clamav")
public class ClamAvVirusScanner implements VirusScanner {

    private static final byte[] INSTREAM_COMMAND = "zINSTREAM\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] END_CHUNK = {0, 0, 0, 0};
    private static final int CHUNK_SIZE = 16_384;

    private final VirusScannerProperties properties;

    public ClamAvVirusScanner(VirusScannerProperties properties) {
        this.properties = properties;
    }

    @Override
    public ScanOutcome scan(InputStream content, String objectName) {
        try (Socket socket = new Socket(properties.getClamdHost(), properties.getClamdPort())) {
            socket.setSoTimeout(properties.getTimeoutMs());
            try (InputStream in = socket.getInputStream(); OutputStream out = socket.getOutputStream()) {
                out.write(INSTREAM_COMMAND);

                byte[] buffer = new byte[CHUNK_SIZE];
                int read;
                while ((read = content.read(buffer)) != -1) {
                    out.write(ByteBuffer.allocate(4).putInt(read).array());
                    out.write(buffer, 0, read);
                }
                out.write(END_CHUNK);
                out.flush();

                StringBuilder response = new StringBuilder();
                int c;
                while ((c = in.read()) != -1) {
                    response.append((char) c);
                }
                String text = response.toString();

                if (text.contains("FOUND")) {
                    log.warn("ClamAV flagged '{}' as infected: {}", objectName, text.trim());
                    return ScanOutcome.INFECTED;
                }
                if (text.contains("OK")) {
                    log.debug("ClamAV scan clean for '{}'", objectName);
                    return ScanOutcome.CLEAN;
                }
                log.warn("ClamAV returned an unexpected response for '{}': {}", objectName, text.trim());
                return ScanOutcome.ERROR;
            } finally {
                try {
                    content.close();
                } catch (IOException ignored) {
                    // already closed / caller owns the stream
                }
            }
        } catch (IOException e) {
            log.error("ClamAV daemon unreachable at {}:{} — upload '{}' failed open: {}",
                    properties.getClamdHost(), properties.getClamdPort(), objectName, e.getMessage());
            return ScanOutcome.ERROR;
        }
    }

    @Override
    public String name() {
        return "clamav";
    }
}
