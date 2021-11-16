package xyz.wagyourtail.minimap.baritone.fabric;

import xyz.wagyourtail.minimap.baritone.WYMBaritone;
import net.fabricmc.api.ModInitializer;

public class WYMBaritoneFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        WYMBaritone.init();
    }
}
