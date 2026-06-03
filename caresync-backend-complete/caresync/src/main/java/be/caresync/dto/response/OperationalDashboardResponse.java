package be.caresync.dto.response;

import be.caresync.domain.iot.DeviceType;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OperationalDashboardResponse {
    private long                      activeAlertsCount;
    private long                      criticalAlertsCount;
    private List<AlertSummaryResponse> recentAlerts;
    private Instant                   generatedAt;
}
