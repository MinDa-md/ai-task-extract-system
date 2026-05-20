package com.mindamd.taskextractor.global.config;

import com.mindamd.taskextractor.listener.DiscordCommandListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JdaConfig {

    @Value("${discord.token}")
    private String token;

    @Bean
    public JDA jda(DiscordCommandListener listener) throws InterruptedException {
        return JDABuilder.createLight(token,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(listener)
                .build()
                .awaitReady();
    }
}
