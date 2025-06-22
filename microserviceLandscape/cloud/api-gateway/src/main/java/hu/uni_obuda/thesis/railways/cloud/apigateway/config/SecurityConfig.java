package hu.uni_obuda.thesis.railways.cloud.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Order(1)
    @Bean
    public SecurityWebFilterChain permitAllChain(ServerHttpSecurity http) {
        ServerWebExchangeMatcher publicPathsMatcher = exchange -> {
            String path = exchange.getRequest().getPath().value();
            if (path.startsWith("/eureka/") || path.startsWith("/config/")) {
                return ServerWebExchangeMatcher.MatchResult.match();
            }
            return ServerWebExchangeMatcher.MatchResult.notMatch();
        };

        return http
                .securityMatcher(publicPathsMatcher)
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    @Order(2)
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/route-planner/openapi/**", "/route-planner/webjars/**").authenticated()
                        .pathMatchers("/rail/openapi/**", "/rail/webjars/**").authenticated()
                        .pathMatchers("/weather/openapi/**", "/weather/webjars/**").authenticated()
                        .pathMatchers("/geocoding/openapi/**", "/geocoding/webjars/**").authenticated()
                        .pathMatchers("/delays/openapi/**", "/delays/webjars/**").authenticated()

                        .pathMatchers("/route-planner/**").permitAll()
                        .pathMatchers("/rail/**").permitAll()
                        .pathMatchers("/weather/**").permitAll()
                        .pathMatchers("/geocoding/**").permitAll()
                        .pathMatchers("/delays/**").permitAll()

                        .anyExchange().denyAll()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(@Value("${swagger.username}") String username,
                                                            @Value("${swagger.password}") String rawPassword,
                                                            BCryptPasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username(username)
                .password(rawPassword)
                .passwordEncoder(passwordEncoder::encode)
                .roles("SWAGGER_USER")
                .build();
        return new MapReactiveUserDetailsService(user);
    }
}

