package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.service.impl.S3FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceImplTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private MultipartFile multipartFile;

    private S3FileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new S3FileStorageServiceImpl(s3Client);
        ReflectionTestUtils.setField(fileStorageService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(fileStorageService, "region", "us-east-1");
    }

    @Test
    void storeFile_uploadsToS3AndReturnsUrl() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("photo.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(multipartFile.getSize()).thenReturn(3L);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String url = fileStorageService.storeFile(multipartFile);

        assertThat(url).startsWith("https://test-bucket.s3.us-east-1.amazonaws.com/");
        assertThat(url).endsWith("_photo.jpg");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void storeFile_s3Throws_throwsRuntimeException() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("photo.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        when(multipartFile.getSize()).thenReturn(1L);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 error"));

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload to S3");
    }

    @Test
    void storeFile_generatesUniqueKeyWithOriginalFilename() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("document.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        when(multipartFile.getSize()).thenReturn(1L);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String url = fileStorageService.storeFile(multipartFile);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        String key = captor.getValue().key();
        assertThat(key).endsWith("_document.pdf");
        assertThat(key).contains("_");
        // UUID prefix should be 36 chars + underscore
        assertThat(key.substring(0, 36)).matches("[0-9a-f\\-]{36}");
    }
}
