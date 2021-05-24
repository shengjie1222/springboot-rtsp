package com.ethereal.rtmp.handler;


import com.ethereal.rtmp.common.Options;
import com.ethereal.rtmp.common.TransferDataType;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FrameGrabber.Exception;

import java.io.IOException;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;

/**
 * rtsp转rtmp（转封装方式）
 *
 * @author Administrator
 */
@Slf4j
public class ConvertVideoPacket {

    private FFmpegFrameGrabber grabber;
    private FFmpegFrameRecorder recorder;

    private int width = -1, height = -1;

    /**
     * 音频解码器
     */
    private int audioCodec;
    /**
     * 视频解码器
     */
    private int videoCodec;
    /**
     * 帧率
     */
    private double videoFramerate;
    /**
     * 比特率
     */
    private int videoBitrate;

    /**
     * 音频参数
     * 想要录制音频，这三个参数必须有：audioChannels > 0 && audioBitrate > 0 && sampleRate > 0
     */
    private int audioChannels;
    /**
     * 音频比特率
     */
    private int audioBitrate;
    private int sampleRate;

    private String fromSrc,outSrc;

    /**
     * 选择视频源
     */
    public ConvertVideoPacket from(String src) throws Exception {
        this.fromSrc = src;
        // 采集/抓取器
        grabber = new FFmpegFrameGrabber(src);
        grabber.setOptions(Options.getGrabber());
        if (src.startsWith(TransferDataType.rtsp.name())) {
            grabber.setOption("rtsp_transport", "tcp");
        }

        // 开始之后ffmpeg会采集视频信息，之后就可以获取音视频信息
        grabber.startUnsafe();

        width = grabber.getImageWidth();
        height = grabber.getImageHeight();
        sampleRate = grabber.getSampleRate();
        // 视频参数
        videoCodec = grabber.getVideoCodec();
        videoFramerate = grabber.getVideoFrameRate();
        videoBitrate = grabber.getVideoBitrate();
        // 音频参数
        audioCodec = grabber.getAudioCodec();
        audioChannels = grabber.getAudioChannels();
        audioBitrate = grabber.getAudioBitrate();
        if (audioBitrate < 1) {
            audioBitrate = 128 * 1000;
        }
        if (videoBitrate < 1) {
            videoBitrate = 4 * 1024 * 1024;
        }
        return this;
    }

    /**
     * 选择输出
     */
    public ConvertVideoPacket to(String out) throws IOException {
        this.outSrc = out;
        // 录制/推流器
        recorder = new FFmpegFrameRecorder(out, width, height, audioChannels);
        recorder.setVideoOptions(Options.Video.getRecorder());
        recorder.setAudioOptions(Options.Audio.getRecorder());
        recorder.setGopSize(2);
        recorder.setFrameRate(videoFramerate);
        recorder.setVideoBitrate(videoBitrate);
        recorder.setVideoCodec(videoCodec);
        recorder.setAudioBitrate(audioBitrate);
        recorder.setAudioCodec(audioCodec);
        recorder.setSampleRate(sampleRate);
        AVFormatContext fc = null;
        if (out.startsWith(TransferDataType.rtmp.name())
                || out.startsWith(TransferDataType.flv.name())) {
            // 封装格式flv
            recorder.setFormat("flv");
            fc = grabber.getFormatContext();
        }
        recorder.start(fc);
        return this;
    }

    /**
     * 转封装
     */
    public void go() throws IOException {
        log.info("fromSrc:{} -> outSrc:{}",fromSrc, outSrc);
        //采集或推流导致的错误次数
        long err_index = 0;
        grabber.flush();
        //连续五次没有采集到帧则认为视频采集结束，程序错误次数超过1次即中断程序
        for (int no_frame_index = 0; no_frame_index < 5 || err_index > 1; ) {
            AVPacket pkt;
            try {
                //没有解码的音视频帧
                pkt = grabber.grabPacket();
                if (pkt == null || pkt.size() <= 0 || pkt.data() == null) {
                    //空包记录次数跳过
                    no_frame_index++;
                    continue;
                }
                //不需要编码直接把音视频帧推出去
                //如果失败err_index自增1
                err_index += (recorder.recordPacket(pkt) ? 0 : 1);
                av_packet_unref(pkt);
            } catch (Exception e) {//推流失败
                err_index++;
                e.printStackTrace();
            }
        }
        log.warn("连接断开");
    }

}