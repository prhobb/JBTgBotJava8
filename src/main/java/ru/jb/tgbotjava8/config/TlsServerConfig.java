package ru.jb.tgbotjava8.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Configuration
@Data
@PropertySource("application.properties")
@ConfigurationProperties(prefix = "tlsserver")
public class TlsServerConfig {
    int port;
    String tlsVersion;
    String keyStore;
    char[] keyStorePassword;
    String trustStore;
    char[] trustStorePassword;
    final int backlog = 1;
}
