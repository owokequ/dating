package com.dating.owoke.media.storage.adapter;

import org.springframework.stereotype.Component;

import com.dating.owoke.media.storage.configuration.MediaStorageProperties;
import com.dating.owoke.media.storage.port.ObjectStorage;
import com.dating.owoke.media.storage.port.StoredObject;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3ObjectStorage implements ObjectStorage {

    private final S3Client client;
    private final String bucket;

    public S3ObjectStorage(S3Client client, MediaStorageProperties properties) {
        this.client = client;
        this.bucket = properties.bucket();
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength((long) content.length)
                .build();
        client.putObject(request, RequestBody.fromBytes(content));
    }

    @Override
    public StoredObject get(String key) {
        ResponseBytes<GetObjectResponse> response = client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(key).build());
        return new StoredObject(
                response.asByteArray(),
                response.response().contentType(),
                response.response().eTag());
    }

    @Override
    public void delete(String key) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
