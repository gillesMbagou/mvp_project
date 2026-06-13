package be.caresync.demo.model;

public record AlertEvent(
    String alertId,
    String deviceSerial,
    String deviceName,
    String severity,
    String message,
    String triggeredValue,
    String thresholdValue,
    String createdAt,
    int    sourcePartition,
    long   sourceOffset
) {}
