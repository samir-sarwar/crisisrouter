package com.crisisrouter.crisisRouter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider; // Import this
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    // You can remove the @Value for accessKey and secretKey!

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                // This "Default" provider checks your environment variables/files locally,
                // AND checks the IAM Role when deployed on AWS.
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}