package com.ethereal.rtmp.handler;


import com.ethereal.rtmp.common.Options;
import com.ethereal.rtmp.common.TransferDataType;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.springframework.util.CollectionUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
        recorder.setImageHeight(160);
        recorder.setImageWidth(240);
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
    public void go(String imagePath,int key) throws IOException {
        log.info("fromSrc:{} -> outSrc:{}",fromSrc, outSrc);
        lockMap.put(key,Arrays.asList(new AtomicBoolean(false)));
        //采集或推流导致的错误次数
        long err_index = 0;
        runOnOk();
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

                //2.帧截图
                if(tunOn.get()){
                    long target = System.currentTimeMillis();

                    Frame frame = grabber.grabImage();
                    System.out.println("spend time: " +(System.currentTimeMillis() - target));
//                    if (frame != null) {
//                        BufferedImage buff = frameToBufferedImage(frame);
//                        if(buff != null) {
//                            imageProcessing(buff, imagePath,key);
//                        }
//                    }
                }

            } catch (Exception e) {//推流失败
                err_index++;
                e.printStackTrace();
            }
        }
        log.warn("连接断开");
    }


    private Map<Integer,List<AtomicBoolean>> lockMap =  new HashMap<>();

    private AtomicBoolean tunOn = new AtomicBoolean(false);

    private static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(5,5,5000,
            TimeUnit.MILLISECONDS,new LinkedBlockingQueue());

    private void  imageProcessing(final BufferedImage buff,String imagePath,int key){
        threadPool.execute(()->{
            List<AtomicBoolean> locks = lockMap.get(key);
            for (int i = 0; i < locks.size(); i++) {
                if(!locks.get(i).get()){
                    locks.get(i).set(true);
                    try {
                        System.out.println(LocalTime.now()+":"+key+"_"+i+"拿到锁了");
                        File outPut = new File(imagePath+key+"_"+i+".jpg");
                        ImageIO.write(buff, "jpg", outPut);
                        Thread.sleep(5000);
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    } finally {
                        locks.get(i).set(false);
                        System.out.println(LocalTime.now()+":"+key+"_"+i+"释放锁");
                        break;
                    }
                }
            }
        });
    }

    private void runOnOk(){
        threadPool.execute(()->{
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                tunOn.set(true);
            }
        });
    }

    /**
     * 创建BufferedImage对象
     */
    private BufferedImage frameToBufferedImage(Frame frame) {
        Java2DFrameConverter converter = new Java2DFrameConverter();
        BufferedImage bufferedImage = converter.getBufferedImage(frame);
//		bufferedImage=rotateClockwise90(bufferedImage);
        return bufferedImage;
    }

    /**
     * 处理图片，将图片旋转90度。
     */
    private BufferedImage rotateClockwise90(BufferedImage bi) {
        int width = bi.getWidth();
        int height = bi.getHeight();
        BufferedImage bufferedImage = new BufferedImage(height, width, bi.getType());
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                bufferedImage.setRGB(j, i, bi.getRGB(i, j));
            }
        }
        return bufferedImage;
    }
}