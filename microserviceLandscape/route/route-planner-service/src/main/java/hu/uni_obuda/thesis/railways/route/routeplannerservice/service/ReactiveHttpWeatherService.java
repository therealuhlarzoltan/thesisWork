package hu.uni_obuda.thesis.railways.route.routeplannerservice.service;

import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.WeatherDataGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Primary
@Service
public class ReactiveHttpWeatherService implements WeatherService {

    private final WeatherDataGateway gateway;

    @Autowired
    public ReactiveHttpWeatherService(@Qualifier("reactiveWeatherDataGateway") WeatherDataGateway gateway) {
        this.gateway = gateway;
    }
}
