package com.maorou.SpringAIDemo.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "tool-service", fallback = WeatherFeignClientFallback.class)
public interface WeatherFeignClient {

    @GetMapping("/weather") //表示 我要去调对方的 /weather 路径
    String getWeather(@RequestParam(value = "city") String city);
}
