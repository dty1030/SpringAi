package com.maorou.SpringAIDemo.controller;

import com.maorou.SpringAIDemo.feign.WeatherFeignClient;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LBController.class);

    @Autowired
    WeatherFeignClient weatherFeignClient;          // 注入 Feign客户端

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

    @GetMapping("/feignweather")
    public String feignweather(@RequestParam String city){
        log.info("chat 准备调用 tool-service, city={}", city);
        return weatherFeignClient.getWeather(city); // 直接走Feign，不经 LLM、不经 RestTemplate
    }
}
