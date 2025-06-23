package hu.uni_obuda.thesis.railways.cloud.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

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
    public SecurityWebFilterChain swaggerChain(ServerHttpSecurity http) {
        ServerWebExchangeMatcher swaggerPaths = exchange -> {
            String path = exchange.getRequest().getPath().value();
            if (path.startsWith("/route-planner/openapi/") || path.startsWith("/route-planner/webjars/")
                    || path.startsWith("/rail/openapi/") || path.startsWith("/rail/webjars/")
                    || path.startsWith("/weather/openapi/") || path.startsWith("/weather/webjars/")
                    || path.startsWith("/geocoding/openapi/") || path.startsWith("/geocoding/webjars/")
                    || path.startsWith("/delays/openapi/") || path.startsWith("/delays/webjars/")
                    || path.startsWith("/security/openapi/") || path.startsWith("/security/webjars/")) {
                return ServerWebExchangeMatcher.MatchResult.match();
            }
            return ServerWebExchangeMatcher.MatchResult.notMatch();
        };

        return http
                .securityMatcher(swaggerPaths)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex.anyExchange().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Order(3)
    @Bean
    public SecurityWebFilterChain permitRefreshChain(ServerHttpSecurity http) {
        ServerWebExchangeMatcher publicPathsMatcher = exchange -> {
            String path = exchange.getRequest().getPath().value();
            if (path.startsWith("/security/refresh")) {
                LOG.info("New chain matched");
                return ServerWebExchangeMatcher.MatchResult.match();
            }
            LOG.info("New chain not matched");
            return ServerWebExchangeMatcher.MatchResult.notMatch();
        };

        return http
                .securityMatcher(publicPathsMatcher)
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    @Order(4)
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/route-planner/**").hasAnyRole("ROLE_USER", "ROLE_ADMIN")
                        .pathMatchers("/rail/**").hasRole("ROLE_ADMIN")
                        .pathMatchers("/weather/**").hasRole("ROLE_ADMIN")
                        .pathMatchers("/geocoding/**").hasRole("ROLE_ADMIN")
                        .pathMatchers("/delays/**").hasRole("ROLE_ADMIN")
                        .pathMatchers( "/security/custom-logout").hasAnyRole("ROLE_USER", "ROLE_ADMIN")
                        .pathMatchers("/security/**").permitAll()

                        .anyExchange().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
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

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        return NimbusReactiveJwtDecoder
                .withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .build();
    }
}

