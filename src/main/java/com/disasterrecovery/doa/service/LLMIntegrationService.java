package com.disasterrecovery.doa.service;

import com.disasterrecovery.doa.controller.dto.DisasterDataDTO;
import com.disasterrecovery.doa.controller.dto.LLMResponseDTO;
import com.disasterrecovery.exception.LLMServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to interact with LLM API for generating disaster summaries
 */
@Service
public class LLMIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(LLMIntegrationService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${llm.api.url:https://api.llm.provider.com/v1/chat}")
    private String llmApiUrl;

    @Value("${llm.api.key:your-api-key}")
    private String llmApiKey;

    @Value("${llm.model:gpt-4}")
    private String llmModel;

    @Value("${llm.max.retries:3}")
    private int maxRetries;

    /**
     * Main method to generate disaster summary using LLM
     */
    public LLMResponseDTO generateDisasterSummary(DisasterDataDTO disasterData) {
        logger.info("Generating disaster summary for Report ID: {}", disasterData.getReportId());

        try {
            // Build the prompt string
            String promptString = buildPromptString(disasterData);
            logger.debug("Built prompt string for LLM: {}", promptString);

            // Call LLM API with retry logic
            String llmResponse = callLLMWithRetry(promptString);

            // Parse LLM response
            LLMResponseDTO responseDTO = parseLLMResponse(llmResponse);
            responseDTO.setRawResponse(llmResponse);
            responseDTO.setSuccess(true);

            logger.info("Successfully generated disaster summary for Report ID: {}", disasterData.getReportId());
            return responseDTO;

        } catch (Exception e) {
            logger.error("Failed to generate disaster summary for Report ID: {}", disasterData.getReportId(), e);
            return new LLMResponseDTO(false, "LLM service failed: " + e.getMessage());
        }
    }

    /**
     * Build comprehensive prompt string for LLM
     */
    private String buildPromptString(DisasterDataDTO data) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Generate a comprehensive disaster summary report based on the following information:\n\n");

        // Date and Time
        prompt.append("**Date and Time:** ");
        if (data.getDateTime() != null) {
            prompt.append(data.getDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        }
        prompt.append("\n");

        // Disaster Type
        prompt.append("**Disaster Type:** ").append(data.getDisasterType()).append("\n");

        // Location
        prompt.append("**Location:** ").append(data.getLocation());
        if (data.getEirCode() != null) {
            prompt.append(" (EIR Code: ").append(data.getEirCode()).append(")");
        }
        if (data.getLatitude() != null && data.getLongitude() != null) {
            prompt.append(" - Coordinates: [").append(data.getLatitude())
                    .append(", ").append(data.getLongitude()).append("]");
        }
        prompt.append("\n\n");

        // Severity and Scale
        prompt.append("**Severity Level:** ").append(data.getSeverity()).append("/10\n");
        prompt.append("**Scale:** ").append(data.getScale()).append("\n\n");

        // Resources Deployed
        prompt.append("**Resources Deployed:**\n");
        prompt.append("- Number of Vehicles: ").append(data.getNumberOfVehicles()).append("\n");
        prompt.append("- Food Supplies: ").append(data.getFoodSupplies()).append("\n");
        prompt.append("- Medical Kits: ").append(data.getMedicalKits()).append("\n");
        prompt.append("- Other Resources: ").append(data.getOtherResources()).append("\n\n");

        // Impact Information
        prompt.append("**Impact Assessment:**\n");
        prompt.append("- Number of Casualties: ").append(data.getNumberOfCasualties()).append("\n");
        prompt.append("- Damage Assessment: ").append(data.getDamageAssessment()).append("\n\n");

        // Evacuation Information
        prompt.append("**Evacuation Details:**\n");
        prompt.append("- Evacuation Action (Authority Report): ").append(data.getEvacuationAction()).append("\n");
        prompt.append("- Evacuation Routes: ").append(data.getEvacuationRoutes()).append("\n\n");

        // Impact Analysis
        prompt.append("**Impact Analysis:**\n");
        prompt.append("- Before Impact: ").append(data.getBeforeImpact()).append("\n");
        prompt.append("- After Impact: ").append(data.getAfterImpact()).append("\n");
        prompt.append("- Permanent Impact: ").append(data.getPermanentImpact()).append("\n");
        prompt.append("- Temporary Impact: ").append(data.getTemporaryImpact()).append("\n");
        prompt.append("- Effect on Normal Life: ").append(data.getEffectOnNormalLife()).append("\n\n");

        // Additional Details
        if (data.getAdditionalDetails() != null) {
            prompt.append("**Additional Details:** ").append(data.getAdditionalDetails()).append("\n\n");
        }

        // Social Media Data
        if (data.getSocialMediaData() != null && !data.getSocialMediaData().isEmpty()) {
            prompt.append("**Social Media Insights:** ").append(data.getSocialMediaData().toString()).append("\n\n");
        }

        // Instructions for LLM
        prompt.append("\n**INSTRUCTIONS:**\n");
        prompt.append("Based on the above information, generate TWO separate reports:\n\n");
        prompt.append("1. **AUTHORITY REPORT**: A detailed, comprehensive report including:\n");
        prompt.append("   - Complete incident timeline\n");
        prompt.append("   - Detailed resource utilization\n");
        prompt.append("   - Response activities and operational data\n");
        prompt.append("   - Lessons learned and recommendations\n");
        prompt.append("   - Any error logs or issues encountered\n\n");
        prompt.append("2. **CIVILIAN REPORT**: A simplified, public-friendly version including:\n");
        prompt.append("   - Basic incident overview\n");
        prompt.append("   - Safety information and precautions\n");
        prompt.append("   - Impact on daily life\n");
        prompt.append("   - Recovery timeline (if applicable)\n");
        prompt.append("   - Contact information for support\n\n");
        prompt.append("Please format the response as JSON with keys 'authoritySummary' and 'civilianSummary'.");

        return prompt.toString();
    }

    /**
     * Call LLM API with retry logic
     */
    private String callLLMWithRetry(String prompt) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                attempt++;
                logger.info("Attempting LLM API call (Attempt {}/{})", attempt, maxRetries);
                return callLLMAPI(prompt);

            } catch (Exception e) {
                lastException = e;
                logger.warn("LLM API call failed (Attempt {}/{}): {}", attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        // Exponential backoff: 2s, 4s, 8s
                        Thread.sleep((long) Math.pow(2, attempt) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new LLMServiceException("LLM API call interrupted", ie);
                    }
                }
            }
        }

        logger.error("LLM API call failed after {} attempts", maxRetries);
        throw new LLMServiceException("LLM API call failed after " + maxRetries + " attempts", lastException);
    }

    /**
     * Make actual API call to LLM
     */
    private String callLLMAPI(String prompt) {
        try {
            // Prepare request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + llmApiKey);

            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", llmModel);
            requestBody.put("messages", new Object[]{
                    Map.of("role", "user", "content", prompt)
            });
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                    llmApiUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("LLM API call successful");
                return response.getBody();
            } else {
                throw new LLMServiceException("LLM API returned status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Error calling LLM API", e);
            throw new LLMServiceException("LLM API call failed", e);
        }
    }

    /**
     * Parse LLM response to extract authority and civilian summaries
     */
    private LLMResponseDTO parseLLMResponse(String llmResponse) {
        try {
            // Parse JSON response from LLM
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(llmResponse, Map.class);

            // Extract the content from the response
            // Adjust based on actual LLM API response structure
            String content = extractContentFromResponse(responseMap);

            // Parse the content to extract authority and civilian summaries
            @SuppressWarnings("unchecked")
            Map<String, String> summaries = objectMapper.readValue(content, Map.class);

            String authoritySummary = summaries.get("authoritySummary");
            String civilianSummary = summaries.get("civilianSummary");

            return new LLMResponseDTO(authoritySummary, civilianSummary);

        } catch (Exception e) {
            logger.error("Error parsing LLM response", e);
            throw new LLMServiceException("Failed to parse LLM response", e);
        }
    }

    /**
     * Extract content from LLM API response
     * Adjust this method based on actual API response structure
     */
    private String extractContentFromResponse(Map<String, Object> responseMap) {
        // Example structure for OpenAI API:
        // response.choices[0].message.content

        try {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> choices =
                    (java.util.List<Map<String, Object>>) responseMap.get("choices");

            if (choices != null && !choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }

            throw new LLMServiceException("Invalid LLM response structure");

        } catch (Exception e) {
            logger.error("Error extracting content from LLM response", e);
            throw new LLMServiceException("Failed to extract content from LLM response", e);
        }
    }
}

---------------------------------------------------------------------------
