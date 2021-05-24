package com.ethereal.rtmp.controllers;

import com.ethereal.rtmp.server.cfg.GrabberConfig;
import com.ethereal.rtmp.vo.Url;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Administrator
 * @Description
 * @create 2021-05-24 14:49
 */
@Controller
public class MenuController {

    @Autowired
    private GrabberConfig grabberConfig;

    @RequestMapping("/menu")
    public String menu(Model model){
        List<Integer> sourceNum = grabberConfig.getUrls().stream().map(Url::getKey).distinct().sorted().collect(Collectors.toList());
        model.addAttribute("SourceNum",sourceNum);
        return "index";
    }

}
