package com.jhard.wifidemo;

import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by Justin on 4/8/2018.
 */

public class Microphone
{
    MicrophoneInput micInput;
    private int mSampleRate;
    private int mAudioSource;

    public Microphone(MicrophoneInputListener listener)
    {
        mSampleRate = 8000;
        mAudioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
        micInput = new MicrophoneInput(listener);
        micInput.setSampleRate(mSampleRate);
        micInput.setAudioSource(mAudioSource);
    }
    public void turnOn()
    {
        micInput.start();
    }
    public void turnOff()
    {
        micInput.stop();
    }
}
