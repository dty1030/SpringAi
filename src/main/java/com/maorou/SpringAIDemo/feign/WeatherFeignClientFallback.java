package com.maorou.SpringAIDemo.feign;

import org.springframework.stereotype.Component;

@Component
public class WeatherFeignClientFallback implements WeatherFeignClient{
    @Override
    public String getWeather(String city){
        return "天气服务暂时不可用, 请稍后再试（城市:\n " + city + ")";
    }

}
