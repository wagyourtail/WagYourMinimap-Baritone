package xyz.wagyourtail.minimap.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.cache.IWaypoint;
import baritone.api.cache.IWaypointCollection;
import baritone.api.event.events.PathEvent;
import baritone.api.event.events.WorldEvent;
import baritone.api.event.events.type.EventState;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import xyz.wagyourtail.minimap.api.MinimapApi;
import xyz.wagyourtail.minimap.api.MinimapEvents;
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
import xyz.wagyourtail.minimap.waypoint.filters.DimensionFilter;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
        MinimapClientEvents.WAYPOINT_LIST_MENU.register((s, b, nnb) -> {
            nnb.add(new Button(0, 0, 0, 20, new TranslatableComponent("gui.wagyourminimap.baritone.path_to"), (btn) -> {
                Waypoint p = s.getSelected().point;
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(p.posX, p.posY, p.posZ));
            }));
        });

        MinimapEvents.WAYPOINT_ADDED.register((w) -> {
            IWaypointCollection btWaypointManager = BaritoneAPI.getProvider()
                .getPrimaryBaritone()
                .getWorldProvider()
                .getCurrentWorld()
                .getWaypoints();
            Set<IWaypoint> btWaypoints = btWaypointManager.getByTag(IWaypoint.Tag.USER);

            BetterBlockPos pos = new BetterBlockPos(w.posX, w.posY, w.posZ);

            btWaypointManager.addWaypoint(new baritone.api.cache.Waypoint(
                w.name + "_WYM",
                IWaypoint.Tag.USER,
                pos
            ));
        });

        MinimapEvents.WAYPOINT_REMOVED.register((w) -> {
            IWaypointCollection btWaypointManager = BaritoneAPI.getProvider()
                .getPrimaryBaritone()
                .getWorldProvider()
                .getCurrentWorld()
                .getWaypoints();
            Set<IWaypoint> btWaypoints = btWaypointManager.getByTag(IWaypoint.Tag.USER);

            btWaypoints.stream().filter(p -> p.getName().equals(w.name + "_WYM")).collect(Collectors.toSet()).forEach(btWaypointManager::removeWaypoint);
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
            "default",
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
                    "default",
                    true,
                    true
                );
        }

        public Waypoint createBaritoneWaypoint(int x, int z) {
            MapServer.MapLevel ml = MinimapApi.getInstance().getMapServer().getCurrentLevel();
            return createBaritoneWaypoint(x, ChunkLocation.locationForChunkPos(ml, x << 4, z << 4).get().getData(SurfaceDataPart.class).map(e -> e.heightmap[SurfaceDataPart.blockPosToIndex(x, z)] + 2).orElse(64), z);
        }

        @Override
        public void onWorldEvent(WorldEvent event) {
            if (event.getState() == EventState.POST) {
                WaypointManager wymWaypointManager = MinimapClientApi.getInstance().getMapServer().waypoints;
                Set<Waypoint> wymWaypoints = wymWaypointManager.getAllWaypoints()
                    .stream()
                    .filter(new DimensionFilter())
                    .collect(
                        Collectors.toSet()
                    );
                IWaypointCollection btWaypointManager = BaritoneAPI.getProvider()
                    .getPrimaryBaritone()
                    .getWorldProvider()
                    .getCurrentWorld()
                    .getWaypoints();
                Set<IWaypoint> btWaypoints = btWaypointManager.getByTag(IWaypoint.Tag.USER);

                Map<BetterBlockPos, Waypoint> wymWaypointPos = new HashMap<>();

                for (Waypoint wymWaypoint : ImmutableSet.copyOf(wymWaypoints)) {
                    if (Arrays.asList(wymWaypoint.groups).contains("baritone")) {
                        wymWaypointManager.removeWaypoint(wymWaypoint);
                    } else {
                        wymWaypointPos.put(new BetterBlockPos(wymWaypoint.posX, wymWaypoint.posY, wymWaypoint.posZ), wymWaypoint);
                    }
                }

                for (IWaypoint btWaypoint : ImmutableSet.copyOf(btWaypoints)) {
                    if (btWaypoint.getName().endsWith("_WYM")) {
                        if (wymWaypointPos.containsKey(btWaypoint.getLocation())) {
                            if (!btWaypoint.getName().equals(wymWaypointPos.get(btWaypoint.getLocation()).name + "_WYM")) {
                                btWaypointManager.removeWaypoint(btWaypoint);
                                btWaypointManager.addWaypoint(new baritone.api.cache.Waypoint(
                                    wymWaypointPos.get(btWaypoint.getLocation()).name + "_WYM",
                                    IWaypoint.Tag.USER,
                                    btWaypoint.getLocation()
                                ));
                                wymWaypointPos.remove(btWaypoint.getLocation());
                            }
                        } else {
                            btWaypointManager.removeWaypoint(btWaypoint);
                        }
                    } else {
                        BetterBlockPos pos = btWaypoint.getLocation();
                        wymWaypointManager.addWaypoint(new Waypoint(
                            event.getWorld().dimensionType().coordinateScale(),
                            pos.x,
                            pos.y,
                            pos.z,
                            (byte) 0x00,
                            (byte) 0xFF,
                            (byte) 0x00,
                            "baritone:" + btWaypoint.getTag().getName() + " " + btWaypoint.getName(),
                            new String[] {"baritone"},
                            new String[] {MinimapApi.getInstance().getMapServer().getCurrentLevel().level_slug()},
                            new JsonObject(),
                            "default",
                            true,
                            true
                        ));
                    }
                }

                for (Map.Entry<BetterBlockPos, Waypoint> wymWaypoint : wymWaypointPos.entrySet()) {
                    btWaypointManager.addWaypoint(new baritone.api.cache.Waypoint(
                        wymWaypoint.getValue().name + "_WYM",
                        IWaypoint.Tag.USER,
                        wymWaypoint.getKey()
                    ));
                }
            }
        }

    }
}
