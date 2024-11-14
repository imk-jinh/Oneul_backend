package com.oneul.service;

import java.io.IOException;

import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service {
    @Value("${cloud.aws.s3.bucket}")
    String BUCKET_NAME;

    @Autowired
    private S3Client s3Client;

    public String uploadFile(MultipartFile file) {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        try {
            s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(fileName).build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 업로드한 파일의 URL 반환
            return s3Client.utilities().getUrl(builder -> builder.bucket(BUCKET_NAME).key(fileName)).toString();
        } catch (IOException e) {
            throw new RuntimeException("S3에 파일 업로드 실패", e);
        }
    }
}