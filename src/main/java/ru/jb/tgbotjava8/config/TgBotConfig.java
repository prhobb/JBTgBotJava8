package ru.jb.tgbotjava8.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Configuration
@Data
@PropertySource("application.properties")
public class TgBotConfig {
    @Value("${tgbot.name}")
    String botName;
    @Value("${tgbot.token}")
    String token;
    @Value("${tgbot.otpfile}")
    String otpfile;

}
