package hu.uni_obuda.thesis.railways.data.raildatacollector.config;

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

    private HttpClient createHttpClient(int connectionTimeoutInMs, int connectionReadTimeoutInMs, int connectionWriteTimeoutInMs) {
        HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutInMs);
        if (connectionReadTimeoutInMs > 0 && connectionWriteTimeoutInMs > 0) {
            httpClient.doOnConnected(conn -> conn
                    .addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.MICROSECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.MICROSECONDS))
            );
        } else if (connectionReadTimeoutInMs > 0) {
            httpClient.doOnConnected(conn -> conn
                    .addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.MICROSECONDS)));
        } else if (connectionWriteTimeoutInMs > 0) {
            httpClient.doOnConnected(conn -> conn
                    .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.MICROSECONDS))
            );
        }

        return httpClient;
    }
}
