package com.carbon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "carbon.factors")
public class CarbonFactorConfig {
    private Traffic traffic;
    private Energy energy;
    private Food food;
    private Shopping shopping;
    private Waste waste;

    @Data
    public static class Traffic {
        private double car;
        private double bus;
        private double metro;
        private double defaultFactor;
    }

    @Data
    public static class Energy {
        private double grid;
        private Appliances appliances;
        private double defaultPower;
    }

    @Data
    public static class Appliances {
        private double ac;
        private double computer;
        private double tv;
        private double light;
    }

    @Data
    public static class Food {
        private double meat;
        private double normal;
        private double defaultFactor;
    }

    @Data
    public static class Shopping {
        private double defaultFactor;
    }

    @Data
    public static class Waste {
        private double plastic;
        private double normal;
    }
}