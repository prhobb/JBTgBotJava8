package ru.jb.tgbotjava8.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;


@Configuration
@Data
@PropertySource("application.properties")
public class TgBotConfig {
    public final static String TOKEN_ARG = "-token";
    @Value("${tgbot.name}")
    String botName;
    //@Value("${tgbot.token}")
    String token;
    @Value("${tgbot.otpfile}")
    String otpfile;
    @Autowired
    public TgBotConfig(ApplicationArguments args, Environment env){

        token=env.getProperty("tgbot.token");

        List<String> runargs = Arrays.asList(args.getSourceArgs());
        if(runargs.contains(TOKEN_ARG)) {
            int tokenIndex = runargs.indexOf(TOKEN_ARG) +1;
            if(runargs.size()>tokenIndex)
                token = runargs.get(tokenIndex);
        }

    }

}
