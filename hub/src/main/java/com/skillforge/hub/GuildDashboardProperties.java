package com.skillforge.hub;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Controls which heroes appear on the hub dashboard.
 * Bound from the guild.dashboard.* namespace in application.yml.
 */
@Component
@ConfigurationProperties("guild.dashboard")
public class GuildDashboardProperties {

    /**
     * When false, heroes whose githubLogin equals their heroId (self-registered
     * internal agents) are hidden from the Heróis card on the dashboard.
     * Env override: SHOW_GENERIC_HEROES=false
     */
    private boolean showGenericHeroes = true;

    public boolean isShowGenericHeroes() { return showGenericHeroes; }
    public void setShowGenericHeroes(boolean showGenericHeroes) {
        this.showGenericHeroes = showGenericHeroes;
    }
}
