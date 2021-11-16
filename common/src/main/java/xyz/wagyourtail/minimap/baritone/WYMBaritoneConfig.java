package xyz.wagyourtail.minimap.baritone;

import xyz.wagyourtail.config.field.Setting;
import xyz.wagyourtail.config.field.SettingsContainer;

@SettingsContainer("gui.wagyourminimap.baritone")
public class WYMBaritoneConfig {

    @Setting(value = "gui.wagyourminimap.settings.baritone.enablewaypoint")
    public boolean enableWaypoint = true;
}
