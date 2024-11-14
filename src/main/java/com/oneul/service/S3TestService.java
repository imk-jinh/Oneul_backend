package com.oneul.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.sql.*;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class S3TestService {
    private final S3Service s3Service;

    public String saveOneImgS3(MultipartFile image) {

        if (image == null || image.isEmpty()) {
            return null;
        }

        String imageUrl = s3Service.uploadFile(image);
        return imageUrl;
    }

    public List<String> saveImgS3(List<MultipartFile> images) {
        List<String> imageUrls = new ArrayList<>();

        if (images == null || images.isEmpty()) {
            return imageUrls;
        }

        for (MultipartFile image : images) {

            String imageUrl = s3Service.uploadFile(image);
            imageUrls.add(imageUrl);
        }

        return imageUrls;
    }
}
