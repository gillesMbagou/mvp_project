package be.caresync.common.events;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

/** Déserialiseur Kafka JSON pour IoTObservationEvent (topic caresync.iot.observations). */
public class IoTObservationEventDeserializer extends ObjectMapperDeserializer<IoTObservationEvent> {

    public IoTObservationEventDeserializer() {
        super(IoTObservationEvent.class);
    }
}
