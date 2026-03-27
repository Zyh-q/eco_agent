package com.carbon.dto;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class ImageParseDTO {
    private String type;
    private String detail;
    private Double amount;
    private String unit;
    private Double confidence;

    // AI直接提供的排放因子（kg/单位）
    private Double emissionFactor;

    // AI识别的功率（千瓦），仅对能源消耗有效
    private Double powerKw;

    public boolean checkValid() {
        return StringUtils.hasText(type)
                && StringUtils.hasText(detail)
                && amount != null && amount > 0
                && StringUtils.hasText(unit)
                && confidence != null && confidence >= 0 && confidence <= 1;
    }
}