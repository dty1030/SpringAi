package com.maorou.SpringAIDemo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RefreshScope
public class LBController {

    @Value("${demo.greeting}")
    String greeting;

    @GetMapping("/greeting")
    public String greeting(){
        return greeting;
    }

    @Value("${server.port}")
    String port;

    @GetMapping("/whoami")
    public String whoami(){
        return "Im instance " + port;
    }

    @Autowired
    RestTemplate restTemplate;

    @GetMapping("/call")
    public String call(){
        return restTemplate.getForObject("http://SpringAIDemo/whoami", String.class);
    }

    @GetMapping("/callweather")
    public String callweather(@RequestParam String city){
        return restTemplate.getForObject("http://tool-service/weather?city={city}", String.class, city);
    }
}
