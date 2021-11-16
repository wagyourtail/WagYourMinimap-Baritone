package xyz.wagyourtail.minimap.baritone.config;

import xyz.wagyourtail.config.field.SettingsContainer;
import xyz.wagyourtail.minimap.api.client.config.overlay.AbstractOverlaySettings;
import xyz.wagyourtail.minimap.baritone.BaritoneMinimapOverlay;
import xyz.wagyourtail.minimap.client.gui.hud.map.AbstractMinimapRenderer;

@SettingsContainer("gui.wagyourminimap.baritone")
public class BaritoneOverlaySettings extends AbstractOverlaySettings<BaritoneMinimapOverlay> {
    @Override
    public BaritoneMinimapOverlay compileOverlay(AbstractMinimapRenderer mapRenderer) {
        return new BaritoneMinimapOverlay(mapRenderer);
    }

}
