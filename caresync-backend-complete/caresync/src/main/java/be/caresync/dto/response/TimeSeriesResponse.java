package be.caresync.dto.response;

import be.caresync.domain.iot.DeviceType;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TimeSeriesResponse {
    private UUID            patientId;
    private DeviceType      deviceType;
    private String          loincCode;
    private String          unit;
    private Instant         from;
    private Instant         to;
    private List<DataPoint> points;

    @Data @AllArgsConstructor
    public static class DataPoint {
        private Instant    timestamp;
        private BigDecimal value;
    }
}
