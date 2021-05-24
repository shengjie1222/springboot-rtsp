package com.ethereal.rtmp.handler;

import com.ethereal.rtmp.server.cfg.GrabberConfig;
import com.ethereal.rtmp.vo.Url;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
@Slf4j
@Order(2)
public class StreamRunner implements ApplicationRunner{

    @Autowired
    private GrabberConfig grabberConfig;

    @Autowired
    private StreamService streamService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Url> urls = grabberConfig.getUrls();
        if(!CollectionUtils.isEmpty(urls)){
            for (Url url : urls) {
                Integer i = url.getKey();
                String streamUrl = url.getStreamUrl();
                streamService.play(streamUrl,i);
            }
        }
    }

}
