package com.powerpuff.backend.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.ekt")
public class EktProperties {
    private boolean enabled;
    @NotNull private URI baseUrl = URI.create("https://ekt.kz");
    private String username = "";
    private String password = "";
    @NotNull private Duration connectTimeout = Duration.ofSeconds(5);
    @NotNull private Duration readTimeout = Duration.ofSeconds(15);

    @AssertTrue(message = "EKT requires an HTTPS base URL without credentials, query or fragment")
    public boolean isSafeBaseUrl() {
        return baseUrl != null && "https".equalsIgnoreCase(baseUrl.getScheme())
            && baseUrl.getHost() != null && baseUrl.getUserInfo() == null
            && baseUrl.getQuery() == null && baseUrl.getFragment() == null;
    }
    @AssertTrue(message = "Configure EKT_USERNAME and EKT_PASSWORD when EKT_ENABLED=true")
    public boolean isCredentialsConfigured() {
        return !enabled || (username != null && !username.isBlank() && password != null && !password.isBlank());
    }
    @AssertTrue(message = "EKT timeouts must be positive and at most 2 minutes")
    public boolean isTimeoutsValid() { return valid(connectTimeout) && valid(readTimeout); }
    private boolean valid(Duration d) {
        return d != null && d.compareTo(Duration.ofMillis(1)) >= 0 && d.compareTo(Duration.ofMinutes(2)) <= 0;
    }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public URI getBaseUrl() { return baseUrl; }
    public void setBaseUrl(URI baseUrl) { this.baseUrl = baseUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
}
