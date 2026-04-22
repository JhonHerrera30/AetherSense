package it.sensorplatform.model;

import it.sensorplatform.service.IngestService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class MeasurementEntity {
    public enum MeasurementType {
        MEASUREMENT, INDICATOR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MeasurementType type;

    @Column(name = "key_name")
    private String key;

    @Column(name = "label")
    private String label;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "component")
    private String component;

    @Column(name = "unit")
    private String unit;

    @Column(name = "min")
    private Double min;

    @Column(name = "max")
    private Double max;

    @Column(name = "double_value")
    private Double doubleValue;

    @Column(name = "int_value")
    private Integer intValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false)
    private SampleEntity sample;
    
    public static MeasurementEntity fromMeasurement(
        IngestService.MeasurementSample m) {
    
    MeasurementEntity e = new MeasurementEntity();
    e.type = MeasurementType.MEASUREMENT;
    e.key = m.key();
    e.label = m.label();
    e.displayName = m.displayName();
    e.component = m.component();
    e.unit = m.unit();
    e.min = m.min();
    e.max = m.max();
    e.doubleValue = m.value();
    return e;
    }

    public static MeasurementEntity fromIndicator(
            IngestService.IndicatorSample i) {          
        MeasurementEntity e = new MeasurementEntity();
        e.type = MeasurementType.INDICATOR;
        e.key = i.key();
        e.label = i.label();
        e.intValue = i.value();
        return e;
    }
}
