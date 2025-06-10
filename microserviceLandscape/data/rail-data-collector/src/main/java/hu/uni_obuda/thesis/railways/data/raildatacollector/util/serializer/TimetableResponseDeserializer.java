package hu.uni_obuda.thesis.railways.data.raildatacollector.util.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.TimetableResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TimetableResponseDeserializer extends JsonDeserializer<TimetableResponse> {

    @Override
    public TimetableResponse deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode root = mapper.readTree(p);

        JsonNode timetableNode = root.get("timetable");
        List<TimetableResponse.TimetableEntry> passengerEntries = new ArrayList<>();

        if (timetableNode != null && timetableNode.isArray()) {
            for (JsonNode entryNode : timetableNode) {
                String type = entryNode.path("type").asText();
                if ("passenger".equals(type)) {
                    TimetableResponse.TimetableEntry entry = mapper.treeToValue(entryNode, TimetableResponse.TimetableEntry.class);
                    passengerEntries.add(entry);
                }
            }
        }
        return new TimetableResponse(passengerEntries);
    }
}