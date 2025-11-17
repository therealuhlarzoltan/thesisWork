package hu.uni_obuda.thesis.railways.data.raildatacollector.util.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraTimetableResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TimetableResponseDeserializerTest {

    private ObjectMapper createObjectMapperWithDeserializer() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ElviraTimetableResponse.class, new TimetableResponseDeserializer());
        mapper.registerModule(module);
        return mapper;
    }

    @Test
    public void deserialize_entryWithLocalTransport_filteredOut() throws Exception {
        String json = """
                {
                  "timetable": [
                    {
                      "details": [
                        {
                          "train_info": {
                            "is_local_transport": true,
                            "url": "url1",
                            "get_url": "getUrl1",
                            "code": "code1",
                            "vsz_code": "vsz1"
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        ObjectMapper mapper = createObjectMapperWithDeserializer();
        ElviraTimetableResponse response = mapper.readValue(json, ElviraTimetableResponse.class);

        assertNotNull(response.getTimetable());
        assertTrue(response.getTimetable().isEmpty());
    }

    @Test
    public void deserialize_entryWithoutLocalTransport_kept() throws Exception {
        String json = """
                {
                  "timetable": [
                    {
                      "details": [
                        {
                          "train_info": {
                            "is_local_transport": false,
                            "url": "url1",
                            "get_url": "getUrl1",
                            "code": "code1",
                            "vsz_code": "vsz1"
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        ObjectMapper mapper = createObjectMapperWithDeserializer();
        ElviraTimetableResponse response = mapper.readValue(json, ElviraTimetableResponse.class);

        List<ElviraTimetableResponse.TimetableEntry> entries = response.getTimetable();
        assertNotNull(entries);
        assertEquals(1, entries.size());

        ElviraTimetableResponse.TimetableEntry entry = entries.get(0);
        List<ElviraTimetableResponse.TrainInfo> segments = entry.getTrainSegments();
        assertEquals(1, segments.size());

        ElviraTimetableResponse.TrainInfo trainInfo = segments.get(0);
        assertEquals("url1", trainInfo.getUrl());
        assertEquals("getUrl1", trainInfo.getGetUrl());
        assertEquals("code1", trainInfo.getCode());
        assertEquals("vsz1", trainInfo.getVszCode());
    }

    @Test
    void deserialize_mixedEntries_onlyNonLocalTransportReturned() throws Exception {
        String json = """
                {
                  "timetable": [
                    {
                      "details": [
                        {
                          "train_info": {
                            "is_local_transport": true,
                            "url": "url1",
                            "get_url": "getUrl1",
                            "code": "code1",
                            "vsz_code": "vsz1"
                          }
                        }
                      ]
                    },
                    {
                      "details": [
                        {
                          "train_info": {
                            "is_local_transport": false,
                            "url": "url2",
                            "get_url": "getUrl2",
                            "code": "code2",
                            "vsz_code": "vsz2"
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        ObjectMapper mapper = createObjectMapperWithDeserializer();
        ElviraTimetableResponse response = mapper.readValue(json, ElviraTimetableResponse.class);

        List<ElviraTimetableResponse.TimetableEntry> entries = response.getTimetable();
        assertNotNull(entries);
        assertEquals(1, entries.size());

        ElviraTimetableResponse.TimetableEntry entry = entries.get(0);
        List<ElviraTimetableResponse.TrainInfo> segments = entry.getTrainSegments();
        assertEquals(1, segments.size());

        ElviraTimetableResponse.TrainInfo trainInfo = segments.get(0);
        assertEquals("url2", trainInfo.getUrl());
        assertEquals("getUrl2", trainInfo.getGetUrl());
        assertEquals("code2", trainInfo.getCode());
        assertEquals("vsz2", trainInfo.getVszCode());
    }
}
