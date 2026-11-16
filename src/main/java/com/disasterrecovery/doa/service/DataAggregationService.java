package com.disasterrecovery.doa.service;

import com.disasterrecovery.doa.controller.dto.DisasterDataDTO;
import com.disasterrecovery.repository.ReportDisasterRepository;
import com.disasterrecovery.entities.DisasterReport;
import com.disasterrecovery.exception.DataAggregationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service to aggregate disaster data from multiple sources:
 * - Database (DisasterReport)
 * - External APIs (Social Media, Weather, etc.)
 * - Resource allocation databases
 */
@Service
public class DataAggregationService {

    private static final Logger logger = LoggerFactory.getLogger(DataAggregationService.class);

    @Autowired
    private ReportDisasterRepository disasterReportRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${external.api.socialmedia.url:https://api.socialmedia.com}")
    private String socialMediaApiUrl;

    @Value("${external.api.weather.url:https://api.weather.com}")
    private String weatherApiUrl;

    @Value("${external.api.resources.url:https://api.resources.com}")
    private String resourcesApiUrl;

    /**
     * Main method to aggregate data from all sources
     * Implements exception handling - continues even if one source fails
     */
    public DisasterDataDTO aggregateDisasterData(Integer reportId) {
        logger.info("Starting data aggregation for Report ID: {}", reportId);

        DisasterDataDTO dataDTO = new DisasterDataDTO();
        Map<String, String> errors = new HashMap<>();

        // 1. Fetch data from Database (Primary source - must succeed)
        try {
            DisasterReport report = fetchFromDatabase(reportId);
            populateFromDisasterReport(dataDTO, report);
            logger.info("Successfully fetched data from database for Report ID: {}", reportId);
        } catch (Exception e) {
            logger.error("CRITICAL: Failed to fetch data from database for Report ID: {}", reportId, e);
            throw new DataAggregationException("Failed to fetch disaster report from database", "DATABASE", e);
        }

        // 2. Fetch data from Social Media API (Optional - log error and continue)
        try {
            Map<String, Object> socialMediaData = fetchFromSocialMediaAPI(reportId);
            dataDTO.setSocialMediaData(socialMediaData);
            logger.info("Successfully fetched data from Social Media API for Report ID: {}", reportId);
        } catch (Exception e) {
            logger.warn("Failed to fetch data from Social Media API for Report ID: {}. Continuing without social media data.", reportId, e);
            errors.put("SOCIAL_MEDIA_API", e.getMessage());
        }

        // 3. Fetch data from Weather/External APIs (Optional - log error and continue)
        try {
            Map<String, Object> externalData = fetchFromExternalAPIs(reportId, dataDTO.getLatitude(), dataDTO.getLongitude());
            dataDTO.setExternalApiData(externalData);
            logger.info("Successfully fetched data from External APIs for Report ID: {}", reportId);
        } catch (Exception e) {
            logger.warn("Failed to fetch data from External APIs for Report ID: {}. Continuing without external data.", reportId, e);
            errors.put("EXTERNAL_API", e.getMessage());
        }

        // 4. Fetch resource allocation data (Optional - log error and continue)
        try {
            enrichWithResourceData(dataDTO, reportId);
            logger.info("Successfully enriched with resource data for Report ID: {}", reportId);
        } catch (Exception e) {
            logger.warn("Failed to fetch resource data for Report ID: {}. Continuing without resource data.", reportId, e);
            errors.put("RESOURCE_API", e.getMessage());
        }

        // Log summary of errors if any
        if (!errors.isEmpty()) {
            logger.warn("Data aggregation completed with {} errors for Report ID: {}. Errors: {}",
                    errors.size(), reportId, errors);
        } else {
            logger.info("Data aggregation completed successfully for Report ID: {}", reportId);
        }

        return dataDTO;
    }

    /**
     * Fetch disaster report from database
     */
    private DisasterReport fetchFromDatabase(Integer reportId) {
        Optional<DisasterReport> reportOpt = disasterReportRepository.findByReportId(reportId);

        if (reportOpt.isEmpty()) {
            throw new DataAggregationException("Disaster report not found with ID: " + reportId);
        }

        return reportOpt.get();
    }

    /**
     * Populate DTO from DisasterReport entity
     */
    private void populateFromDisasterReport(DisasterDataDTO dto, DisasterReport report) {
        dto.setReportId(report.getReportId());
        dto.setLocation(report.getLocation());
        dto.setEirCode(report.getEirCode());
        dto.setLatitude(report.getLatitude());
        dto.setLongitude(report.getLongitude());
        dto.setDateTime(report.getDateAdded());
        dto.setDisasterType(report.getReportType());
        dto.setSeverity(report.getSeverity());
        dto.setScale(report.getScale());
        dto.setMediaUrls(report.getMediaUrls());
        dto.setAdditionalDetails(report.getAdditionalDetails());
    }

    /**
     * Fetch data from Social Media API
     * Returns trending posts, public sentiment, etc.
     */
    private Map<String, Object> fetchFromSocialMediaAPI(Integer reportId) {
        try {
            String url = socialMediaApiUrl + "/disaster/" + reportId;

            // Example API call - adjust based on actual API structure
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            return response != null ? response : new HashMap<>();

        } catch (Exception e) {
            logger.error("Error calling Social Media API", e);
            throw new DataAggregationException("Social Media API call failed", "SOCIAL_MEDIA_API", e);
        }
    }

    /**
     * Fetch data from external APIs (weather, news, etc.)
     */
    private Map<String, Object> fetchFromExternalAPIs(Integer reportId, Double latitude, Double longitude) {
        Map<String, Object> externalData = new HashMap<>();

        try {
            // Example: Fetch weather data if coordinates are available
            if (latitude != null && longitude != null) {
                String weatherUrl = weatherApiUrl + "?lat=" + latitude + "&lon=" + longitude;
                @SuppressWarnings("unchecked")
                Map<String, Object> weatherData = restTemplate.getForObject(weatherUrl, Map.class);
                externalData.put("weather", weatherData);
            }

            // Add more external API calls as needed

        } catch (Exception e) {
            logger.error("Error calling External APIs", e);
            throw new DataAggregationException("External API call failed", "EXTERNAL_API", e);
        }

        return externalData;
    }

    /**
     * Enrich DTO with resource allocation data
     * (vehicles, supplies, medical kits, casualties, etc.)
     */
    private void enrichWithResourceData(DisasterDataDTO dto, Integer reportId) {
        try {
            String url = resourcesApiUrl + "/disaster/" + reportId + "/resources";

            @SuppressWarnings("unchecked")
            Map<String, Object> resourceData = restTemplate.getForObject(url, Map.class);

            if (resourceData != null) {
                dto.setNumberOfVehicles(getIntValue(resourceData, "vehicles"));
                dto.setFoodSupplies(getStringValue(resourceData, "foodSupplies"));
                dto.setMedicalKits(getStringValue(resourceData, "medicalKits"));
                dto.setOtherResources(getStringValue(resourceData, "otherResources"));
                dto.setNumberOfCasualties(getIntValue(resourceData, "casualties"));
                dto.setEvacuationRoutes(getStringValue(resourceData, "evacuationRoutes"));
                dto.setEvacuationAction(getStringValue(resourceData, "evacuationAction"));
                dto.setDamageAssessment(getStringValue(resourceData, "damageAssessment"));
            }

        } catch (Exception e) {
            logger.error("Error fetching resource data", e);
            throw new DataAggregationException("Resource API call failed", "RESOURCE_API", e);
        }
    }

    // Helper methods
    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}

---------------------------------------------------------------------------
