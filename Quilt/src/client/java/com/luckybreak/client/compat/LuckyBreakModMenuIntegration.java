package com.luckybreak.client.compat;

import com.luckybreak.client.screen.LuckyChanceConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class LuckyBreakModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return LuckyChanceConfigScreen::new;
    }
}
