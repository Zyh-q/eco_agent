package com.carbon.service.impl;

import com.carbon.entity.AiSuggestion;
import com.carbon.mapper.AiSuggestionMapper;
import com.carbon.service.AiSuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiSuggestionServiceImpl implements AiSuggestionService {

    // 🔥 替换为 @Autowired，彻底解决注解报错
    @Autowired
    private AiSuggestionMapper aiSuggestionMapper;

    // 手写保存
    @Override
    public void save(AiSuggestion suggestion) {
        aiSuggestionMapper.insertSuggestion(suggestion);
    }

    // 查询
    @Override
    public List<AiSuggestion> getSuggestionsByUserId(Long userId) {
        return aiSuggestionMapper.selectByUserId(userId);
    }

    // 删除
    @Override
    public void removeByUserId(Long userId) {
        aiSuggestionMapper.deleteByUserId(userId);
    }
}