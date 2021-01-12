package com.wilbert.svcamera.dev;

import android.graphics.Matrix;

public class PointTransform{
    public Matrix matrix = new Matrix();  // 从显示View的位置转换到摄像头位置的坐标转换矩阵
    public int leftMargin = 0;
    public int topMargin = 0;
    public int scaledPreviewWidth = 0;  // 将采集图像放大显示后的图像宽度
    public int scaledPreviewHeight = 0;  // 将采集图像放大显示后的图像高度

    @Override
    public String toString() {
        return "PointTransform{" +
                "matrix=" + matrix +
                ", leftMargin=" + leftMargin +
                ", topMargin=" + topMargin +
                ", scaledPreviewWidth=" + scaledPreviewWidth +
                ", scaledPreviewHeight=" + scaledPreviewHeight +
                '}';
    }
}

