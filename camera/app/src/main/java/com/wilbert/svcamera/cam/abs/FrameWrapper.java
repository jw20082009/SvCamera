package com.wilbert.svcamera.cam.abs;

import android.graphics.RectF;

/**
 * @author wilbert
 * @Date 2021/2/18 16:58
 * @email jiangwang.wilbert@bigo.sg
 **/
public class FrameWrapper {
    private int width;
    private int height;
    private int rotation;
    private int mirror;
    private int noiseReduce;
    private int luminancePromote;
    private int colorPromote;
    private int dirtyCheck;
    private int preBeautify;
    private RectF cropRectf;
    private float scaleX;
    private float scaleY;

    public FrameWrapper(){}

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public int getMirror() {
        return mirror;
    }

    public void setMirror(int mirror) {
        this.mirror = mirror;
    }

    public int getNoiseReduce() {
        return noiseReduce;
    }

    public void setNoiseReduce(int noiseReduce) {
        this.noiseReduce = noiseReduce;
    }

    public int getLuminancePromote() {
        return luminancePromote;
    }

    public void setLuminancePromote(int luminancePromote) {
        this.luminancePromote = luminancePromote;
    }

    public int getColorPromote() {
        return colorPromote;
    }

    public void setColorPromote(int colorPromote) {
        this.colorPromote = colorPromote;
    }

    public int getDirtyCheck() {
        return dirtyCheck;
    }

    public void setDirtyCheck(int dirtyCheck) {
        this.dirtyCheck = dirtyCheck;
    }

    public int getPreBeautify() {
        return preBeautify;
    }

    public void setPreBeautify(int preBeautify) {
        this.preBeautify = preBeautify;
    }

    public RectF getCropRectf() {
        return cropRectf;
    }

    public void setCropRectf(RectF cropRectf) {
        this.cropRectf = cropRectf;
    }

    public float getScaleX() {
        return scaleX;
    }

    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }
}
