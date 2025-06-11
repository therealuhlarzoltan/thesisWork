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
        List<TimetableResponse.TimetableEntry> filteredEntries = new ArrayList<>();

        if (timetableNode != null && timetableNode.isArray()) {
            for (JsonNode entryNode : timetableNode) {
                boolean hasLocalTransport = false;

                JsonNode detailsNode = entryNode.get("details");
                if (detailsNode != null && detailsNode.isArray()) {
                    for (JsonNode detailNode : detailsNode) {
                        JsonNode trainInfoNode = detailNode.get("train_info");
                        if (trainInfoNode != null && trainInfoNode.path("is_local_transport").asBoolean(false)) {
                            hasLocalTransport = true;
                            break;
                        }
                    }
                }

                if (!hasLocalTransport) {
                    TimetableResponse.TimetableEntry entry =
                            mapper.treeToValue(entryNode, TimetableResponse.TimetableEntry.class);
                    filteredEntries.add(entry);
                }
            }
        }

        return new TimetableResponse(filteredEntries);
    }
}