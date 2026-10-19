package de.sauberzauber.backend.push;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PushProperties.class)
public class PushConfig {

}
