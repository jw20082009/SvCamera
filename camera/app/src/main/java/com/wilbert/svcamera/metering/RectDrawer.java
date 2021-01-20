package com.wilbert.svcamera.metering;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


/**
 * @author wilbert
 * @Date 2021/1/14 15:46
 * @email jiangwang.wilbert@bigo.sg
 **/
public class RectDrawer {


    private final String VERTEX = "attribute vec4 a_Position;\n" +
            "void main()\n" +
            "{\n" +
            "   gl_Position = a_Position;\n" +
            "}";
    private final String FRAGMENT = "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "void main()\n" +
            "{\n" +
            "   gl_FragColor = u_Color;\n" +
            "}";

    private static final int BYTES_PER_FLOAT = 4;
    static final int COORDS_PER_VERTEX = 2;
    private static final int POSITION_COMPONENT_COUNT = 4;
    static float squareCoords[] = {-0.5f, 0.5f,   // top left
            -0.5f, -0.5f,   // bottom left
            0.5f, -0.5f,   // bottom right
            0.5f, 0.5f}; // top right
    private FloatBuffer vertexBuffer;
    private static final String A_POSITION = "a_Position";
    private static final String U_COLOR = "u_Color";
    private int uColorLocation;
    private int aPositionLocation;
    private int program = -1;

    public RectDrawer() {
        vertexBuffer = ByteBuffer
                .allocateDirect(squareCoords.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);
        getProgram();
        uColorLocation = GLES20.glGetUniformLocation(program, U_COLOR);
        aPositionLocation = GLES20.glGetAttribLocation(program, A_POSITION);

        //---------第五步: 传入数据
        GLES20.glVertexAttribPointer(aPositionLocation, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(aPositionLocation);
    }

    //获取program
    private void getProgram() {
        String vertexShaderSource = VERTEX;
        String fragmentShaderSource = FRAGMENT;
        program = buildProgram(vertexShaderSource, fragmentShaderSource);
    }

    //以GL_LINE_LOOP方式绘制
    public void draw(float[] squareCoords) {
        if (squareCoords.length != 8 || program == -1) {
            return;
        }
        GLES20.glUseProgram(program);
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(aPositionLocation, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, 4);
        GLES20.glDisableVertexAttribArray(aPositionLocation);
    }

    public void release(){
        if (program != -1) {
            GLES20.glDeleteProgram(program);
            program = -1;
        }
    }
    /**
     * 加载并编译顶点着色器，返回得到的opengl id
     *
     * @param shaderCode
     * @return
     */
    public static int compileVertexShader(String shaderCode) {
        return compileShader(GLES20.GL_VERTEX_SHADER, shaderCode);
    }

    /**
     * 加载并编译片段着色器，返回opengl id
     *
     * @param shaderCode
     * @return
     */
    public static int compileFragmentShader(String shaderCode) {
        return compileShader(GLES20.GL_FRAGMENT_SHADER, shaderCode);
    }

    /**
     * 加载并编译着色器，返回opengl id
     *
     * @param type
     * @param shaderCode
     * @return
     */
    private static int compileShader(int type, String shaderCode) {
        final int shaderObjectId = GLES20.glCreateShader(type);
        if (shaderObjectId == 0) {
            return 0;
        }
        GLES20.glShaderSource(shaderObjectId, shaderCode);
        GLES20.glCompileShader(shaderObjectId);
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shaderObjectId, GLES20.GL_COMPILE_STATUS,
                compileStatus, 0);
        if (compileStatus[0] == 0) {
            GLES20.glDeleteShader(shaderObjectId);
            return 0;
        }
        return shaderObjectId;
    }

    /**
     * 链接顶点着色器和片段着色器成一个program
     * 并返回这个pragram的opengl id
     *
     * @param vertexShaderId
     * @param fragmentShaderId
     * @return
     */
    public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        final int programObjectId = GLES20.glCreateProgram();
        if (programObjectId == 0) {
            return 0;
        }
        GLES20.glAttachShader(programObjectId, vertexShaderId);
        GLES20.glAttachShader(programObjectId, fragmentShaderId);
        GLES20.glLinkProgram(programObjectId);
        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programObjectId, GLES20.GL_LINK_STATUS,
                linkStatus, 0);
        if (linkStatus[0] == 0) {
            // If it failed, delete the program object.
            GLES20.glDeleteProgram(programObjectId);
            return 0;
        }
        return programObjectId;
    }

    /**
     * Validates an OpenGL program. Should only be called when developing the
     * application.
     */
    public static boolean validateProgram(int programObjectId) {
        GLES20.glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        GLES20.glGetProgramiv(programObjectId, GLES20.GL_VALIDATE_STATUS,
                validateStatus, 0);
        return validateStatus[0] != 0;
    }

    /**
     * /**
     * 编译，连接 ，返回 program 的 ID
     *
     * @param vertexShaderSource
     * @param fragmentShaderSource
     * @return
     */
    public static int buildProgram(String vertexShaderSource,
                                   String fragmentShaderSource) {
        int program;
        int vertexShader = compileVertexShader(vertexShaderSource);
        int fragmentShader = compileFragmentShader(fragmentShaderSource);
        program = linkProgram(vertexShader, fragmentShader);
        return program;
    }

}
