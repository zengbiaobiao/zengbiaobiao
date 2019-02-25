package com.example.mqttdemo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
public class MqttController {

    @Autowired
    private SpringConfig.MqttGateway mqttGateway;

    @RequestMapping("/send/{topic}/{message}")
    public String send(@PathVariable String topic, @PathVariable String message) {
        // 发送消息到指定topic
        mqttGateway.sendToMqtt(topic, message);
        return "send message : " + message;
    }
}
