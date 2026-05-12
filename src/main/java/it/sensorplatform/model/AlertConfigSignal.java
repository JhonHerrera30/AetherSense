package it.sensorplatform.model;

import jakarta.persistence.*;

@Entity
@Table(name = "alert_config_signal", uniqueConstraints = @UniqueConstraint(columnNames = { "project_id",
        "signal_key" }))
public class AlertConfigSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "signal_key", nullable = false)
    private String signalKey;

    @Column(name = "threshold_warning")
    private Double thresholdWarning;

    @Column(name = "threshold_critical")
    private Double thresholdCritical;

    // per booleani/stati: valore che scatta l'alert (es. 1)
    @Column(name = "trigger_value")
    private Integer triggerValue;

    // null = usa il timer globale del progetto
    @Column(name = "interval_min")
    private Integer intervalMin;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getSignalKey() {
        return signalKey;
    }

    public void setSignalKey(String signalKey) {
        this.signalKey = signalKey;
    }

    public Double getThresholdWarning() {
        return thresholdWarning;
    }

    public void setThresholdWarning(Double thresholdWarning) {
        this.thresholdWarning = thresholdWarning;
    }

    public Double getThresholdCritical() {
        return thresholdCritical;
    }

    public void setThresholdCritical(Double thresholdCritical) {
        this.thresholdCritical = thresholdCritical;
    }

    public Integer getTriggerValue() {
        return triggerValue;
    }

    public void setTriggerValue(Integer triggerValue) {
        this.triggerValue = triggerValue;
    }

    public Integer getIntervalMin() {
        return intervalMin;
    }

    public void setIntervalMin(Integer intervalMin) {
        this.intervalMin = intervalMin;
    }
}