package com.cp.workskillai.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ResumeUploadRequest {
    private MultipartFile file;
    private String fileName;
    private String fileType;
    private Long fileSize;
}