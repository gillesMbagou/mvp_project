package be.caresync.demo.streaming;

import be.caresync.demo.model.db.ObservationRecord;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Bus d'événements réactif — hot publisher partagé entre le simulateur,
 * le bridge MQTT et les endpoints SSE.
 *
 * Chaque client SSE reçoit les événements à partir du moment où il souscrit.
 * Le buffer de 512 éléments absorbe les pics d'émission sans bloquer les producteurs.
 */
@Component
public class ReactiveEventBus {

    private final Sinks.Many<ObservationRecord> sink =
            Sinks.many()
                 .multicast()
                 .onBackpressureBuffer(512, false);  // false = ne pas échouer si 0 souscripteurs

    public void publish(ObservationRecord obs) {
        sink.tryEmitNext(obs);
    }

    public Flux<ObservationRecord> asFlux() {
        return sink.asFlux();
    }
}
