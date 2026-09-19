package com.prizm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "prizm")
public class PrizmProperties {

    private double similarityThreshold = 0.75;
    private boolean demoSeed = true;

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public boolean isDemoSeed() {
        return demoSeed;
    }

    public void setDemoSeed(boolean demoSeed) {
        this.demoSeed = demoSeed;
    }
}
