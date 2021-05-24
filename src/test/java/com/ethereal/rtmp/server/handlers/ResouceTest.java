package com.ethereal.rtmp.server.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.ethereal.rtmp.server.cfg.LiveConfig;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

public class ResouceTest {
    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Resource resource = new ClassPathResource("rtmpServer.yml");
        File targetFile = resource.getFile();

        LiveConfig cfg = mapper.readValue(targetFile, LiveConfig.class);


        int a=1;
    }
}
