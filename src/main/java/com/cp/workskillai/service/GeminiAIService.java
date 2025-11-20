package com.cp.workskillai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAIService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final ObjectMapper objectMapper;

    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_TEXT_LENGTH = 3000;
    private static final int CONNECT_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 60000;

    public Map<String, Object> analyzeResume(MultipartFile file) {
        try {
            log.info("Starting Gemini AI analysis for file: {} ({} bytes)", 
                     file.getOriginalFilename(), file.getSize());
            
            validateFile(file);
            
            // Extract text from file
            String resumeText = extractTextFromFile(file);
            log.info("Extracted text length: {} characters", resumeText.length());
            log.debug("Extracted text preview: {}", 
                     resumeText.substring(0, Math.min(200, resumeText.length())));
            
            // Prepare prompt for Gemini
            String prompt = createAnalysisPrompt(resumeText);
            
            // Call Gemini API
            String response = callGeminiAPI(prompt);
            
            // Parse response
            Map<String, Object> result = parseGeminiResponse(response);
            
            log.info("Gemini AI analysis completed successfully. Extracted {} skills, {} education entries, {} experience entries",
                     ((List<?>) result.getOrDefault("skills", List.of())).size(),
                     ((List<?>) result.getOrDefault("education", List.of())).size(),
                     ((List<?>) result.getOrDefault("experience", List.of())).size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Gemini AI analysis failed for file: {}", file.getOriginalFilename(), e);
            return getFallbackAnalysisData();
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size must be less than 5MB");
        }
        
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
        );
        
        if (!allowedTypes.contains(contentType)) {
            throw new RuntimeException("Invalid file type. Only PDF, DOCX, and TXT files are allowed.");
        }
    }

    private String extractTextFromFile(MultipartFile file) throws Exception {
        String contentType = file.getContentType();
        
        if (contentType == null) {
            throw new RuntimeException("Unable to determine file type");
        }

        try (InputStream inputStream = file.getInputStream()) {
            switch (contentType) {
                case "text/plain":
                    return extractTextFromTxt(inputStream);
                    
                case "application/pdf":
                    return extractTextFromPdf(inputStream);
                    
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                    return extractTextFromDocx(inputStream);
                    
                default:
                    throw new RuntimeException("Unsupported file type: " + contentType);
            }
        } catch (Exception e) {
            log.error("Error extracting text from file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to extract text from file: " + e.getMessage());
        }
    }

    private String extractTextFromTxt(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String extractTextFromPdf(InputStream inputStream) throws Exception {
        try (PDDocument document = PDDocument.load(inputStream)) {
            if (document.isEncrypted()) {
                throw new RuntimeException("Encrypted PDF files are not supported");
            }
            
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setSortByPosition(true);
            pdfStripper.setLineSeparator("\n");
            return pdfStripper.getText(document);
        } catch (IOException e) {
            log.error("Error reading PDF file", e);
            throw new RuntimeException("Failed to read PDF file: " + e.getMessage());
        }
    }

    private String extractTextFromDocx(InputStream inputStream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();
            
            // Extract paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paragraphText = paragraph.getText();
                if (paragraphText != null && !paragraphText.trim().isEmpty()) {
                    text.append(paragraphText).append("\n");
                }
            }
            
            // Extract tables
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.trim().isEmpty()) {
                            text.append(cellText).append("\t");
                        }
                    }
                    text.append("\n");
                }
            }
            
            // Extract headers
            for (XWPFHeader header : document.getHeaderList()) {
                for (XWPFParagraph paragraph : header.getParagraphs()) {
                    String headerText = paragraph.getText();
                    if (headerText != null && !headerText.trim().isEmpty()) {
                        text.append(headerText).append("\n");
                    }
                }
            }
            
            return text.toString().trim();
        } catch (IOException e) {
            log.error("Error reading DOCX file", e);
            throw new RuntimeException("Failed to read DOCX file: " + e.getMessage());
        }
    }

    private String createAnalysisPrompt(String resumeText) {
        String truncatedText = resumeText.substring(0, Math.min(resumeText.length(), MAX_TEXT_LENGTH));
        
        return """
            Analyze this resume text and extract structured information. Return ONLY valid JSON.
            
            RESUME TEXT:
            %s
            
            Extract into this exact JSON structure:
            {
              "fullName": "extracted full name",
              "email": "extracted email",
              "contactNumber": "extracted phone number",
              "title": "extracted job title or current position",
              "skills": ["array of technical skills"],
              "certifications": ["array of certification names"],
              "education": [
                {
                  "degree": "degree name",
                  "institution": "institution name", 
                  "year": "graduation year"
                }
              ],
              "experience": [
                {
                  "position": "job position",
                  "company": "company name", 
                  "duration": "employment duration",
                  "description": "job description"
                }
              ],
              "summary": "professional summary"
            }
            
            IMPORTANT RULES:
            - Return ONLY valid JSON, no other text or explanations
            - Use empty strings ("") for missing fields
            - Use empty arrays ([]) for missing arrays
            - Ensure all JSON syntax is correct (proper quotes, brackets, commas)
            - Remove any trailing commas that break JSON parsing
            - If you can't find information, use empty values
            - Clean and format the data properly
            """.formatted(truncatedText);
    }

    private String callGeminiAPI(String prompt) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;
        
        log.info("Calling Gemini API with prompt length: {}", prompt.length());
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setDoOutput(true);
        
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("contents", List.of(
            Map.of("parts", List.of(
                Map.of("text", prompt)
            ))
        ));
        requestBodyMap.put("generationConfig", Map.of(
            "temperature", 0.1,
            "topK", 40,
            "topP", 0.95,
            "maxOutputTokens", 2048
        ));
        
        String requestBody = objectMapper.writeValueAsString(requestBodyMap);
        
        log.debug("Gemini API Request Body prepared");
        
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            String errorResponse = readErrorStream(connection);
            log.error("Gemini API request failed with code: {}. Error: {}", responseCode, errorResponse);
            throw new RuntimeException("Gemini API request failed with code: " + responseCode + ". Error: " + errorResponse);
        }
        
        String response = readInputStream(connection);
        log.debug("Gemini API Response received successfully");
        
        return response;
    }

    private String readInputStream(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String readErrorStream(HttpURLConnection connection) throws IOException {
        InputStream errorStream = connection.getErrorStream();
        if (errorStream == null) {
            return "No error response available";
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                errorResponse.append(line);
            }
            return errorResponse.toString();
        }
    }

    private Map<String, Object> parseGeminiResponse(String response) throws Exception {
        try {
            log.debug("Parsing Gemini API response");
            
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            
            if (candidates == null || candidates.isEmpty()) {
                throw new RuntimeException("No candidates in API response");
            }
            
            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            
            if (content == null) {
                throw new RuntimeException("No content in candidate");
            }
            
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            
            if (parts == null || parts.isEmpty()) {
                throw new RuntimeException("No parts in content");
            }
            
            String text = (String) parts.get(0).get("text");
            
            if (text == null || text.trim().isEmpty()) {
                throw new RuntimeException("Empty response text from Gemini");
            }
            
            log.debug("Raw Gemini response text: {}", text);
            
            // Clean and extract JSON
            String cleanedJson = extractJsonFromText(text);
            log.debug("Cleaned JSON: {}", cleanedJson);
            
            // Parse the JSON
            Map<String, Object> result = objectMapper.readValue(cleanedJson, Map.class);
            
            // Clean and validate the parsed data
            result = cleanParsedData(result);
            
            log.info("Successfully parsed analysis result with {} skills, {} education entries, {} experience entries",
                     ((List<?>) result.getOrDefault("skills", List.of())).size(),
                     ((List<?>) result.getOrDefault("education", List.of())).size(),
                     ((List<?>) result.getOrDefault("experience", List.of())).size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to parse Gemini response. Response: {}", response, e);
            throw new RuntimeException("Failed to parse AI analysis response: " + e.getMessage());
        }
    }

    private String extractJsonFromText(String text) {
        // Remove code blocks and any surrounding text
        String cleaned = text.replaceAll("(?i)```json|```", "").trim();
        
        // Find JSON object boundaries
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}') + 1;
        
        if (start < 0 || end <= start) {
            throw new RuntimeException("No valid JSON object found in response");
        }
        
        String json = cleaned.substring(start, end);
        
        // Clean common JSON issues
        json = cleanJsonString(json);
        
        return json;
    }

    private String cleanJsonString(String json) {
        // Remove trailing commas before closing brackets/braces
        json = json.replaceAll(",(\\s*[}\\])])", "$1");
        
        // Ensure proper string quoting
        json = json.replaceAll("'", "\"");
        
        // Fix unescaped quotes within strings (basic handling)
        json = json.replaceAll("([^\\\\])\"([^\"\\\\]*)\"([^\"])", "$1\"$2\"$3");
        
        return json;
    }

    private Map<String, Object> cleanParsedData(Map<String, Object> parsedData) {
        Map<String, Object> cleaned = new HashMap<>();
        
        // Clean string fields
        cleaned.put("fullName", String.valueOf(parsedData.getOrDefault("fullName", "")).trim());
        cleaned.put("email", String.valueOf(parsedData.getOrDefault("email", "")).trim());
        cleaned.put("contactNumber", String.valueOf(parsedData.getOrDefault("contactNumber", "")).trim());
        cleaned.put("title", String.valueOf(parsedData.getOrDefault("title", "")).trim());
        cleaned.put("summary", String.valueOf(parsedData.getOrDefault("summary", "")).trim());
        
        // Clean arrays
        cleaned.put("skills", cleanStringArray((List<?>) parsedData.getOrDefault("skills", List.of())));
        cleaned.put("certifications", cleanStringArray((List<?>) parsedData.getOrDefault("certifications", List.of())));
        
        // Clean education array
        cleaned.put("education", cleanEducationArray((List<?>) parsedData.getOrDefault("education", List.of())));
        
        // Clean experience array
        cleaned.put("experience", cleanExperienceArray((List<?>) parsedData.getOrDefault("experience", List.of())));
        
        return cleaned;
    }

    private List<String> cleanStringArray(List<?> array) {
        if (array == null) return List.of();
        
        return array.stream()
                .map(item -> String.valueOf(item).trim())
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private List<Map<String, String>> cleanEducationArray(List<?> array) {
        if (array == null) return List.of();
        
        List<Map<String, String>> cleaned = new ArrayList<>();
        
        for (Object item : array) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> eduMap = (Map<String, Object>) item;
                
                Map<String, String> cleanedEdu = new HashMap<>();
                cleanedEdu.put("degree", String.valueOf(eduMap.getOrDefault("degree", "")).trim());
                cleanedEdu.put("institution", String.valueOf(eduMap.getOrDefault("institution", "")).trim());
                cleanedEdu.put("year", String.valueOf(eduMap.getOrDefault("year", "")).trim());
                
                // Only add if there's meaningful data
                if (!cleanedEdu.get("degree").isEmpty() || !cleanedEdu.get("institution").isEmpty()) {
                    cleaned.add(cleanedEdu);
                }
            }
        }
        
        return cleaned;
    }

    private List<Map<String, String>> cleanExperienceArray(List<?> array) {
        if (array == null) return List.of();
        
        List<Map<String, String>> cleaned = new ArrayList<>();
        
        for (Object item : array) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> expMap = (Map<String, Object>) item;
                
                Map<String, String> cleanedExp = new HashMap<>();
                cleanedExp.put("position", String.valueOf(expMap.getOrDefault("position", "")).trim());
                cleanedExp.put("company", String.valueOf(expMap.getOrDefault("company", "")).trim());
                cleanedExp.put("duration", String.valueOf(expMap.getOrDefault("duration", "")).trim());
                cleanedExp.put("description", String.valueOf(expMap.getOrDefault("description", "")).trim());
                
                // Only add if there's meaningful data
                if (!cleanedExp.get("position").isEmpty() || !cleanedExp.get("company").isEmpty()) {
                    cleaned.add(cleanedExp);
                }
            }
        }
        
        return cleaned;
    }

    private Map<String, Object> getFallbackAnalysisData() {
        return Map.of(
            "fullName", "",
            "email", "",
            "contactNumber", "",
            "title", "",
            "skills", List.of(),
            "certifications", List.of(),
            "education", List.of(),
            "experience", List.of(),
            "summary", ""
        );
    }

    // Utility method for testing text extraction
    public Map<String, Object> testTextExtraction(MultipartFile file) {
        try {
            validateFile(file);
            
            String extractedText = extractTextFromFile(file);
            
            return Map.of(
                "success", true,
                "fileName", file.getOriginalFilename(),
                "fileSize", file.getSize(),
                "contentType", file.getContentType(),
                "extractedTextLength", extractedText.length(),
                "extractedTextPreview", extractedText.substring(0, Math.min(500, extractedText.length())),
                "fullText", extractedText
            );
        } catch (Exception e) {
            log.error("Text extraction test failed", e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "fileName", file.getOriginalFilename()
            );
        }
    }
}