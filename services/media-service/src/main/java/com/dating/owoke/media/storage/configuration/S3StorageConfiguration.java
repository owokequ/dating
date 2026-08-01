package com.dating.owoke.media.storage.configuration;

import java.net.URI;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dating.owoke.media.processing.configuration.MediaProcessingProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({MediaStorageProperties.class, MediaProcessingProperties.class})
public class S3StorageConfiguration {

    @Bean(destroyMethod = "close")
    S3Client mediaS3Client(MediaStorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.pathStyleAccess())
                        .build())
                .build();
    }

    @Bean
    ApplicationRunner mediaBucketInitializer(S3Client client, MediaStorageProperties properties) {
        return arguments -> {
            try {
                client.headBucket(HeadBucketRequest.builder().bucket(properties.bucket()).build());
            } catch (NoSuchBucketException exception) {
                createBucket(client, properties.bucket());
            } catch (S3Exception exception) {
                if (exception.statusCode() != 404) {
                    throw exception;
                }
                createBucket(client, properties.bucket());
            }
        };
    }

    private void createBucket(S3Client client, String bucket) {
        try {
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (BucketAlreadyOwnedByYouException ignored) {
            // Another replica won the startup race.
        }
    }
}
