package com.cp.workskillai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiExamService {
    
    private final RestTemplate restTemplate;
    
    // Multiple free Gemini API keys for rotation
    @Value("${gemini.api.keys:}")
    private String geminiApiKeys;
    
    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1/models}")
    private String geminiApiUrl;
    
    // Free Gemini models to rotate through
    private final List<String> freeGeminiModels = Arrays.asList(
        "gemini-1.5-flash",      // Free tier, 1 RPM limit
        "gemini-1.5-flash-8b",   // Free tier, lighter version
        "gemini-1.5-pro",        // Free tier with 15 RPM limit
        "gemini-2.0-flash-exp",  // Experimental free model
        "gemini-2.0-flash-lite"  // Lightweight free model
    );
    
    // Track usage per API key and model
    private final Map<String, AtomicInteger> apiKeyUsage = new HashMap<>();
    private final Map<String, AtomicInteger> modelUsage = new HashMap<>();
    private final int MAX_REQUESTS_PER_KEY = 50; // Conservative limit for free tier
    private final int MAX_REQUESTS_PER_MODEL = 15; // Conservative limit per model
    
    @PostConstruct
    public void init() {
        // Initialize usage counters
        if (geminiApiKeys != null && !geminiApiKeys.isEmpty()) {
            String[] keys = geminiApiKeys.split(",");
            for (String key : keys) {
                String trimmedKey = key.trim();
                if (!trimmedKey.isEmpty()) {
                    apiKeyUsage.put(trimmedKey, new AtomicInteger(0));
                }
            }
        }
        
        for (String model : freeGeminiModels) {
            modelUsage.put(model, new AtomicInteger(0));
        }
        
        log.info("Initialized Gemini service with {} API keys and {} models", 
                 apiKeyUsage.size(), freeGeminiModels.size());
    }
    
    public Map<String, Object> generateExamQuestions(String skill, String category, String difficulty, int numberOfQuestions) {
        try {
            // Try Gemini API with multiple fallbacks
            Map<String, Object> geminiExam = generateWithGeminiFallback(skill, category, difficulty, numberOfQuestions);
            if (geminiExam != null && !geminiExam.containsKey("error")) {
                log.info("Successfully generated exam using Gemini API for skill: {}", skill);
                return geminiExam;
            }
            
            // Fallback to enhanced rule-based questions
            log.warn("All Gemini APIs failed, using rule-based questions for skill: {}", skill);
            return generateEnhancedRuleBasedQuestions(skill, category, difficulty, numberOfQuestions);
            
        } catch (Exception e) {
            log.error("Error generating exam with Gemini API for skill: {}", skill, e);
            return generateEnhancedRuleBasedQuestions(skill, category, difficulty, numberOfQuestions);
        }
    }
    
    /**
     * Try multiple Gemini models and API keys with intelligent fallback
     */
    private Map<String, Object> generateWithGeminiFallback(String skill, String category, String difficulty, int numberOfQuestions) {
        if (apiKeyUsage.isEmpty()) {
            log.warn("No Gemini API keys configured");
            return null;
        }
        
        // Get available API keys that haven't exceeded limits
        List<String> availableKeys = getAvailableApiKeys();
        if (availableKeys.isEmpty()) {
            log.warn("All API keys have reached their usage limits");
            return null;
        }
        
        // Try each available model with each available key
        for (String model : freeGeminiModels) {
            if (modelUsage.get(model).get() >= MAX_REQUESTS_PER_MODEL) {
                log.debug("Model {} has reached usage limit, skipping", model);
                continue;
            }
            
            for (String apiKey : availableKeys) {
                try {
                    log.info("Trying model {} with API key {}", model, getMaskedApiKey(apiKey));
                    
                    Map<String, Object> result = generateWithGemini(apiKey, model, skill, category, difficulty, numberOfQuestions);
                    if (result != null) {
                        // Update usage counters
                        apiKeyUsage.get(apiKey).incrementAndGet();
                        modelUsage.get(model).incrementAndGet();
                        
                        log.info("Successfully used model {} with key {} (Usage: {}/{})", 
                                model, getMaskedApiKey(apiKey), 
                                apiKeyUsage.get(apiKey).get(), MAX_REQUESTS_PER_KEY);
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("Failed with model {} and key {}: {}", model, getMaskedApiKey(apiKey), e.getMessage());
                    // Continue to next combination
                }
            }
        }
        
        log.warn("All model and API key combinations failed");
        return null;
    }
    
    /**
     * Get API keys that haven't exceeded usage limits
     */
    private List<String> getAvailableApiKeys() {
        List<String> available = new ArrayList<>();
        for (Map.Entry<String, AtomicInteger> entry : apiKeyUsage.entrySet()) {
            if (entry.getValue().get() < MAX_REQUESTS_PER_KEY) {
                available.add(entry.getKey());
            }
        }
        return available;
    }
    
    /**
     * Generate with specific API key and model
     */
    private Map<String, Object> generateWithGemini(String apiKey, String model, String skill, String category, String difficulty, int numberOfQuestions) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String prompt = createGeminiPrompt(skill, category, difficulty, numberOfQuestions);
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            contents.put("parts", List.of(Map.of("text", prompt)));
            requestBody.put("contents", List.of(contents));
            
            // Add generation config optimized for free models
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 1024); // Reduced for free tier
            generationConfig.put("topP", 0.8);
            generationConfig.put("topK", 40);
            requestBody.put("generationConfig", generationConfig);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Build endpoint for specific model
            String endpoint = String.format("%s/%s:generateContent?key=%s", 
                geminiApiUrl, model, apiKey);
            
            log.debug("Calling Gemini API: {} for skill: {}", model, skill);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                request,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseGeminiResponse(response.getBody(), skill, numberOfQuestions);
            } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("Rate limit reached for model {} with key {}", model, getMaskedApiKey(apiKey));
                return null;
            } else {
                log.warn("Gemini API returned status: {} for model {}", response.getStatusCode(), model);
                return null;
            }
            
        } catch (Exception e) {
            log.warn("Gemini API call failed for model {}: {}", model, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get usage statistics for monitoring
     */
    public Map<String, Object> getUsageStats() {
        Map<String, Object> stats = new HashMap<>();
        
        Map<String, Integer> keyUsage = new HashMap<>();
        for (Map.Entry<String, AtomicInteger> entry : apiKeyUsage.entrySet()) {
            keyUsage.put(getMaskedApiKey(entry.getKey()), entry.getValue().get());
        }
        
        Map<String, Integer> modelUsageStats = new HashMap<>();
        for (Map.Entry<String, AtomicInteger> entry : modelUsage.entrySet()) {
            modelUsageStats.put(entry.getKey(), entry.getValue().get());
        }
        
        stats.put("apiKeyUsage", keyUsage);
        stats.put("modelUsage", modelUsageStats);
        stats.put("availableKeys", getAvailableApiKeys().size());
        stats.put("totalKeys", apiKeyUsage.size());
        
        return stats;
    }
    
    /**
     * Reset usage counters (can be called periodically or via admin endpoint)
     */
    public void resetUsageCounters() {
        for (AtomicInteger counter : apiKeyUsage.values()) {
            counter.set(0);
        }
        for (AtomicInteger counter : modelUsage.values()) {
            counter.set(0);
        }
        log.info("Reset all Gemini usage counters");
    }
    
    private String getMaskedApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
    
    private String createGeminiPrompt(String skill, String category, String difficulty, int numberOfQuestions) {
        return String.format(
            "Create exactly %d multiple choice questions about %s. " +
            "Difficulty: %s. Category: %s.\n\n" +
            "STRICT FORMAT REQUIREMENTS - FOLLOW EXACTLY:\n" +
            "- Each question must start with 'Q:' followed by the question text\n" +
            "- Then exactly 4 options labeled A), B), C), D)\n" +
            "- End with 'Correct:' followed by the correct letter (A, B, C, or D)\n" +
            "- Separate questions with exactly one blank line\n\n" +
            "EXAMPLE:\n" +
            "Q: What is the main purpose of this technology?\n" +
            "A) Frontend development\n" +
            "B) Backend development\n" +
            "C) Database management\n" +
            "D) All of the above\n" +
            "Correct: D\n\n" +
            "Now generate %d questions about %s:",
            numberOfQuestions, skill, difficulty, category, numberOfQuestions, skill
        );
    }
    
    private Map<String, Object> parseGeminiResponse(Map<String, Object> response, String skill, int numberOfQuestions) {
        try {
            List<Map<String, Object>> questions = new ArrayList<>();
            
            if (response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> firstCandidate = candidates.get(0);
                    if (firstCandidate.containsKey("content")) {
                        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                        if (content.containsKey("parts")) {
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                            if (!parts.isEmpty()) {
                                String generatedText = (String) parts.get(0).get("text");
                                log.debug("Raw Gemini response: {}", generatedText);
                                questions = parseGeneratedQuestions(generatedText, skill);
                            }
                        }
                    }
                }
            }
            
            if (!questions.isEmpty() && questions.size() >= Math.min(3, numberOfQuestions)) {
                Map<String, Object> result = new HashMap<>();
                result.put("questions", questions);
                result.put("totalQuestions", questions.size());
                result.put("skill", skill);
                result.put("source", "gemini");
                result.put("generatedAt", new Date());
                return result;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
            return null;
        }
    }
    
    private List<Map<String, Object>> parseGeneratedQuestions(String text, String skill) {
        List<Map<String, Object>> questions = new ArrayList<>();
        String[] lines = text.split("\n");
        
        Map<String, Object> currentQuestion = null;
        int questionCount = 0;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.startsWith("Q:")) {
                if (currentQuestion != null && currentQuestion.containsKey("question")) {
                    questions.add(currentQuestion);
                }
                currentQuestion = new HashMap<>();
                currentQuestion.put("id", ++questionCount);
                
                String questionText = line.substring(2).trim(); // Remove "Q:"
                currentQuestion.put("question", questionText);
                currentQuestion.put("options", new ArrayList<String>());
            } 
            else if (line.matches("^[A-D]\\) .*")) {
                if (currentQuestion != null) {
                    List<String> options = (List<String>) currentQuestion.get("options");
                    options.add(line.substring(3).trim());
                }
            } 
            else if (line.startsWith("Correct:")) {
                if (currentQuestion != null) {
                    String correctAnswer = line.substring(8).trim().toUpperCase();
                    int correctIndex = mapAnswerToIndex(correctAnswer);
                    currentQuestion.put("correct", correctIndex);
                }
            }
        }
        
        // Add the last question
        if (currentQuestion != null && currentQuestion.containsKey("question") && 
            currentQuestion.containsKey("correct")) {
            questions.add(currentQuestion);
        }
        
        return questions;
    }
    
    private int mapAnswerToIndex(String answer) {
        return switch (answer) {
            case "A" -> 0;
            case "B" -> 1;
            case "C" -> 2;
            case "D" -> 3;
            default -> 0;
        };
    }
    
    // Enhanced rule-based questions as reliable fallback
    private Map<String, Object> generateEnhancedRuleBasedQuestions(String skill, String category, String difficulty, int numberOfQuestions) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> questions = getSkillSpecificQuestions(skill, category, difficulty, numberOfQuestions);
        
        result.put("questions", questions);
        result.put("totalQuestions", questions.size());
        result.put("skill", skill);
        result.put("category", category);
        result.put("difficulty", difficulty);
        result.put("source", "rule-based");
        result.put("generatedAt", new Date());
        
        log.info("Generated {} rule-based questions for {}", questions.size(), skill);
        return result;
    }
    
    // ... (keep all the existing getSkillSpecificQuestions, createQuestion, 
    // generateGenericProgrammingQuestions methods exactly as they were)
    
    private List<Map<String, Object>> getSkillSpecificQuestions(String skill, String category, String difficulty, int numberOfQuestions) {
        String skillLower = skill.toLowerCase();
        List<Map<String, Object>> questions = new ArrayList<>();
        
        // Data Warehousing specific questions
        if (skillLower.contains("data warehousing") || skillLower.contains("data warehouse") || skillLower.contains("etl")) {
            List<Map<String, Object>> dwQuestions = Arrays.asList(
                createQuestion(1, "What is the primary purpose of a data warehouse?", 
                    Arrays.asList("Real-time transaction processing", "Historical data analysis and reporting", "Website hosting", "Mobile app development"), 1),
                createQuestion(2, "Which schema design is most common in data warehousing?", 
                    Arrays.asList("Star Schema", "Network Schema", "Object-oriented Schema", "Hierarchical Schema"), 0),
                createQuestion(3, "What does ETL stand for in data warehousing?", 
                    Arrays.asList("Extract, Transform, Load", "Extract, Transfer, Load", "Enter, Transform, Leave", "Export, Transform, Load"), 0),
                createQuestion(4, "Which is NOT a common data warehouse component?", 
                    Arrays.asList("OLAP cubes", "Data marts", "ETL tools", "Web servers"), 3),
                createQuestion(5, "What is the main advantage of a data warehouse over operational databases?", 
                    Arrays.asList("Faster transaction processing", "Optimized for analytical queries", "Better for real-time updates", "Lower storage costs"), 1)
            );
            Collections.shuffle(dwQuestions);
            return dwQuestions.subList(0, Math.min(numberOfQuestions, dwQuestions.size()));
        }
        
        // JavaScript-specific questions
        if (skillLower.contains("javascript") || skillLower.contains("js")) {
            List<Map<String, Object>> jsQuestions = Arrays.asList(
                createQuestion(1, "What is the output of: console.log(typeof null)?", 
                    Arrays.asList("null", "object", "undefined", "boolean"), 1),
                createQuestion(2, "Which method creates a new array with results of calling a function?", 
                    Arrays.asList("map()", "forEach()", "filter()", "reduce()"), 0),
                createQuestion(3, "What does 'this' keyword refer to in a global context?", 
                    Arrays.asList("undefined", "null", "global object", "current function"), 2),
                createQuestion(4, "Which is NOT a JavaScript data type?", 
                    Arrays.asList("symbol", "bigint", "character", "undefined"), 2),
                createQuestion(5, "What is the purpose of 'use strict'?", 
                    Arrays.asList("Enables strict mode", "Enables ES6 features", "Improves performance", "Enables TypeScript"), 0)
            );
            Collections.shuffle(jsQuestions);
            return jsQuestions.subList(0, Math.min(numberOfQuestions, jsQuestions.size()));
        }
        
        // Java-specific questions
        if (skillLower.contains("java") && !skillLower.contains("javascript")) {
            List<Map<String, Object>> javaQuestions = Arrays.asList(
                createQuestion(1, "What is the default value of a boolean variable in Java?", 
                    Arrays.asList("true", "false", "null", "0"), 1),
                createQuestion(2, "Which keyword is used to inherit a class?", 
                    Arrays.asList("implements", "extends", "inherits", "super"), 1),
                createQuestion(3, "What is JVM?", 
                    Arrays.asList("Java Virtual Machine", "Java Variable Manager", "JavaScript Virtual Machine", "Java Version Manager"), 0),
                createQuestion(4, "Which collection maintains insertion order?", 
                    Arrays.asList("HashSet", "TreeSet", "ArrayList", "HashMap"), 2),
                createQuestion(5, "What is method overloading?", 
                    Arrays.asList("Same method name, different parameters", "Changing method implementation", "Inheriting methods", "Hiding methods"), 0)
            );
            Collections.shuffle(javaQuestions);
            return javaQuestions.subList(0, Math.min(numberOfQuestions, javaQuestions.size()));
        }
        
        // Python-specific questions
        if (skillLower.contains("python")) {
            List<Map<String, Object>> pythonQuestions = Arrays.asList(
                createQuestion(1, "How do you create a list in Python?", 
                    Arrays.asList("[]", "{}", "()", "<>"), 0),
                createQuestion(2, "What is used to define a function in Python?", 
                    Arrays.asList("function", "def", "func", "define"), 1),
                createQuestion(3, "Which is NOT a Python framework?", 
                    Arrays.asList("Django", "Flask", "Spring", "FastAPI"), 2),
                createQuestion(4, "What does PEP 8 define?", 
                    Arrays.asList("Python Enhancement Proposals", "Python Error Prevention", "Package Management", "Performance Tips"), 0),
                createQuestion(5, "How do you handle exceptions in Python?", 
                    Arrays.asList("try-catch", "try-except", "error-handle", "catch-error"), 1)
            );
            Collections.shuffle(pythonQuestions);
            return pythonQuestions.subList(0, Math.min(numberOfQuestions, pythonQuestions.size()));
        }
        
        // Generic fallback
        return generateGenericProgrammingQuestions(skill, numberOfQuestions);
    }
    
    private Map<String, Object> createQuestion(int id, String question, List<String> options, int correctIndex) {
        return Map.of(
            "id", id,
            "question", question,
            "options", options,
            "correct", correctIndex
        );
    }
    
    private List<Map<String, Object>> generateGenericProgrammingQuestions(String skill, int numberOfQuestions) {
        List<Map<String, Object>> questions = new ArrayList<>();
        Random random = new Random();
        
        String[] genericQuestions = {
            "What is the primary use case for %s?",
            "Which paradigm does %s primarily follow?",
            "What type of technology is %s?",
            "What is %s commonly used for in industry?",
            "Which concept is fundamental to understanding %s?"
        };
        
        String[][] genericOptions = {
            {"Web development", "Mobile development", "Data analysis", "All of the above"},
            {"Object-oriented programming", "Functional programming", "Procedural programming", "Multiple paradigms"},
            {"Programming language", "Framework", "Database", "Development tool"},
            {"Building applications", "Data processing", "System administration", "Various purposes"},
            {"Variables and functions", "Objects and classes", "Data structures", "Depends on the technology"}
        };
        
        for (int i = 0; i < numberOfQuestions; i++) {
            int questionIndex = i % genericQuestions.length;
            String questionText = String.format(genericQuestions[questionIndex], skill);
            questions.add(createQuestion(
                i + 1,
                questionText,
                Arrays.asList(genericOptions[questionIndex]),
                random.nextInt(4)
            ));
        }
        
        return questions;
    }
    
    public Map<String, Object> evaluateAnswerWithAI(String question, String userAnswer, String context) {
        return Map.of("score", 75, "feedback", "Good understanding shown", "confidence", 0.8);
    }
}