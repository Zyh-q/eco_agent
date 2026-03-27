package com.carbon.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchConfirmDTO {
    private Long userId;
    private String recordTime;
    private List<ImageParseDTO> items;
    private Double latitude;
    private Double longitude;
}