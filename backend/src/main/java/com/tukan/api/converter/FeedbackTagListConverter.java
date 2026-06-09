package com.tukan.api.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.entity.RecommendationFeedback;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class FeedbackTagListConverter implements AttributeConverter<List<RecommendationFeedback.FeedbackTag>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<RecommendationFeedback.FeedbackTag> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize FeedbackTag list", e);
        }
    }

    @Override
    public List<RecommendationFeedback.FeedbackTag> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(dbData, new TypeReference<List<RecommendationFeedback.FeedbackTag>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize FeedbackTag list", e);
        }
    }
}
