package com.carbon.service;

import com.carbon.entity.AiSuggestion;
import java.util.List;

public interface AiSuggestionService {
    // 保存建议
    void save(AiSuggestion suggestion);
    // 查询建议
    List<AiSuggestion> getSuggestionsByUserId(Long userId);
    // 删除建议
    void removeByUserId(Long userId);
}