package com.wilbert.svcamera.filters;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.wilbert.svcamera.glutils.MatrixUtils;
import com.wilbert.svcamera.glutils.OpenGLUtils;

/**
 * 外部纹理(OES纹理)输入
 * Created by cain on 2017/7/9.
 */

public class GLImageOESInputFilter extends GLImageFilter {

    private int mTransformMatrixHandle;
    private float[] mTransformMatrix;

    public GLImageOESInputFilter(Context context) {
        this(context, OpenGLUtils.getShaderFromAssets(context, "shader/vertex_oes_input.glsl"),
            OpenGLUtils.getShaderFromAssets(context, "shader/fragment_oes_input.glsl"));
    }

    public GLImageOESInputFilter(Context context, String vertexShader, String fragmentShader) {
        super(context, vertexShader, fragmentShader);
        mTransformMatrix = MatrixUtils.getOriginalMatrix();
    }

    @Override
    public void initProgramHandle() {
        super.initProgramHandle();
        mTransformMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "transformMatrix");
    }

    @Override
    public int getTextureType() {
        return GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    }

    @Override
    public void onDrawFrameBegin() {
        super.onDrawFrameBegin();
        GLES20.glUniformMatrix4fv(mTransformMatrixHandle, 1, false, mTransformMatrix, 0);
    }

    /**
     * 设置SurfaceTexture的变换矩阵
     */
    public void setTextureTransformMatrix(float[] transformMatrix) {
        mTransformMatrix = transformMatrix;
    }
}
