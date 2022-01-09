package com.mybb.dialogue;

import ai.olami.android.tts.ITtsPlayerListener;

public class MyITtsPlayerListener implements ITtsPlayerListener {
    //播放结束时的
    @Override
    public void onPlayEnd() {

    }

    //播放停止时的
    @Override
    public void onStop() {

    }

    //正在播放时的 Callback
    @Override
    public void onPlayingTTS(String s) {
        System.out.println(s);
    }
}
