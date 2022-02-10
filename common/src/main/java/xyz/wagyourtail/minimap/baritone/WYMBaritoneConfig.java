package xyz.wagyourtail.minimap.baritone;

import xyz.wagyourtail.config.field.IntRange;
import xyz.wagyourtail.config.field.Setting;
import xyz.wagyourtail.config.field.SettingsContainer;

@SettingsContainer("gui.wagyourminimap.baritone")
public class WYMBaritoneConfig {

    @Setting(value = "gui.wagyourminimap.settings.baritone.enablewaypoint")
    public boolean enableWaypoint = true;

    @Setting(value = "gui.wagyourminimap.settings.baritone.selection_min_y")
    @IntRange(from = -4096, to = 4096)
    public int minY = -64;

    @Setting(value = "gui.wagyourminimap.settings.baritone.selection_max_y")
    @IntRange(from = -4096, to = 4096)
    public int maxY = 320;
}
