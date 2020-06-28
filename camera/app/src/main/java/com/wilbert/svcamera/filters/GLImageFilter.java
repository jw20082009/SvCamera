package com.wilbert.svcamera.filters;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.text.TextUtils;
import android.util.Log;

import com.wilbert.svcamera.glutils.OpenGLUtils;
import com.wilbert.svcamera.glutils.TextureRotationUtils;

import java.nio.FloatBuffer;
import java.util.LinkedList;

/**
 * 基类滤镜
 * Created by cain on 2017/7/9.
 */

public class GLImageFilter {

    public static final String VERTEX_SHADER = "" +
            "attribute vec4 aPosition;                                  \n" +
            "attribute vec4 aTextureCoord;                              \n" +
            "varying vec2 textureCoordinate;                            \n" +
            "void main() {                                              \n" +
            "    gl_Position = aPosition;                               \n" +
            "    textureCoordinate = aTextureCoord.xy;                  \n" +
            "}                                                          \n";

    public static final String FRAGMENT_SHADER = "" +
            "precision mediump float;                                   \n" +
            "varying vec2 textureCoordinate;                            \n" +
            "uniform sampler2D inputTexture;                                \n" +
            "void main() {                                              \n" +
            "    gl_FragColor = texture2D(inputTexture, textureCoordinate); \n" +
            "}                                                          \n";

    protected String TAG = getClass().getSimpleName();

    protected Context mContext;
    // 当前时钟
    protected float mCurrentTimer;

    private final LinkedList<Runnable> mRunOnDraw;

    // 纹理字符串
    protected String mVertexShader;
    protected String mFragmentShader;

    // 是否初始化成功
    protected boolean mIsInitialized;
    // 滤镜是否可用，默认可用
    protected boolean mFilterEnable = true;

    // 每个顶点坐标有几个参数
    protected int mCoordsPerVertex = TextureRotationUtils.CoordsPerVertex;
    // 顶点坐标数量
    protected int mVertexCount = TextureRotationUtils.CubeVertices.length / mCoordsPerVertex;

    // 句柄
    protected int mProgramHandle;
    protected int mPositionHandle;
    protected int mTextureCoordinateHandle;
    protected int mInputTextureHandle;

    // 渲染的Image的宽高
    protected int mImageWidth;
    protected int mImageHeight;

    //显示起始位置
    protected int mStartX;
    protected int mStartY;

    // 显示输出的宽高
    protected int mDisplayWidth;
    protected int mDisplayHeight;

    // FBO的宽高，可能跟输入的纹理大小不一致
    protected int mFrameStartX = 0;
    protected int mFrameStartY = 0;
    protected int mFrameWidth = -1;
    protected int mFrameHeight = -1;
    protected boolean mHasFrameBufferSizeChanged = false;

    // FBO
    protected int[] mFrameBuffers;
    protected int[] mFrameBufferTextures;

    public GLImageFilter(Context context) {
        this(context, VERTEX_SHADER, FRAGMENT_SHADER);
    }

    public GLImageFilter(Context context, String vertexShader, String fragmentShader) {
        mContext = context;
        mRunOnDraw = new LinkedList<>();
        // 记录shader数据
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
        // 初始化程序句柄
        initProgramHandle();
    }

    public GLImageFilter(String vertexShader, String fragmentShader) {
        mRunOnDraw = new LinkedList<>();
        // 记录shader数据
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
        // 初始化程序句柄
        initProgramHandle();
    }

