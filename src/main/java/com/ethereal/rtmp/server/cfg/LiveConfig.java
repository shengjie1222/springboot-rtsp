package com.ethereal.rtmp.server.cfg;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Administrator
 */
@Data
@Component
@ConfigurationProperties(prefix = "live")
public class LiveConfig {
	private int rtmpPort;
	private int httpFlvPort;
	private boolean saveFlvFile;
	private String saveFlVFilePath;
	private int handlerThreadPoolSize;
	private boolean enableHttpFlv;
	
	
}
