package com.ashishhiggins.aws_bucket.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehendmedical.ComprehendMedicalClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.textract.TextractClient;

@Controller
public class S3Config {
    @Value("${cloud.aws.credentials.accessKey}")
    private String accessKey;
    @Value("${cloud.aws.credentials.secretKey}")
    private String secretKey;
    @Value("${cloud.aws.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create
                                (AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Bean
    public TextractClient textractClient() {
        return TextractClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create
                                (AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
    @Bean
    public ComprehendClient comprehendClient() {
        return ComprehendClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create
                                (AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
    @Bean
    public ComprehendMedicalClient comprehendMedicalClient() {
        return ComprehendMedicalClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create
                                (AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

}