    /**
     * 初始化程序句柄
     */
    public void initProgramHandle() {
        // 只有在shader都不为空的情况下才初始化程序句柄
        if (!TextUtils.isEmpty(mVertexShader) && !TextUtils.isEmpty(mFragmentShader)) {
            mProgramHandle = OpenGLUtils.createProgram(mVertexShader, mFragmentShader);
            mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
            mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
            mInputTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, "inputTexture");
            mIsInitialized = true;
        } else {
            mPositionHandle = OpenGLUtils.GL_NOT_INIT;
            mTextureCoordinateHandle = OpenGLUtils.GL_NOT_INIT;
            mInputTextureHandle = OpenGLUtils.GL_NOT_TEXTURE;
            mIsInitialized = false;
        }
    }

    /**
     * Surface发生变化时调用
     */
    public void onInputSizeChanged(int width, int height) {
        if (mImageWidth != width || mImageHeight != height) {
            mFrameStartX = 0;
            mFrameStartY = 0;
            mImageWidth = width;
            mImageHeight = height;
            mFrameWidth = mImageWidth;
            mFrameHeight = mImageHeight;
        }
    }

    public int getStartX() {
        return mFrameStartX;
    }

    public int getStartY() {
        return mFrameStartY;
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }

    /**
     * 显示视图发生变化时调用
     */
    public void onDisplaySizeChanged(int width, int height) {
        mDisplayWidth = width;
        mDisplayHeight = height;
    }

    /**
     * 绘制Frame
     */
    public boolean drawFrame(int textureId, FloatBuffer vertexBuffer,
                             FloatBuffer textureBuffer) {
        // 没有初始化、输入纹理不合法、滤镜不可用时直接返回
        if (!mIsInitialized || textureId == OpenGLUtils.GL_NOT_INIT || !mFilterEnable) {
            return false;
        }
        Log.i(TAG, "drawFrame:" + mStartX + ";" + mStartY + ";" + mDisplayWidth + ";" + mDisplayHeight + ";" + hashCode());
        // 设置视口大小
        GLES20.glViewport(mStartX, mStartY, mDisplayWidth, mDisplayHeight);
        // 使用当前的program
        GLES20.glUseProgram(mProgramHandle);
        // 运行延时任务
        runPendingOnDrawTasks();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // 绘制纹理
        onDrawTexture(textureId, vertexBuffer, textureBuffer);

        return true;
    }

    /**
     * 绘制到FBO
     *
     * @return FBO绑定的Texture
     */
    public int drawFrameBuffer(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        // 没有FBO、没初始化、输入纹理不合法、滤镜不可用时，直接返回
        if (textureId == OpenGLUtils.GL_NOT_TEXTURE
                || mFrameBuffers == null
                || mFrameBufferTextures == null
                || !mIsInitialized
                || !mFilterEnable) {
            return textureId;
        }
        Log.i(TAG, "drawFrameBuffer:" + mFrameStartX + ";" + mFrameStartY + ";" + mFrameWidth + ";" + mFrameHeight + ";" + hashCode());
        // 绑定FBO
        GLES20.glViewport(mFrameStartX, mFrameStartY, mFrameWidth, mFrameHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        if (mHasFrameBufferSizeChanged) {
            GLES20.glClearColor(0f, 0f, 0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            mHasFrameBufferSizeChanged = false;
        }
        // 使用当前的program
        GLES20.glUseProgram(mProgramHandle);
        // 运行延时任务，这个要放在glUseProgram之后，要不然某些设置项会不生效
        runPendingOnDrawTasks();

        // 绘制纹理
        onDrawTexture(textureId, vertexBuffer, textureBuffer);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return mFrameBufferTextures[0];
    }

    public int getFrameBuffer() {
        if (mFrameBuffers != null)
            return mFrameBuffers[0];
        return -1;
    }

    public int getFrameBufferTextureId() {
        if (mFrameBufferTextures != null)
            return mFrameBufferTextures[0];
        return OpenGLUtils.GL_NOT_TEXTURE;
    }

    /**
     * 绘制
     */
    protected void onDrawTexture(int textureId, FloatBuffer vertexBuffer,
                                 FloatBuffer textureBuffer) {
        if (vertexBuffer == null || textureBuffer == null) {
            return;
        }
        // 绑定顶点坐标缓冲
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, mCoordsPerVertex,
                GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // 绑定纹理坐标缓冲
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, 2,
                GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(getTextureType(), textureId);
        GLES20.glUniform1i(mInputTextureHandle, 0);
        onDrawFrameBegin();
        onDrawFrame();
        onDrawFrameAfter();
        // 解绑
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandle);
        GLES20.glBindTexture(getTextureType(), 0);

        GLES20.glUseProgram(0);
    }

    /**
     * 调用glDrawArrays/glDrawElements之前，方便添加其他属性
     */
    public void onDrawFrameBegin() {

    }

    /**
     * 绘制图像
     */
    protected void onDrawFrame() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mVertexCount);
    }

    /**
     * glDrawArrays/glDrawElements调用之后，方便销毁其他属性
     */
    public void onDrawFrameAfter() {

    }

    protected void onUnbindTextureValue() {

    }

    /**
     * 获取Texture类型
     * GLES20.TEXTURE_2D / GLES11Ext.GL_TEXTURE_EXTERNAL_OES等
     */
    public int getTextureType() {
        return GLES20.GL_TEXTURE_2D;
    }

    /**
     * 释放资源
     */
    public void release() {
        if (mIsInitialized) {
            GLES20.glDeleteProgram(mProgramHandle);
            mProgramHandle = OpenGLUtils.GL_NOT_INIT;
        }
        destroyFrameBuffer();
    }

    /**
     * 创建FBO
     */
    public void initFrameBuffer(int width, int height) {
        if (!isInitialized()) {
            return;
        }
        if (mFrameBuffers != null && (mImageWidth != width || mImageHeight != height)) {
            destroyFrameBuffer();
        }
        if (mFrameBuffers == null) {
            mImageWidth = width;
            mImageHeight = height;
            mFrameWidth = width;
            mFrameHeight = height;
            mFrameBuffers = new int[1];
            mFrameBufferTextures = new int[1];
            OpenGLUtils.createFrameBuffer(mFrameBuffers, mFrameBufferTextures, width, height);
        }
    }

    /**
     * 销毁纹理
     */
    public void destroyFrameBuffer() {
        if (!mIsInitialized) {
            return;
        }
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }

        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
        mFrameWidth = -1;
    }

    /**
     * 判断是否初始化
     */
    public boolean isInitialized() {
        return mIsInitialized;
    }

    /**
     * 设置滤镜是否可用
     */
    public void setFilterEnable(boolean enable) {
        mFilterEnable = enable;
    }

    /**
     * 设置时钟
     */
    public void setTimerValue(float currentTimer) {
        mCurrentTimer = currentTimer;
    }

    /**
     * 获取输出宽度
     */
    public int getDisplayWidth() {
        return mDisplayWidth;
    }

    /**
     * 获取输出高度
     */
    public int getDisplayHeight() {
        return mDisplayHeight;
    }

    public int getDisplayStartX() {
        return mStartX;
    }

    public int getDisplayStartY() {
        return mStartY;
    }

    ///------------------ 统一变量(uniform)设置 ------------------------///
    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setPoint(final int location, final PointF point) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = point.x;
                vec2[1] = point.y;
                GLES20.glUniform2fv(location, 1, vec2, 0);
            }
        });
    }

    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
            }
        });
    }

    /**
     * 添加延时任务
     */
    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    /**
     * 运行延时任务
     */
    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    protected static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }

    public void centerInsideFrameBufferSimple(int frameWidth, int frameHeight) {
        if (mImageWidth > 0 && mImageHeight > 0 && mFrameWidth > 0 && mFrameHeight > 0 && frameWidth > 0 && frameHeight > 0) {
            if (1.0f * frameWidth / frameHeight > 1.0f * mImageWidth / mImageHeight) {
                mFrameStartX = 0;
                mFrameWidth = mImageWidth;
                mFrameHeight = (int) (mFrameWidth / (1.0f * frameWidth / frameHeight));
                mFrameStartY = (int) ((mImageHeight - mFrameHeight) / 2.0f);
            } else if (1.0f * frameWidth / frameHeight < 1.0f * mImageWidth / mImageHeight) {
                mFrameStartY = 0;
                mFrameHeight = mImageHeight;
                mFrameWidth = (int) (mFrameHeight * (1.0f * frameWidth / frameHeight));
                mFrameStartX = (int) ((mImageWidth - mFrameWidth) / 2.0f);
            } else {
                mFrameStartX = 0;
                mFrameStartY = 0;
                mFrameWidth = mImageWidth;
                mFrameHeight = mImageHeight;
            }
            Log.i(TAG, "centerInsideFrameBufferSimple:" + frameWidth + ";" + frameHeight + ";" + mImageWidth + ";" + mImageHeight + ";" + mFrameWidth + ";" + mFrameHeight);
            mHasFrameBufferSizeChanged = true;
        }
        Log.i(TAG, "centerInsideFrameBufferSimple:" + frameWidth + "*" + frameHeight + ";" + mImageWidth + "*" + mImageHeight + ";" + mFrameStartX + "*" + mFrameStartY + "*" + mFrameWidth + "*" + mFrameHeight);
    }
}
