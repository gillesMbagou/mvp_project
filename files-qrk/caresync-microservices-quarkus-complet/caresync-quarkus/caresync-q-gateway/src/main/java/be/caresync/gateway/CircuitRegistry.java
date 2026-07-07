// caresync-q-gateway/src/main/java/be/caresync/gateway/CircuitRegistry.java
package be.caresync.gateway;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Circuit breaker léger par service.
 * En production : utiliser @CircuitBreaker de SmallRye Fault Tolerance.
 */
@ApplicationScoped
public class CircuitRegistry {

    private static final Logger log = Logger.getLogger(CircuitRegistry.class);

    record CircuitState(AtomicInteger failures, Instant openedAt, boolean open) {}

    private final Map<String, CircuitState> circuits = new ConcurrentHashMap<>();
    private static final int THRESHOLD   = 5;      // échecs avant ouverture
    private static final int WAIT_SEC    = 10;     // secondes en état OPEN

    public boolean isOpen(String host) {
        CircuitState state = circuits.get(host);
        if (state == null || !state.open()) return false;

        // Tenter la réouverture après WAIT_SEC
        if (Instant.now().isAfter(state.openedAt().plusSeconds(WAIT_SEC))) {
            circuits.put(host, new CircuitState(new AtomicInteger(0), null, false));
            log.infof("Circuit [%s] → HALF-OPEN", host);
            return false;
        }
        return true;
    }

    public void recordSuccess(String host) {
        circuits.computeIfPresent(host, (k, s) ->
            new CircuitState(new AtomicInteger(0), null, false));
    }

    public void recordFailure(String host) {
        CircuitState state = circuits.computeIfAbsent(host,
            k -> new CircuitState(new AtomicInteger(0), null, false));
        int count = state.failures().incrementAndGet();
        if (count >= THRESHOLD && !state.open()) {
            circuits.put(host, new CircuitState(state.failures(), Instant.now(), true));
            log.warnf("Circuit [%s] → OPEN (%d échecs)", host, count);
        }
    }
}