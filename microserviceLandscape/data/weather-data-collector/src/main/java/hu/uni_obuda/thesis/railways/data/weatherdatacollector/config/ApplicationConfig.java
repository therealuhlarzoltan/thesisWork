package hu.uni_obuda.thesis.railways.data.weatherdatacollector.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class ApplicationConfig {

    @Value("${tcp.connection.timeout-in-ms:30000}")
    private int connectionTimeoutInMs;
    @Value("${tcp.connection.read.timeout-in-ms:0}")
    private int connectionReadTimeoutInMs;
    @Value("${tcp.connection.write.timeout-in-ms:0}")
    private int connectionWriteTimeoutInMs;
    @Value("${weather.api.base-url}")
    private String weatherApiUrl;
    @Value("${maps.api.base-url}")
    private String mapsApiUrl;

    @Bean(name = "weatherWebClient")
    public WebClient weatherWebClient(WebClient.Builder builder) {
        return builder.baseUrl(weatherApiUrl)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(connectionTimeoutInMs, connectionReadTimeoutInMs, connectionWriteTimeoutInMs)))
                .build();
    }

    @Bean(name = "mapsWebClient")
    public WebClient mapsWebClient(WebClient.Builder builder) {
        return builder.baseUrl(mapsApiUrl)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(connectionTimeoutInMs, connectionReadTimeoutInMs, connectionWriteTimeoutInMs)))
                .build();
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
