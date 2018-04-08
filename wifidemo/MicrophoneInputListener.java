package com.jhard.wifidemo;

/**
 * Created by Chris on 11/30/2015.
 */
public interface MicrophoneInputListener {

    public void processAudioFrame(short[] audioFrame);
}
