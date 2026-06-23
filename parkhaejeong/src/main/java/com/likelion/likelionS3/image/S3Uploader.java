package com.likelion.likelionS3.image;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor    // final 필드 생성자 자동 생성 (Lombok)
public class S3Uploader {

    private final AmazonS3 amazonS3;  // S3Config에서 만든 빈 자동 주입

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;            // application.yml에서 버킷명 주입

    public String upload(MultipartFile file) throws IOException {

        // ① 중복 방지 파일명 생성
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // ② S3에 전달할 파일 정보 세팅
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        // ③ 실제 업로드
        amazonS3.putObject(bucket, fileName, file.getInputStream(), metadata);

        // ④ 업로드된 파일 URL 반환
        return amazonS3.getUrl(bucket, fileName).toString();
    }

    // url 삭제
    public void deleteFile (String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // URL에서 맨 마지막 '/' 다음 이미지 이름 추출
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            // S3 버킷에서 해당 파일명 삭제
            amazonS3.deleteObject(bucket, fileName);
        } catch (Exception e) {
            // 삭제 실패 시 에러 로그
            System.err.println("S3 파일 삭제 실패: " + e.getMessage());
        }
    }
}