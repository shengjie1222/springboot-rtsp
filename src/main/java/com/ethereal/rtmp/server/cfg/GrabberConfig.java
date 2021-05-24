package com.ethereal.rtmp.server.cfg;

import com.ethereal.rtmp.vo.Url;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Administrator
 * @Description
 * @create 2021-05-21 14:18
 */
@Data
@Component
@ConfigurationProperties(prefix = "grabber")
public class GrabberConfig {

    private List<Url> urls;

}
