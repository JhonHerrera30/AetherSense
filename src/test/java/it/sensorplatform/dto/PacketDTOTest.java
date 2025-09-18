package it.sensorplatform.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.sensorplatform.model.Indicator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesIndicatorObjectsIntoCanonicalStrings() throws Exception {
        String json = """
                {
                  "indicator": [
                    "plain",
                    {"key": "fan_err", "label": "Fan Error"},
                    {"key": "status", "name": "Status"},
                    {"label": "Only Label"},
                    {"name": "Name Only"},
                    {"key": "solo_key"},
                    {"key": "spaced", "label": " Trim "}
                  ]
                }
                """;

        PacketDTO dto = objectMapper.readValue(json, PacketDTO.class);

        assertEquals(List.of(
                "plain",
                "fan_err:Fan Error",
                "status:Status",
                "Only Label",
                "Name Only",
                "solo_key",
                "spaced:Trim"
        ), dto.getIndicator());
    }

    @Test
    void setterAcceptsPojoIndicators() {
        PacketDTO dto = new PacketDTO();
        Indicator indicator = new Indicator();
        indicator.setKey("pojo_key");
        indicator.setName("Pojo Name");

        dto.setIndicator(List.of(indicator));

        assertEquals(List.of("pojo_key:Pojo Name"), dto.getIndicator());
    }
}
