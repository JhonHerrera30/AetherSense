// src/main/java/it/sensorplatform/dto/TtnWebhookDto.java
package it.sensorplatform.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TtnWebhookDTO {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EndDeviceIds {
        private String device_id;
        private String dev_eui;
        public String getDevice_id() { return device_id; }
        public void setDevice_id(String v) { this.device_id = v; }
        public String getDev_eui() { return dev_eui; }
        public void setDev_eui(String v) { this.dev_eui = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UplinkMessage {
        private Integer f_port;
        private String frm_payload;                    // base64
        private Map<String,Object> decoded_payload;    // output del decoder TTN
        // opzionali ma comuni: rx_metadata, settings, consumed_airtime, ecc.
        public Integer getF_port() { return f_port; }
        public void setF_port(Integer v) { this.f_port = v; }
        public String getFrm_payload() { return frm_payload; }
        public void setFrm_payload(String v) { this.frm_payload = v; }
        public Map<String, Object> getDecoded_payload() { return decoded_payload; }
        public void setDecoded_payload(Map<String, Object> v) { this.decoded_payload = v; }
    }

    private EndDeviceIds end_device_ids;
    private String received_at;                       // ISO 8601
    private UplinkMessage uplink_message;

    public EndDeviceIds getEnd_device_ids() { return end_device_ids; }
    public void setEnd_device_ids(EndDeviceIds v) { this.end_device_ids = v; }
    public String getReceived_at() { return received_at; }
    public void setReceived_at(String v) { this.received_at = v; }
    public UplinkMessage getUplink_message() { return uplink_message; }
    public void setUplink_message(UplinkMessage v) { this.uplink_message = v; }
}
