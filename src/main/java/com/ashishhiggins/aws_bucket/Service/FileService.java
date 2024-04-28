package com.ashishhiggins.aws_bucket.Service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    String saveFile(String personId, MultipartFile file);

    byte[] downloadFile(String filename);

    String deleteFile(String filename);

    List<String> listAllFiles();

    String extractTextFromImage(String filename);

    String extractAndAnalyzeMedicalInfo(String filename);
    String generate(String filename);
}
