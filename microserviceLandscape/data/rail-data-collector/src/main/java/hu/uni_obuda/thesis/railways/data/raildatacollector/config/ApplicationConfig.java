package hu.uni_obuda.thesis.railways.data.raildatacollector.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.resource.CachingYamlGraphQlVariableLoader;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.resource.YamlGraphQlVariableLoader;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.serializer.TimetableResponseDeserializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.graphql.support.CachingDocumentSource;
import org.springframework.graphql.support.DocumentSource;
import org.springframework.graphql.support.ResourceDocumentSource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.List;
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
    @Profile("data-source-elvira")
    public WebClient webClient(WebClient.Builder builder) {
        return builder.baseUrl(railwayApiUrl)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(connectionTimeoutInMs, connectionReadTimeoutInMs, connectionWriteTimeoutInMs)))
                .build();
    }

    @Bean
    @Profile("data-source-emma")
    public HttpGraphQlClient graphQlClient(WebClient.Builder webClientBuilder, DocumentSource documentSource) {
        WebClient webClient = webClientBuilder.baseUrl(railwayApiUrl)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(connectionTimeoutInMs, connectionReadTimeoutInMs, connectionWriteTimeoutInMs)))
                .build();
        return HttpGraphQlClient.builder(webClient).documentSource(documentSource).build();
    }

    @Bean
    @Profile("data-source-emma")
    public DocumentSource graphQlDocumentSource() {
        return new CachingDocumentSource(
                new ResourceDocumentSource(List.of(new ClassPathResource("graphql/emma/")), ResourceDocumentSource.FILE_EXTENSIONS)
        );
    }

    @Lazy
    @Bean
    @Profile("data-source-emma")
    public CachingYamlGraphQlVariableLoader cachingGraphQlVariableLoader(YamlGraphQlVariableLoader nonCachingVariableLoader) {
        return new CachingYamlGraphQlVariableLoader(nonCachingVariableLoader);
    }

    @Bean
    @Profile("data-source-emma")
    public YamlGraphQlVariableLoader graphQlVariableLoader() {
        return new YamlGraphQlVariableLoader("graphql/emma/default-variables");
    }

    @Bean
    public ObjectMapper objectMapper() {
        SimpleModule timetableModule = new SimpleModule();
        timetableModule.addDeserializer(ElviraTimetableResponse.class, new TimetableResponseDeserializer());
        return new ObjectMapper()
                .registerModule(timetableModule)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // makes it ISO8601
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
