package com.maorou.SpringAIDemo.functions;


import com.maorou.SpringAIDemo.feign.WeatherFeignClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestTemplate;

public class WeatherTools {

//    private final RestTemplate restTemplate = new RestTemplate();
//
//    @Tool(description = "获取指定城市天气. 当用户询问'某个城市今天天气怎么样'等天气相关问题时，必须调用此工具")
//    public String getWeather(@ToolParam(description = "要查询天气的城市名称") String city){
//        return restTemplate.getForObject("https://wttr.in/{city}?format=3", String.class, city);
//    }

    private final WeatherFeignClient weatherFeignClient;
    public WeatherTools(WeatherFeignClient weatherFeignClient){
        this.weatherFeignClient = weatherFeignClient;
    }
    @Tool(description = "获取指定城市天气. 当用户询问'某个城市今天天气怎么样'等天气相关问题时，必须调用此工具")
    public String getWeather(@ToolParam(description = "要查询天气的城市名称") String city){
          return weatherFeignClient.getWeather(city);}

}
