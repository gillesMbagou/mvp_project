package be.caresync.dto.response;

import be.caresync.domain.iot.DeviceType;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StrategicDashboardResponse {
    private int     periodDays;
    private long    totalAlertsGenerated;
    private long    criticalAlertsCount;
    private double  escalationRatePercent;
    private Instant generatedAt;
}
