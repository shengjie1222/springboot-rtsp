package com.ethereal.rtmp.common;

import com.ethereal.rtmp.server.cfg.LiveConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * 输出参数
 * @author Administrator
 * @Description
 * @create 2021-05-21 14:03
 */
public class Options {
    public static LiveConfig liveConfig;
    private static Map<String,String> GRABBER_OPTIONS = new HashMap<>();


    public static void grabberPutAll(Map map){
        GRABBER_OPTIONS.putAll(map);
    }

    public static Map<String,String> getGrabber(){
        return GRABBER_OPTIONS;
    }

    public static class Video {
        private static Map<String,String> RECORDER_OPTIONS = new HashMap<>();

        public static void recorderPutAll(Map map){
            RECORDER_OPTIONS.putAll(map);
        }
        public static Map<String,String> getRecorder(){
            return RECORDER_OPTIONS;
        }
    }
    public static class Audio {
        private static Map<String,String> RECORDER_OPTIONS = new HashMap<>();

        public static void recorderPutAll(Map map){
            RECORDER_OPTIONS.putAll(map);
        }
        public static Map<String,String> getRecorder(){
            return RECORDER_OPTIONS;
        }
    }

}
