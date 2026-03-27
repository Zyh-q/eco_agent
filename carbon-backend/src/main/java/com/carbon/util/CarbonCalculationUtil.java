package com.carbon.util;

import com.carbon.config.CarbonFactorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CarbonCalculationUtil {
    private static final Logger logger = LoggerFactory.getLogger(CarbonCalculationUtil.class);
    private static final int SCALE = 2;
    @Autowired
    private CarbonFactorConfig factorConfig;
    public BigDecimal calculateCarbon(String type, String detail, BigDecimal amount, String unit) {
        if (type == null || amount == null || unit == null) {
            return BigDecimal.ZERO;
        }
        double value = amount.doubleValue();
        try {
            switch (type) {
                case "交通出行":
                    return calcTraffic(value, detail);
                case "能源消耗":
                    return calcEnergy(value, unit, detail);
                case "饮食消费":
                    return calcFood(value, detail);
                case "购物消费":
                    return calcShopping(value);
                case "垃圾处理":
                    return calcWaste(value, detail);
                default:
                    logger.warn("未知类型：{}，返回0", type);
                    return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            logger.error("计算异常", e);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcTraffic(double value, String detail) {
        CarbonFactorConfig.Traffic traffic = factorConfig.getTraffic();
        double factor;
        if (detail.contains("开车") || detail.contains("汽车") || detail.contains("燃油车")) {
            factor = traffic.getCar();
        } else if (detail.contains("公交")) {
            factor = traffic.getBus();
        } else if (detail.contains("地铁")) {
            factor = traffic.getMetro();
        } else {
            factor = traffic.getDefaultFactor();
            logger.debug("未识别交通方式，使用默认因子 {}，行为：{}", factor, detail);
        }
        return BigDecimal.valueOf(value * factor).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calcEnergy(double value, String unit, String detail) {
        CarbonFactorConfig.Energy energy = factorConfig.getEnergy();
        double gridFactor = energy.getGrid();
        String detailLower = detail.toLowerCase();

        // 直接用电（度）
        if ("度".equals(unit)) {
            return BigDecimal.valueOf(value * gridFactor).setScale(SCALE, RoundingMode.HALF_UP);
        }

        // 按小时计算（需要功率）
        if ("小时".equals(unit)) {
            double power = energy.getDefaultPower();
            if (detailLower.contains("空调")) {
                power = energy.getAppliances().getAc();
            } else if (detailLower.contains("电脑") || detailLower.contains("网吧") || detailLower.contains("玩游戏")) {
                power = energy.getAppliances().getComputer();
            } else if (detailLower.contains("电视") || detailLower.contains("电视机")) {
                power = energy.getAppliances().getTv();
            } else if (detailLower.contains("灯") || detailLower.contains("照明") || detailLower.contains("电灯")) {
                power = energy.getAppliances().getLight();
            } else {
                logger.debug("未识别具体电器，使用默认功率 {} kW，行为：{}", power, detail);
            }
            return BigDecimal.valueOf(value * power * gridFactor).setScale(SCALE, RoundingMode.HALF_UP);
        }

        logger.warn("能源消耗单位无法处理：{}，返回0", unit);
        return BigDecimal.ZERO;
    }

    private BigDecimal calcFood(double value, String detail) {
        CarbonFactorConfig.Food food = factorConfig.getFood();
        double factor;
        if (detail.contains("肉") || detail.contains("鸡") || detail.contains("鱼") || detail.contains("牛肉")) {
            factor = food.getMeat();
        } else {
            factor = food.getNormal();
        }
        return BigDecimal.valueOf(value * factor).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calcShopping(double value) {
        return BigDecimal.valueOf(value * factorConfig.getShopping().getDefaultFactor())
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calcWaste(double value, String detail) {
        double factor;
        if (detail.contains("塑料") || detail.contains("包装")) {
            factor = factorConfig.getWaste().getPlastic();
        } else {
            factor = factorConfig.getWaste().getNormal();
        }
        return BigDecimal.valueOf(value * factor).setScale(SCALE, RoundingMode.HALF_UP);
    }
}