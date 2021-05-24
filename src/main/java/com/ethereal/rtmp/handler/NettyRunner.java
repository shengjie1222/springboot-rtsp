package com.ethereal.rtmp.handler;

import com.ethereal.rtmp.common.Options;
import com.ethereal.rtmp.server.manager.StreamManager;
import com.ethereal.rtmp.server.HttpFlvServer;
import com.ethereal.rtmp.server.RTMPServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Order(1)
public class NettyRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {

            StreamManager streamManager = new StreamManager();

            int rtmpPort = Options.liveConfig.getRtmpPort();
            int handlerThreadPoolSize= Options.liveConfig.getHandlerThreadPoolSize();

            RTMPServer rtmpServer = new RTMPServer(rtmpPort, streamManager,handlerThreadPoolSize);
            rtmpServer.run();

            if (!Options.liveConfig.isEnableHttpFlv()) {
                return;
            }

            int httpPort = Options.liveConfig.getHttpFlvPort();
            HttpFlvServer httpFlvServer = new HttpFlvServer(httpPort, streamManager,handlerThreadPoolSize);
            httpFlvServer.run();

    }
}
