package it.sensorplatform.dto;

/**
 * Request payload used by operators to accept or refuse an activation.
 */
public record OperatorActivationDecision(Boolean accepted, Double latitude, Double longitude) {
}
