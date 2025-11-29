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
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

@EnableAsync
@Configuration
public class ApplicationConfig {

    @Value("${tcp.connection.timeout-in-ms:30000}")
    private int connectionTimeoutInMs;
    @Value("${tcp.connection.read.timeout-in-ms:0}")
    private int connectionReadTimeoutInMs;
    @Value("${tcp.connection.write.timeout-in-ms:0}")
    private int connectionWriteTimeoutInMs;
    @Value("${railway.api.base-url}")
    private String railwayBaseUrl;
    @Value("${railway.api.time-table-getter-uri}")
    private String timetableGetterUri;
    @Value("${railway.api.train-details-getter-uri}")
    private String trainDetailsGetterUri;

    @Profile("data-source-elvira")
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.baseUrl(railwayBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(connectionTimeoutInMs, connectionReadTimeoutInMs, connectionWriteTimeoutInMs)))
                .build();
    }

    @Profile("data-source-emma")
    @Bean
    public HttpGraphQlClient shortTimetableClient(WebClient.Builder webClientBuilder, DocumentSource documentSource) {
        WebClient webClient = webClientBuilder.baseUrl(railwayBaseUrl + timetableGetterUri)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(connectionTimeoutInMs, connectionReadTimeoutInMs, connectionWriteTimeoutInMs)))
                .build();
        return HttpGraphQlClient.builder(webClient).documentSource(documentSource).build();
    }

    @Profile("data-source-emma")
    @Bean
    public HttpGraphQlClient shortTrainDetailsClient(WebClient.Builder webClientBuilder, DocumentSource documentSource) {
        WebClient webClient = webClientBuilder.baseUrl(railwayBaseUrl + trainDetailsGetterUri)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(connectionTimeoutInMs, connectionReadTimeoutInMs, connectionWriteTimeoutInMs)))
                .build();
        return HttpGraphQlClient.builder(webClient).documentSource(documentSource).build();
    }


    @Profile("data-source-emma")
    @Bean
    public HttpGraphQlClient timetableClient(WebClient.Builder webClientBuilder, DocumentSource documentSource) {
        WebClient webClient = webClientBuilder.baseUrl(railwayBaseUrl + timetableGetterUri)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(connectionTimeoutInMs, connectionReadTimeoutInMs, connectionWriteTimeoutInMs)))
                .build();
        return HttpGraphQlClient.builder(webClient).documentSource(documentSource).build();
    }


    @Profile("data-source-emma")
    @Bean
    public DocumentSource graphQlDocumentSource() {
        return new CachingDocumentSource(
                new ResourceDocumentSource(List.of(new ClassPathResource("graphql/emma/")), ResourceDocumentSource.FILE_EXTENSIONS)
        );
    }

    @Profile("data-source-emma")
    @Lazy
    @Bean
    public CachingYamlGraphQlVariableLoader cachingGraphQlVariableLoader(YamlGraphQlVariableLoader nonCachingVariableLoader) {
        return new CachingYamlGraphQlVariableLoader(nonCachingVariableLoader);
    }

    @Profile("data-source-emma")
    @Bean
    public YamlGraphQlVariableLoader graphQlVariableLoader() {
        return new YamlGraphQlVariableLoader("graphql/emma/default-variables");
    }

    @Profile("data-source-elvira")
    @Bean
    public ObjectMapper elviraObjectMapper() {
        SimpleModule timetableModule = new SimpleModule();
        timetableModule.addDeserializer(ElviraTimetableResponse.class, new TimetableResponseDeserializer());
        return new ObjectMapper()
                .registerModule(timetableModule)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // makes it ISO8601
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Profile("data-source-emma")
    @Bean
    public ObjectMapper emmaObjectMapper() {
        SimpleModule timetableModule = new SimpleModule();
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