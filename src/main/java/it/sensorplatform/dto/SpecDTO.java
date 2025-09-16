package it.sensorplatform.dto;

import it.sensorplatform.model.Spec;

public record SpecDTO(String measurement, String unitOfMeasurement, String component) {

    public static SpecDTO fromSpec(Spec spec) {
        if (spec == null) {
            return new SpecDTO(null, null, null);
        }
        return new SpecDTO(spec.getMeasurement(), spec.getUnitOfMeasurement(), spec.getComponent());
    }
}

