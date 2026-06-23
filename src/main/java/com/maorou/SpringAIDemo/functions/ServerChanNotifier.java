package com.maorou.SpringAIDemo.functions;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
//PushDeer,
@Component
public class ServerChanNotifier {
    private final String sendKey;
    private final RestTemplate restTemplate = new RestTemplate();

    public ServerChanNotifier(@Value("${app.push.serverchan.key:}") String sendKey){
        this.sendKey = sendKey;
    }
    public void push(String title, String content) {
        restTemplate.getForObject(
                "https://sctapi.ftqq.com/{key}.send?title={title}&desp={desp}",
                String.class,
                sendKey, title, content);     // {} 占位符 RestTemplate 自动 URL 编码,中文不乱
    }
}
