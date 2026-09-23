package com.powerpuff.backend.config;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class EktClientConfig {
    @Bean("ektRestClient")
    RestClient ektRestClient(EktProperties p) {
        var http = HttpClient.newBuilder().connectTimeout(p.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(p.getReadTimeout());
        var builder = RestClient.builder().baseUrl(p.getBaseUrl().toString()).requestFactory(factory);
        if (p.isEnabled()) builder.defaultHeaders(h -> h.setBasicAuth(p.getUsername(), p.getPassword()));
        return builder.build();
    }
}
