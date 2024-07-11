package org.swdc.recorder.core.ffmpeg.filters;

/**
 * 音频Filter监听器。
 * 音频Filter的connection出现变化的时候，本接口的方法
 * 应当被触发。
 */
public interface AudioFilterListener {

    /**
     * 实现本接口的Filter被其他的Filter链接。
     * @param filter 链接本Filter的另一个Filter。
     */
    void onConnected(FFAudioFilter filter);


    /**
     * 实现本接口的Filter被其他的Filter取消链接。
     * @param filter 取消此链接的Filter。
     */
    void onDisconnect(FFAudioFilter filter);

}
