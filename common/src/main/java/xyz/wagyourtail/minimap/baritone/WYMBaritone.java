package xyz.wagyourtail.minimap.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.event.events.PathEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.pathing.goals.GoalXZ;
import com.google.common.base.Suppliers;
import com.google.gson.JsonObject;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import xyz.wagyourtail.minimap.api.MinimapApi;
import xyz.wagyourtail.minimap.api.client.MinimapClientApi;
import xyz.wagyourtail.minimap.api.client.MinimapClientEvents;
import xyz.wagyourtail.minimap.api.client.config.MinimapClientConfig;
import xyz.wagyourtail.minimap.baritone.config.BaritoneOverlaySettings;
import xyz.wagyourtail.minimap.chunkdata.ChunkLocation;
import xyz.wagyourtail.minimap.chunkdata.parts.SurfaceDataPart;
import xyz.wagyourtail.minimap.client.gui.screen.widget.InteractMenuButton;
import xyz.wagyourtail.minimap.map.MapServer;
import xyz.wagyourtail.minimap.waypoint.Waypoint;
import xyz.wagyourtail.minimap.waypoint.WaypointManager;

import java.util.List;
import java.util.function.Supplier;

public class WYMBaritone {
    public static final String MOD_ID = "wagyourminimap-baritone";
    public static final Minecraft mc = Minecraft.getInstance();
    private static boolean first = true;
    
    public static void init() {
        // make sure we get the right api initialized
        MinimapClientApi.getInstance();

        // register config
        MinimapApi.getInstance().getConfig().registerConfig("baritone", WYMBaritoneConfig.class);

        // register minimap renderer
        MinimapClientEvents.AVAILABLE_MINIMAP_OPTIONS.register((clz, layer, overlay) -> {
            overlay.put(BaritoneMinimapOverlay.class, BaritoneOverlaySettings.class);
        });

        // register fullscreen renderer
        //TODO: implement fullscreen renderer

        // defered register baritone event listener
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register((l) -> {
            if (first) {
                BaritoneAPI.getProvider()
                    .getPrimaryBaritone()
                    .getGameEventHandler()
                    .registerEventListener(new WYMBaritoneEventListener());
                first = false;
            }
        });

        // register goto managers
        MinimapClientEvents.FULLSCREEN_INTERACT_MENU.register((menu) -> {
            menu.buttons.put(I18n.get("gui.wagyourminimap.baritone") + ": ", List.of(new InteractMenuButton(new TranslatableComponent("gui.wagyourminimap.baritone.path_to"), (btn) -> {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ((int)menu.x, (int)menu.z));
            })));

            for (Waypoint p : menu.waypoints) {
                menu.buttons.get(I18n.get("gui.wagyourminimap.waypoint") + ": " + p.name).add(new InteractMenuButton(new TranslatableComponent("gui.wagyourminimap.baritone.path_to"), (btn) -> {
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(p.posX, p.posY, p.posZ));
                }));
            }
        });
    }

    private static class WYMBaritoneEventListener implements AbstractGameEventListener {
        private Waypoint baritoneGoalWaypoint = new Waypoint(
            1,
            0,
            0,
            0,
            (byte) 0x00,
            (byte) 0xFF,
            (byte) 0x00,
            "Baritone Goal",
            new String[] {"baritone"},
            new String[] {"minecraft/overworld"},
            new JsonObject(),
            true,
            true
        );

        @Override
        public void onPathEvent(PathEvent event) {
            WaypointManager waypoints =MinimapClientApi.getInstance().getMapServer().waypoints;
            if (event == PathEvent.CANCELED || event == PathEvent.AT_GOAL) {
                if (baritoneGoalWaypoint != null) {
                    waypoints.removeWaypoint(baritoneGoalWaypoint);
                    baritoneGoalWaypoint = null;
                }
            } else {


                // update for baritone
                if (MinimapApi.getInstance().getConfig().get(WYMBaritoneConfig.class).enableWaypoint) {
                    Goal g = BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().getGoal();
                    if (g != null) {
                        if (g instanceof GoalXZ gx) {
                            baritoneGoalWaypoint = createBaritoneWaypoint(gx.getX(), gx.getZ());
                            waypoints.addWaypoint(baritoneGoalWaypoint);
                        } else if (g instanceof GoalBlock gb) {
                            baritoneGoalWaypoint = createBaritoneWaypoint(gb.x, gb.y, gb.z);
                            waypoints.addWaypoint(baritoneGoalWaypoint);
                        } else if (g instanceof GoalGetToBlock ggtb) {
                            baritoneGoalWaypoint = createBaritoneWaypoint(ggtb.x, ggtb.y, ggtb.z);
                            waypoints.addWaypoint(baritoneGoalWaypoint);
                        }
                        // otherwise unsupported goal type
                    }
                } else {
                    if (baritoneGoalWaypoint != null) {
                        waypoints.removeWaypoint(baritoneGoalWaypoint);
                    }
                }

            }
        }

        public Waypoint createBaritoneWaypoint(int x, int y, int z) {
            Level level = mc.level;
            if (level == null) {
                return null;
            }
            return new Waypoint(
                    level.dimensionType().coordinateScale(),
                    x,
                    y,
                    z,
                    (byte) 0x00,
                    (byte) 0xFF,
                    (byte) 0x00,
                    "Baritone Goal",
                    new String[] {"baritone"},
                    new String[] {MinimapApi.getInstance().getMapServer().getCurrentLevel().level_slug()},
                    new JsonObject(),
                    true,
                    true
                );
        }

        public Waypoint createBaritoneWaypoint(int x, int z) {
            Level level = mc.level;
            if (level == null) {
                return null;
            }
            MapServer.MapLevel ml = MinimapApi.getInstance().getMapServer().getCurrentLevel();
            return new Waypoint(
                level.dimensionType().coordinateScale(),
                x,
                ChunkLocation.locationForChunkPos(ml, x << 4, z << 4).get().getData(SurfaceDataPart.class).map(e -> e.heightmap[SurfaceDataPart.blockPosToIndex(x, z)] + 2).orElse(64),
                z,
                (byte) 0x00,
                (byte) 0xFF,
                (byte) 0x00,
                "Baritone Goal",
                new String[] {"baritone"},
                new String[] {ml.level_slug()},
                new JsonObject(),
                true,
                true
            );
        }

    }

}
