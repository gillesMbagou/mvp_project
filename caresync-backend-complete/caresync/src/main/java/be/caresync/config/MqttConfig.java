package be.caresync.config;

import be.caresync.messaging.mqtt.MqttMessageHandler;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

@Configuration
public class MqttConfig {

    @Value("${caresync.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${caresync.mqtt.username:caresync}")
    private String username;

    @Value("${caresync.mqtt.password:mqtt_secret}")
    private String password;

    // Topics IoT abonnés — wildcards supportés
    private static final String[] IOT_TOPICS = {
        "caresync/devices/+/glucose",
        "caresync/devices/+/spo2",
        "caresync/devices/+/bloodpressure",
        "caresync/devices/+/weight",
        "caresync/devices/+/heartrate",
        "caresync/devices/+/temperature",
        "caresync/devices/+/ecg",
    };

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setServerURIs(new String[]{ brokerUrl });
        opts.setUserName(username);
        opts.setPassword(password.toCharArray());
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(false);        // Persist subscriptions
        opts.setConnectionTimeout(10);
        opts.setKeepAliveInterval(30);

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(opts);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttAdapter(MqttPahoClientFactory factory) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        "caresync-backend-" + System.currentTimeMillis(),
                        factory,
                        IOT_TOPICS
                );
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setOutputChannel(mqttInputChannel());
        adapter.setQos(1);  // At least once delivery
        return adapter;
    }
}
