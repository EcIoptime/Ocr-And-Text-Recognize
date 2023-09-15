package com.example.paddleocrlib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BaseResultModel {
    private int index;
    private String name;
    private float confidence;
    private int[] boxesBoundary;

    public BaseResultModel() {

    }

    public BaseResultModel(int index, String name, float confidence, int[] boxesBoundary) {
        this.index = index;
        this.name = name;
        this.confidence = confidence;
        this.boxesBoundary = boxesBoundary;
    }



    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int[] getBoxesBoundary() {
        return boxesBoundary;
    }

    public void setBoxesBoundary(int[] boxesBoundary) {
        this.boxesBoundary = boxesBoundary;
    }
}
