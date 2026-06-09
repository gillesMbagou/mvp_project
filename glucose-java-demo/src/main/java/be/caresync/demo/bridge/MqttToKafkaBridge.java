package be.caresync.demo.bridge;

import be.caresync.demo.model.GlucoseObservation;
import be.caresync.demo.model.db.ObservationRecord;
import be.caresync.demo.streaming.ReactiveEventBus;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Pont MQTT → Kafka — 100% Java Spring Integration.
 * Remplace intégralement bridge.py.
 *
 * Flux :
 *   Mosquitto (MQTT) → MqttChannelAdapter → mqttInputChannel
 *       → MqttToKafkaHandler → Kafka topic glucose.raw
 */
@Configuration
@EnableIntegration
@Slf4j
class MqttConfig {

    @Value("${demo.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    // Topics MQTT écoutés — wildcard + sur tous les types de mesures
    private static final String[] TOPICS = {
        "caresync/devices/+/glucose",
        "caresync/devices/+/spo2",
        "caresync/devices/+/heartrate",
        "caresync/devices/+/weight",
    };

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setServerURIs(new String[]{ brokerUrl });
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(false);
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
                        "caresync-bridge-" + System.currentTimeMillis(),
                        factory,
                        TOPICS
                );
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setOutputChannel(mqttInputChannel());
        adapter.setQos(1);
        return adapter;
    }
}

@Component
@RequiredArgsConstructor
@Slf4j
public class MqttToKafkaBridge {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper                  objectMapper;
    private final ReactiveEventBus              eventBus;

    @Value("${app.topics.glucose-raw:glucose.raw}")
    private String kafkaTopic;

    /**
     * Point d'entrée : reçoit chaque message MQTT et le publie sur Kafka.
     * Spring Integration appelle cette méthode automatiquement.
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handle(Message<String> message) {
        Instant received = Instant.now();
        String  topic    = (String) message.getHeaders().get("mqtt_receivedTopic");
        String  payload  = message.getPayload();

        log.debug("MQTT reçu — topic={}", topic);

        try {
            GlucoseObservation obs = objectMapper.readValue(payload, GlucoseObservation.class);

            // Enrichissement bridge
            obs.setMqttTopic(topic);
            long latencyMs = received.toEpochMilli() - obs.getTs();
            obs.setLatencyMs(latencyMs);

            // Diffuser aux clients SSE (données provenant de vrais dispositifs via MQTT)
            eventBus.publish(ObservationRecord.builder()
                    .deviceSerial(obs.getSerial())
                    .deviceType("GLUCOMETER")
                    .loincCode(obs.getLoincCode())
                    .value(obs.getValue())
                    .unit(obs.getUnit())
                    .severity(obs.computeSeverity())
                    .source(obs.getSource())
                    .observedAt(obs.getObservedAt())
                    .build());

            // Clé Kafka = serial du dispositif (ordre garanti par device)
            String key = obs.getSerial();

            kafkaTemplate.send(kafkaTopic, key, obs)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Erreur Kafka : {}", ex.getMessage());
                        } else {
                            log.info(
                                "→ Kafka [{}] partition={} offset={} | {} g/L | {} | latence={}ms",
                                kafkaTopic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                String.format("%.2f", obs.getValue()),
                                obs.computeSeverity(),
                                latencyMs
                            );
                        }
                    });

        } catch (Exception ex) {
            log.error("Erreur traitement message MQTT topic={} : {}", topic, ex.getMessage());
        }
    }
}
