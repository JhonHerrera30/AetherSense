package it.sensorplatform.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "sample", uniqueConstraints =  @UniqueConstraint(columnNames = {"dev_eui", "timestamp"}))
@Getter
@Setter
@NoArgsConstructor

public class SampleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "dev_eui")
    private String devEui;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @OneToMany(mappedBy = "sample", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeasurementEntity> measurements = new ArrayList<>();

    public SampleEntity(String deviceId, String devEui, Instant timestamp){
        this.devEui = devEui;
        this.deviceId = deviceId;
        this.timestamp = timestamp;
    }

    public void addMeasurement(MeasurementEntity m) {
        measurements.add(m);
        m.setSample(this);
    }

   




}
