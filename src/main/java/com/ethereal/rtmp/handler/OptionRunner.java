package com.ethereal.rtmp.handler;

import com.ethereal.rtmp.server.cfg.LiveConfig;
import com.ethereal.rtmp.common.Options;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@Order(0)
public class OptionRunner implements ApplicationRunner, EnvironmentAware {

    @Autowired
    private LiveConfig liveConfig;

    private Binder binder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initOptions();
    }

    /**
     * init options
     */
    private void initOptions(){
        Map<String, String> grabberOptions = binder.bind("grabber.options", Bindable.mapOf(String.class, String.class)).get();
        Map<String, String> recorderVideoOptions = binder.bind("recorder.options.video", Bindable.mapOf(String.class, String.class)).get();
        Map<String, String> recorderAudioOptions = binder.bind("recorder.options.audio", Bindable.mapOf(String.class, String.class)).get();
        Options.grabberPutAll(grabberOptions);
        Options.Video.recorderPutAll(recorderVideoOptions);
        Options.Audio.recorderPutAll(recorderAudioOptions);
        Options.liveConfig = liveConfig;
    }

    @Override
    public void setEnvironment(Environment environment) {
        binder = Binder.get(environment);
    }
}
