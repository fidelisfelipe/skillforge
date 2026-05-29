package com.skillforge.hub.onboarding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HubUrlProvider {

    private volatile String publicUrl;

    public HubUrlProvider(@Value("${guild.hub.public-url:http://localhost:8080}") String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String get() {
        return publicUrl;
    }

    void update(String url) {
        this.publicUrl = url;
    }
}
