package com.gobro_backend.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Payment configuration.
 *
 * The platform supports three local Tunisian payment providers (D17, Konnect,
 * Flouci) plus classic card payments, an internal wallet and coupons/promo codes.
 * Each provider gets its own pre-configured RestClient with its base URL and
 * timeouts; API secrets are injected from application-*.yml / environment
 * variables and consumed by D17PaymentService / KonnectPaymentService / FlouciPaymentService.
 */
@Configuration
public class PaymentConfig {

    @Value("${app.payment.timeout-connect-ms:5000}")
    private int connectTimeoutMs;

    @Value("${app.payment.timeout-read-ms:15000}")
    private int readTimeoutMs;

    // ---- D17 ----
    @Value("${app.payment.d17.base-url}")
    private String d17BaseUrl;

    @Value("${app.payment.d17.api-key}")
    private String d17ApiKey;

    // ---- Konnect ----
    @Value("${app.payment.konnect.base-url}")
    private String konnectBaseUrl;

    @Value("${app.payment.konnect.api-key}")
    private String konnectApiKey;

    @Value("${app.payment.konnect.wallet-id}")
    private String konnectWalletId;

    // ---- Flouci ----
    @Value("${app.payment.flouci.base-url}")
    private String flouciBaseUrl;

    @Value("${app.payment.flouci.app-token}")
    private String flouciAppToken;

    @Value("${app.payment.flouci.app-secret}")
    private String flouciAppSecret;

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return factory;
    }

    @Bean(name = "d17RestClient")
    public RestClient d17RestClient() {
        return RestClient.builder()
                .baseUrl(d17BaseUrl)
                .requestFactory(requestFactory())
                .defaultHeader("Authorization", "Bearer " + d17ApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean(name = "konnectRestClient")
    public RestClient konnectRestClient() {
        return RestClient.builder()
                .baseUrl(konnectBaseUrl)
                .requestFactory(requestFactory())
                .defaultHeader("x-api-key", konnectApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean(name = "flouciRestClient")
    public RestClient flouciRestClient() {
        return RestClient.builder()
                .baseUrl(flouciBaseUrl)
                .requestFactory(requestFactory())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String getKonnectWalletId() {
        return konnectWalletId;
    }

    public String getFlouciAppToken() {
        return flouciAppToken;
    }

    public String getFlouciAppSecret() {
        return flouciAppSecret;
    }
}