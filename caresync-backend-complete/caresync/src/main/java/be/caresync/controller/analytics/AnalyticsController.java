package be.caresync.controller.analytics;

import be.caresync.domain.iot.DeviceType;
import be.caresync.dto.response.*;
import be.caresync.service.analytics.AnalyticsDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & Tableaux de Bord", description = "KPIs et indicateurs qualité")
public class AnalyticsController {

    private final AnalyticsDashboardService dashboardService;

    @GetMapping("/dashboard/operational")
    @Operation(summary = "Tableau de bord opérationnel temps réel")
    @PreAuthorize("hasAnyRole('MEDECIN','INFIRMIER','ADMIN','COORDINATEUR')")
    public ResponseEntity<OperationalDashboardResponse> getOperational() {
        return ResponseEntity.ok(dashboardService.getOperationalDashboard());
    }

    @GetMapping("/dashboard/tactical/{patientId}")
    @Operation(summary = "Tableau de bord tactique 7 jours par patient")
    @PreAuthorize("hasAnyRole('MEDECIN','COORDINATEUR','ADMIN')")
    public ResponseEntity<TacticalDashboardResponse> getTactical(@PathVariable UUID patientId) {
        return ResponseEntity.ok(dashboardService.getTacticalDashboard(patientId));
    }

    @GetMapping("/dashboard/strategic")
    @Operation(summary = "Tableau de bord stratégique 30 jours")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StrategicDashboardResponse> getStrategic() {
        return ResponseEntity.ok(dashboardService.getStrategicDashboard());
    }

    @GetMapping("/timeseries/{patientId}/{deviceType}")
    @Operation(summary = "Série temporelle d'une mesure — pour les graphiques")
    @PreAuthorize("hasAnyRole('MEDECIN','INFIRMIER','ADMIN')")
    public ResponseEntity<TimeSeriesResponse> getTimeSeries(
            @PathVariable UUID       patientId,
            @PathVariable DeviceType deviceType,
            @RequestParam(defaultValue = "24") int lastHours) {

        return ResponseEntity.ok(
                dashboardService.getTimeSeries(patientId, deviceType, lastHours));
    }
}
