package com.example.be.core.storage.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.storage.StorageService;
import com.example.be.core.storage.dto.FileUploadResult;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

@RestController
@RequestMapping(RoutesConstant.API_PREFIX + "/storage")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Set<String> ALLOWED_PROXY_HOSTS = Set.of("res.cloudinary.com");

    @GetMapping("/files")
    public ResponseEntity<ApiResponse<Object>> listFiles() {
        // Placeholder returning empty list to avoid 500 error
        return ResponseEntity.ok(ApiResponse.success(java.util.Collections.emptyList()));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResult>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "aerostride/general") String folder) {
        
        FileUploadResult result = storageService.uploadFile(file, folder);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/files/{publicId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String publicId) {
        storageService.deleteFile(publicId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyRemoteImage(@RequestParam("url") String rawUrl) {
        try {
            URI remoteUri = buildAllowedRemoteUri(rawUrl);
            HttpRequest request = HttpRequest.newBuilder(remoteUri)
                    .GET()
                    .header(HttpHeaders.USER_AGENT, "AeroStride-Image-Proxy")
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                return ResponseEntity.status(response.statusCode()).build();
            }

            MediaType contentType = resolveContentType(response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(null));

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                    .contentType(contentType)
                    .body(response.body());
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.badRequest().build();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().build();
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private URI buildAllowedRemoteUri(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            throw new IllegalArgumentException("Missing image url");
        }

        URI remoteUri = URI.create(rawUrl.trim());
        String scheme = remoteUri.getScheme();
        String host = remoteUri.getHost();

        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("Unsupported image url scheme");
        }

        if (!StringUtils.hasText(host) || ALLOWED_PROXY_HOSTS.stream().noneMatch(allowedHost -> allowedHost.equalsIgnoreCase(host))) {
            throw new IllegalArgumentException("Image host is not allowed");
        }

        return remoteUri;
    }

    private MediaType resolveContentType(String rawContentType) {
        if (!StringUtils.hasText(rawContentType)) {
            return MediaType.IMAGE_JPEG;
        }

        try {
            return MediaType.parseMediaType(rawContentType);
        } catch (Exception ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
