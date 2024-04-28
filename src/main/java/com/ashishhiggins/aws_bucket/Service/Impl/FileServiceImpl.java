package com.ashishhiggins.aws_bucket.Service.Impl;

import com.ashishhiggins.aws_bucket.Service.FileService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.DetectEntitiesRequest;
import software.amazon.awssdk.services.comprehend.model.DetectEntitiesResponse;
import software.amazon.awssdk.services.comprehend.model.Entity;
import software.amazon.awssdk.services.comprehend.model.LanguageCode;
import software.amazon.awssdk.services.comprehendmedical.ComprehendMedicalClient;
import software.amazon.awssdk.services.comprehendmedical.model.DetectEntitiesV2Response;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextResponse;
import software.amazon.awssdk.services.textract.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
@Service
public class FileServiceImpl implements FileService {


    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    @Autowired
    private S3Client s3Client;

    @Autowired
    private TextractClient textractClient;

    @Autowired
    private ComprehendClient comprehendClient;

    @Autowired
    private ComprehendMedicalClient comprehendMedicalClient;

    @Override
    public String saveFile(String personId, MultipartFile file) {

        try {
            String filename = personId + "_" + file.getOriginalFilename();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return filename;


        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save file");
        }

    }

    @Override
    public byte[] downloadFile(String filename) {

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .build();

            ResponseBytes responseBytes = s3Client.getObjectAsBytes(request);
            return responseBytes.asByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to download file");
        }
    }

    @Override
    public String deleteFile(String filename) {

        try {
            s3Client.deleteObject(builder -> builder.bucket(bucketName).key(filename));
            return "File deleted";
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to delete file");
        }

    }

    @Override
    public List<String> listAllFiles() {
        try {
            return s3Client.listObjects(builder -> builder.bucket(bucketName)).contents().stream()
                    .map(s3Object -> s3Object.key())
                    .toList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to list files");
        }
    }

    @Override
    public String extractTextFromImage(String filename) {

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .build();

            ResponseBytes responseBytes = s3Client.getObjectAsBytes(request);

            DetectDocumentTextResponse response = textractClient.detectDocumentText(DetectDocumentTextRequest.builder()
                    .document(Document.builder().bytes(SdkBytes.fromByteArray(responseBytes.asByteArray())).build())
                    .build());

            StringBuilder extractedText = new StringBuilder();
            List<Block> blockss = response.blocks();
            for (Block block : blockss) {
                if (block.blockType().equals(BlockType.LINE)) {
                    extractedText.append(block.text()).append("\n");
                }
            }
            //return extractedText.toString();

//            ObjectMapper objectMapper = new ObjectMapper();
//            ObjectNode jsonResult = objectMapper.createObjectNode();
//
//            ArrayNode textLines = objectMapper.createArrayNode();
//            List<Block> blocks = response.blocks();
//            for (Block block : blocks) {
//                if (block.blockType().equals(BlockType.LINE)) {
//                    textLines.add(block.text());
//                }
//            }
//            jsonResult.set("textLines", textLines);
//
//            return jsonResult.toString();

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode jsonResult = objectMapper.createObjectNode();

            // Add extracted text to the JSON object
            jsonResult.put("user_input", extractedText.toString().trim());

            // Serialize JSON object to string
            String jsonString = objectMapper.writeValueAsString(jsonResult);

            return jsonString;


        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to extract text from image");
        }
    }


    @Override
    public String extractAndAnalyzeMedicalInfo(String filename) {

        String extractedText = extractTextFromImage(filename);
        if (extractedText == null) {
            return null;
        }


        try {
            DetectEntitiesRequest request = DetectEntitiesRequest.builder()
                    .text(extractedText)
                    .languageCode(LanguageCode.EN)
                    .build();

            DetectEntitiesResponse response = comprehendClient.detectEntities(request);

//            StringBuilder entityDetails = new StringBuilder();
//            for (Entity entity : response.entities()) {
//                entityDetails.append("Entity Type: ").append(entity.type()).append("\n");
//                entityDetails.append("Text: ").append(entity.text()).append("\n");
//                // You can add more details based on entity attributes if needed
//                entityDetails.append("\n");
//            }
//
//
//            return entityDetails.toString();

            StringBuilder medicalInfo = new StringBuilder();
            for (Entity entity : response.entities()) {
                // Check if the entity type is relevant to medical information
                if (isMedicalEntity(entity)) {
                    medicalInfo.append("Entity Type: ").append(entity.type()).append("\n");
                    medicalInfo.append("Text: ").append(entity.text()).append("\n");
                    // You can add more details based on entity attributes if needed
                    medicalInfo.append("\n");
                }
            }

            return medicalInfo.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to extract and analyze text entities");
        }

    }

    private boolean isMedicalEntity(Entity entity) {
        // List of medical entity types you want to include
        List<String> medicalEntityTypes = Arrays.asList("MEDICATION", "SYMPTOM", "DIAGNOSIS");
        return medicalEntityTypes.contains(entity.type());
    }

    public String generate(String filename) {
        try {
            // Simulate text extraction from image
            String extractedText = extractTextFromImage(filename);

            // Create JSON object to send
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonResult = objectMapper.createObjectNode().put("user_input", extractedText);
            String jsonString = objectMapper.writeValueAsString(jsonResult);

            // URL of the Flask app
            URL url = new URL("http://localhost:5000/ask");

            // Open connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set request method to POST
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Write JSON data to the connection's output stream
            try (OutputStream outputStream = connection.getOutputStream()) {
                byte[] input = jsonString.getBytes("utf-8");
                outputStream.write(input, 0, input.length);
            }

            // Read response from the server
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            // Get response code (optional - useful for debugging)
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Close connection
            connection.disconnect();

            // Return the response from the server
            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null; // Or handle the error appropriately
        }
    }
}




//    @Override
//    public String extractAndAnalyzeMedicalInfo(String filename) {
//        try {
//            // Retrieve object (medical document) from S3
//            GetObjectResponse response = s3Client.getObject(GetObjectRequest.builder()
//                    .bucket("your-bucket-name")
//                    .key(filename)
//                    .build()).response();
//
//            // Extract text from the medical document using Textract
////            DetectDocumentTextResponse textResponse = textractClient.detectDocumentText(
////                    DetectDocumentTextRequest.builder()
////                            .document(
////                                    software.amazon.awssdk.services.textract.model.Document.builder()
////                                            .bytes(response.readAllBytes())
////                                            .build()
////                            )
////                            .build()
//            );
//            ResponseBytes responseBytes = s3Client.getObjectAsBytes(request);
//
//
//            DetectDocumentTextResponse response = textractClient.detectDocumentText(DetectDocumentTextRequest.builder()
//                    .document(Document.builder().bytes(SdkBytes.fromByteArray(responseBytes.asByteArray())).build())
//                    .build());
//
//
//            // Extracted text
//            String extractedText = textResponse.blocks().stream()
//                    .filter(block -> block.blockType() == software.amazon.awssdk.services.textract.model.BlockType.LINE)
//                    .map(software.amazon.awssdk.services.textract.model.Block::text)
//                    .reduce("", (a, b) -> a + "\n" + b);
//
//            // Analyze medical entities using Comprehend Medical
//            DetectEntitiesResponse entitiesResponse = comprehendMedicalClient.detectEntities(
//                    DetectEntitiesRequest.builder()
//                            .text(extractedText)
//                            .build()
//            );
//
//            // Process and return the result (e.g., JSON string)
//            return entitiesResponse.entities().toString();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Failed to extract and analyze medical information");
//        }
//    }



