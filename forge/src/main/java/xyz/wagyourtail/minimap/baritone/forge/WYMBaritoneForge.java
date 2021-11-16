package xyz.wagyourtail.minimap.baritone.forge;

import dev.architectury.platform.forge.EventBuses;
import xyz.wagyourtail.minimap.baritone.WYMBaritone;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(WYMBaritone.MOD_ID)
public class WYMBaritoneForge {
    public WYMBaritoneForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(WYMBaritone.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        WYMBaritone.init();
    }
}
