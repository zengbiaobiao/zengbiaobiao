# Spring 整合MQTT

Spring提供了对多种消息中间件的整合，其中也包括MQTT。具体请参见以下链接：

https://docs.spring.io/spring-integration/reference/html/

Spring整合MQTT步骤如下：

1. 创建Spring Boot Maven工程，引入如下依赖：

   ```
   <dependencies>
   		<dependency>
   			<groupId>org.springframework.boot</groupId>
   			<artifactId>spring-boot-starter-web</artifactId>
   		</dependency>
   
   		<dependency>
   			<groupId>org.springframework.boot</groupId>
   			<artifactId>spring-boot-starter-test</artifactId>
   			<scope>test</scope>
   		</dependency>
   
   		<dependency>
   			<groupId>org.springframework.integration</groupId>
   			<artifactId>spring-integration-mqtt</artifactId>
   			<version>5.1.3.RELEASE</version>
   		</dependency>
   	</dependencies>
   ```

2. 配置MQTT消费端

   添加SpringConfig.java类，添加消息消费Bean

   ```
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
       //ServiceActivator注解表明当前方法用于处理MQTT消息，inputChannel参数指定了用于接收消息信息的channel。
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
   ```

   @ServiceActivator注解表明当前方法用于处理MQTT消息，inputChannel参数指定了用于接收消息的channel。

   当接收到消息时，可以先拿到topic，然后根据不同的topic分别对消息进行处理。

3. 配置MQTT消息发送端。

   在MQTT使用场景中，一般处理接收消息的同时，也会发送消息。在SpringConfig.java配置文件中添加如下Bean注入，用于消息发送。

   ```
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
   ```

   @MessagingGateway是一个用于提供消息网关代理整合的注解，参数defaultRequestChannel指定发送消息绑定的channel。

   在这里我们定义了MqttGateway接口，该接口可以被注入到其它类中，用于消息发送。

   ```
   	@Autowired
       private SpringConfig.MqttGateway mqttGateway;
       
       // 然后在类方法中调用下面方法发送消息
       // mqttGateway.sendToMqtt("testTopic",message);
   ```

4. 创建Rest Controller，通过http请求发送MQTT消息。

   ```
   @RestController
   public class MqttController {
   
       @Autowired
       private SpringConfig.MqttGateway mqttGateway;
   
       @RequestMapping("/send/{topic}/{message}")
       public String send(@PathVariable String topic, @PathVariable String message) {
           mqttGateway.sendToMqtt(topic, message);
           return "send message : " + message;
       }
   }
   ```

   在浏览器中输入如下网址进行测试。

   http://localhost:8080/send/topic1/message1

   http://localhost:8080/send/topic2/message2

5. 项目源代码可参考https://github.com/40925645/zengbiaobiao/new/master/mqtt-demo
