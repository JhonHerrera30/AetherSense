package it.sensorplatform.dto;

import java.util.List;

public class AlertConfigDTO {

    // config globale
    private int globalIntervalMin;
    private String telegramChatId;
    private String telegramInviteLink;

    public static class GlobalConfig {
        private int intervalMin;
        private String telegramChatId;
        private String telegramInviteLink;

        public int getIntervalMin() {
            return intervalMin;
        }

        public void setIntervalMin(int intervalMin) {
            this.intervalMin = intervalMin;
        }

        public String getTelegramChatId() {
            return telegramChatId;
        }

        public void setTelegramChatId(String s) {
            this.telegramChatId = s;
        }

        public String getTelegramInviteLink() {
            return telegramInviteLink;
        }

        public void setTelegramInviteLink(String s) {
            this.telegramInviteLink = s;
        }
    }

    // config per segnale
    private List<SignalConfig> signals;

    public static class SignalConfig {
        private String signalKey;
        private Double thresholdWarning;
        private Double thresholdCritical;
        private Integer triggerValue;
        private Integer intervalMin; // null = usa globale
        private Double thresholdWarningLow;
        private Double thresholdCriticalLow;

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

        public Double getThresholdWarningLow() {
            return thresholdWarningLow;
        }

        public void setThresholdWarningLow(Double v) {
            this.thresholdWarningLow = v;
        }

        public Double getThresholdCriticalLow() {
            return thresholdCriticalLow;
        }

        public void setThresholdCriticalLow(Double v) {
            this.thresholdCriticalLow = v;
        }
    }

    public int getGlobalIntervalMin() {
        return globalIntervalMin;
    }

    public void setGlobalIntervalMin(int globalIntervalMin) {
        this.globalIntervalMin = globalIntervalMin;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public String getTelegramInviteLink() {
        return telegramInviteLink;
    }

    public void setTelegramInviteLink(String telegramInviteLink) {
        this.telegramInviteLink = telegramInviteLink;
    }

    public List<SignalConfig> getSignals() {
        return signals;
    }

    public void setSignals(List<SignalConfig> signals) {
        this.signals = signals;
    }

}