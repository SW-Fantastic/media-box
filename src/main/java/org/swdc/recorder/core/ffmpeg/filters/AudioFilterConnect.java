package org.swdc.recorder.core.ffmpeg.filters;

/**
 * 音频滤镜链接符。
 * 记录音频滤镜的链接关系。
 */
public class AudioFilterConnect {

    /**
     * 来自源滤镜的输出index，
     * 将会作为下一个滤镜的input。
     */
    private int inputPad;

    /**
     * 来自目标滤镜的输入index，
     * 源滤镜的数据以此为目标进行输出。
     */
    private int outputPad;

    /**
     * 源滤镜
     */
    private FFAudioFilter inputFilter;

    /**
     * 目标滤镜
     */
    private FFAudioFilter outFilter;

    AudioFilterConnect(FFAudioFilter input, FFAudioFilter output, int inputPad, int outputPad) {

        this.inputFilter = input;
        this.outFilter = output;
        this.inputPad = inputPad;
        this.outputPad = outputPad;

    }

    public FFAudioFilter getInputFilter() {
        return inputFilter;
    }

    public FFAudioFilter getOutFilter() {
        return outFilter;
    }

    public int getInputPad() {
        return inputPad;
    }

    public int getOutputPad() {
        return outputPad;
    }


}
