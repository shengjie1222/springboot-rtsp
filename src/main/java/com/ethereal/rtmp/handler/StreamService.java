package com.ethereal.rtmp.handler;

import com.ethereal.rtmp.common.Options;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author Administrator
 */
@Service
@Slf4j
public class StreamService {

    @Async
    public void play(String streamUrl,Integer i) {
        while (true){
            try {
                new ConvertVideoPacket()
                        .from(streamUrl)
                        .to("rtmp://127.0.0.1:"+ Options.liveConfig.getRtmpPort()+"/live/"+i)
                        .go();
            } catch (Exception e) {
                e.printStackTrace();
            }  finally {
                log.warn("connection break,retrying.....");
            }
        }
    }
}
