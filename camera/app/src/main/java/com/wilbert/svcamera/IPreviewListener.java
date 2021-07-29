package com.wilbert.svcamera;

import android.content.Context;
import android.graphics.SurfaceTexture;

/**
 * @author wilbert
 * @Date 2020/12/15 10:31
 * @email jw20082009@qq.com
 **/
public interface IPreviewListener {
    void onSurfaceCreated(SurfaceTexture surfaceTexture);

    void onSurfaceDestroy();
}
