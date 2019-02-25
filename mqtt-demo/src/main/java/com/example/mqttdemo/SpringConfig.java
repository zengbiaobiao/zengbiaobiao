package com.example.mqttdemo;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;

@Configuration
public class SpringConfig {

    // 消费消息


    /*****
     * 创建MqttPahoClientFactory，设置MQTT Broker连接属性，如果使用SSL验证，也在这里设置。
     * @return
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{"tcp://10.69.94.176:1883"});
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("consumerClient",
                mqttClientFactory(), "topic1", "topic2");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    //ServiceActivator注解表明当前方法用于处理MQTT消息，inputChannel参数指定了用于消费消息的channel。
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return message -> {
            String payload = message.getPayload().toString();
            String topic = message.getHeaders().get("mqtt_receivedTopic").toString();
            // 根据topic分别进行消息处理。
            if (topic.equals("topic1")) {
                System.out.println("topic1: 处理消息 " + payload);
            } else if (topic.equals("topic2")) {
                System.out.println("topic2: 处理消息 " + payload);
            } else {
                System.out.println(topic + ": 丢弃消息 " + payload);
            }
        };
    }


    // 发送消息

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /*****
     * 发送消息和消费消息Channel可以使用相同MqttPahoClientFactory
     * @return
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler outbound() {
        // 在这里进行mqttOutboundChannel的相关设置
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler("publishClient", mqttClientFactory());
        messageHandler.setAsync(true); //如果设置成true，发送消息时将不会阻塞。
        messageHandler.setDefaultTopic("testTopic");
        return messageHandler;
    }

    @MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
    public interface MqttGateway {
        // 定义重载方法，用于消息发送
        void sendToMqtt(String payload);
        // 指定topic进行消息发送
        void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, String payload);
        void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, @Header(MqttHeaders.QOS) int qos, String payload);
    }
}
