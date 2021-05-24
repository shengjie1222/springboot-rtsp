package com.ethereal.rtmp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author Administrator
 */
@SpringBootApplication
@EnableAsync
@Slf4j
public class VideoApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(VideoApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(VideoApplication.class);
	}

}
