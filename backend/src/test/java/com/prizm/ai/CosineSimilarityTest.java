package com.prizm.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CosineSimilarityTest {

    @Test
    void identicalVectorsAreOne() {
        double[] a = {1, 0, 0};
        assertEquals(1.0, CosineSimilarity.cosine(a, a), 1e-9);
    }

    @Test
    void orthogonalVectorsAreZero() {
        double[] a = {1, 0};
        double[] b = {0, 1};
        assertEquals(0.0, CosineSimilarity.cosine(a, b), 1e-9);
    }

    @Test
    void demoClusterVectorsStayAboveThreshold() {
        double[] a = com.prizm.demo.DemoDataSeeder.vec(0);
        double[] b = com.prizm.demo.DemoDataSeeder.vec(0);
        double[] other = com.prizm.demo.DemoDataSeeder.vec(2);
        assertTrue(CosineSimilarity.cosine(a, b) > 0.99);
        assertTrue(CosineSimilarity.cosine(a, other) < 0.1);
    }
}
