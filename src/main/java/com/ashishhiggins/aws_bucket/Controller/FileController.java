package com.ashishhiggins.aws_bucket.Controller;

import com.ashishhiggins.aws_bucket.Service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;


    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("personId") String personId, @RequestParam("file") MultipartFile file) {
        try {
            String fileName = fileService.saveFile(personId, file);
            return ResponseEntity.ok().body("File uploaded successfully: " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<?> downloadFile(@PathVariable("filename") String filename) {
        try {
            byte[] data = fileService.downloadFile(filename);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }
    }

    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable("filename") String filename) {
        try {
            String message = fileService.deleteFile(filename);
            return ResponseEntity.ok().body(message);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete file");
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> listAllFiles() {
        try {
            List<String> files = fileService.listAllFiles();
            return ResponseEntity.ok().body(files);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{filename}")
    public ResponseEntity<String> extractTextFromImage(@PathVariable("filename") String filename) {
        try {
            String extractedText = fileService.extractTextFromImage(filename);
            return ResponseEntity.ok(extractedText);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to extract text from image");
        }
    }

    @PostMapping("/extractAndAnalyze/{filename}")
    public String extractAndAnalyzeMedicalInfo(@PathVariable String filename) {
        return fileService.generate(filename);
    }
}
