package hu.uni_obuda.thesis.railways.cloud.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import reactor.core.publisher.Mono;

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

                        .pathMatchers("/route-planner/**").hasAnyRole("ROLE_USER", "ROLE_ADMIN")
                        .pathMatchers("/rail/**").hasRole("ROLE_ADMIN")
                        .pathMatchers("/weather/**").hasRole("ROLE_ADMIN")
                        .pathMatchers("/geocoding/**").hasRole("ROLE_ADMIN")
                        .pathMatchers("/delays/**").hasRole("ROLE_ADMIN")

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

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter delegate = new JwtGrantedAuthoritiesConverter();
        delegate.setAuthorityPrefix("ROLE_");
        delegate.setAuthoritiesClaimName("roles");

        ReactiveJwtAuthenticationConverterAdapter adapter =
                new ReactiveJwtAuthenticationConverterAdapter(jwt -> {
                    var authorities = delegate.convert(jwt);
                    return new UsernamePasswordAuthenticationToken(
                            jwt.getSubject(), jwt, authorities);
                });

        return adapter;
    }
}

