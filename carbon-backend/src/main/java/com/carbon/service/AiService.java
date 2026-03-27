package com.carbon.service;

import com.carbon.dto.AiSuggestionDTO;
import com.carbon.dto.CarbonChatDTO;
import com.carbon.dto.ImageParseDTO;
import com.carbon.entity.CarbonRecord;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface AiService {
    String getCurrentWeather();
    String getWeatherByLocation(Double latitude, Double longitude);
    CarbonChatDTO generateWarmCarbonChat(Double carbonAmount, List<CarbonRecord> historyRecords, String weather, ImageParseDTO parseDTO, String nickname);
    ImageParseDTO parseCarbonFromImage(MultipartFile file);
    List<ImageParseDTO> parseCarbonFromText(String text);
    List<AiSuggestionDTO> generateCarbonSuggestion(List<CarbonRecord> carbonRecords);
    String chatWithAi(String question, Long userId);
    Double calculateCarbonWithParseDTO(ImageParseDTO parseDTO);
}