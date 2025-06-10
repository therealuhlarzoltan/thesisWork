package hu.uni_obuda.thesis.railways.data.raildatacollector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.TimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.serializer.TimetableResponseDeserializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class ApplicationConfig {

    @Value("${tcp.connection.timeout-in-ms:30000}")
    private int connectionTimeoutInMs;
    @Value("${tcp.connection.read.timeout-in-ms:0}")
    private int connectionReadTimeoutInMs;
    @Value("${tcp.connection.write.timeout-in-ms:0}")
    private int connectionWriteTimeoutInMs;
    @Value("${railway.api.base-url}")
    private String railwayApiUrl;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.baseUrl(railwayApiUrl)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(connectionTimeoutInMs, connectionReadTimeoutInMs, connectionWriteTimeoutInMs)))
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule timetableModule = new SimpleModule();
        timetableModule.addDeserializer(TimetableResponse.class, new TimetableResponseDeserializer());
        mapper.registerModule(timetableModule);
        return mapper;
    }

    private HttpClient createHttpClient(int connectionTimeoutInMs, int connectionReadTimeoutInMs, int connectionWriteTimeoutInMs) {
        HttpClient httpClient = HttpClient.create();

        if (connectionTimeoutInMs > 0) {
            httpClient = httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutInMs);
        }

        return httpClient.doOnConnected(conn -> {
            if (connectionReadTimeoutInMs > 0) {
                conn.addHandlerLast(new ReadTimeoutHandler(connectionReadTimeoutInMs, TimeUnit.MILLISECONDS));
            }
            if (connectionWriteTimeoutInMs > 0) {
                conn.addHandlerLast(new WriteTimeoutHandler(connectionWriteTimeoutInMs, TimeUnit.MILLISECONDS));
            }
        });
    }
}
