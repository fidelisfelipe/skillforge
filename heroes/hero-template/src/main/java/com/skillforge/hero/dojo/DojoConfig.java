package com.skillforge.hero.dojo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Configuration
public class DojoConfig {

    @Bean
    public SseEmitter sseEmitter() {
        return new SseEmitter(-1L);
    }
}
