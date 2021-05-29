/*
    This file is part of Structure Boxes.

    Structure Boxes is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Structure Boxes is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Structure Boxes.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.eirikh1996.structureboxes;

import br.net.fabiozumbi12.RedProtect.Sponge.API.RedProtectAPI;
import br.net.fabiozumbi12.RedProtect.Sponge.RedProtect;
import br.net.fabiozumbi12.RedProtect.Sponge.Region;
import com.arckenver.nations.NationsPlugin;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.intellectualcrafters.plot.IPlotMain;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.sk89q.worldedit.sponge.SpongeWorldEdit;
import com.universeguard.UniverseGuard;
import com.universeguard.region.GlobalRegion;
import com.universeguard.region.LocalRegion;
import com.universeguard.region.components.RegionMember;
import com.universeguard.region.enums.EnumRegionFlag;
import com.universeguard.region.enums.RegionRole;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.common.EagleFactionsPlugin;
import io.github.eirikh1996.structureboxes.command.*;
import io.github.eirikh1996.structureboxes.compat.we6.IWorldEditHandler;
import io.github.eirikh1996.structureboxes.listener.BlockListener;
import io.github.eirikh1996.structureboxes.listener.MovecraftListener;
import io.github.eirikh1996.structureboxes.localisation.I18nSupport;
import io.github.eirikh1996.structureboxes.settings.Settings;
import io.github.eirikh1996.structureboxes.utils.*;
import io.github.pulverizer.movecraft.Movecraft;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.bstats.sponge.Metrics2;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.ConfigManager;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameLoadCompleteEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static io.github.eirikh1996.structureboxes.utils.ChatUtils.COMMAND_PREFIX;


@Plugin(
        id = "structureboxes",
        name = "StructureBoxes",
        description = "A plugin that adds placable blocks that turn into pre-made structures",
        version = "3.0",
        authors = {"eirikh1996"},
        dependencies =
            {
                @Dependency(id = "worldedit"),
                @Dependency(id = "redprotect", optional = true),
                @Dependency(id = "griefprevention", optional = true),
                @Dependency(id = "plotsquared", optional = true),
                @Dependency(id = "eaglefactions", optional = true),
                @Dependency(id = "universeguard", optional = true),
                @Dependency(id = "nations-updated", optional = true),
                @Dependency(id = "movecraft", optional = true)
            }
        )
public class StructureBoxes implements SBMain {

    private static StructureBoxes instance;

    @Inject private Logger logger;
    @Inject private Game game;
    @Inject @DefaultConfig(sharedRoot = false) private Path defaultConfig;
    @Inject @ConfigDir(sharedRoot = false) private Path configDir;

    private ConfigurationLoader<CommentedConfigurationNode> loader;
    private String schematicDir;
    private Path weDir;

    public Path getConfigDir() {
        return configDir;
    }

    @NotNull private SpongeWorldEdit worldEditPlugin;
    @Inject private PluginManager pluginManager;
    @Inject private PluginContainer plugin;
    @Inject private ConfigManager configManager;
    private WorldEditHandler worldEditHandler;
    @NotNull private Optional<RedProtect> redProtectPlugin = Optional.empty();
    @NotNull private Optional<GriefPrevention> griefPreventionPlugin = Optional.empty();
    @NotNull private Optional<EagleFactionsPlugin> eagleFactionsPlugin = Optional.empty();
    @NotNull private Optional<IPlotMain> plotSquaredPlugin = Optional.empty();
    @NotNull private Optional<UniverseGuard> universeGuardPlugin = Optional.empty();
    @NotNull private Optional<NationsPlugin> nationsPlugin = Optional.empty();
    @NotNull private Optional<Movecraft> movecraftPlugin = Optional.empty();

    private boolean plotSquaredInstalled = false;
    @Inject private Metrics2 metrics;

    private ConsoleSource console;

    @Listener
    public void onGameLoaded(GameLoadCompleteEvent event) {
        instance = this;

        try {
            final Optional<Asset> config = plugin.getAsset("structureboxes.conf");
            assert config.isPresent();
            config.get().copyToFile(defaultConfig, false, true);
            readConfig();
            loadLocales();
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
        }
        I18nSupport.initialize(getConfigDir().toFile(), this);
        //Create command
        CommandSpec createCommand = CommandSpec.builder()
                .permission("structureboxes.create")
                .arguments(GenericArguments.string(Text.of()), GenericArguments.flags().flag("m").buildWith(GenericArguments.none()), GenericArguments.flags().flag("e").buildWith(GenericArguments.integer(Text.EMPTY)))
                .executor(new StructureBoxCreateCommand())
                .build();

        //undo command
        CommandSpec undoCommand = CommandSpec.builder()
                .executor(new StructureBoxUndoCommand())
                .permission("structureboxes.undo")
                .build();


        //reload command
        CommandSpec reloadCommand = CommandSpec.builder()
                .executor(new StructureBoxReloadCommand())
                .permission("structureboxes.reload")
                .build();

        //sessions command
        CommandSpec sessionsCommand = CommandSpec.builder()
                .executor(new StructureBoxSessionsCommand())
                .arguments(
                        GenericArguments.optional(GenericArguments.string(Text.of("player|-a"))),
                        GenericArguments.optional(GenericArguments.integer(Text.of("page"))))
                .build();

        CommandSpec structureBoxCommand = CommandSpec.builder()
                .executor(new StructureBoxCommand())
                .child(createCommand, "create", "cr", "c")
                .child(undoCommand, "undo", "u" , "ud")
                .child(reloadCommand, "reload", "r", "rl")
                .child(sessionsCommand, "sessions", "s")
                .build();
        Sponge.getCommandManager().register(plugin, structureBoxCommand, "structurebox", "sbox", "sb");
        console = Sponge.getServer().getConsole();

    }



    @SuppressWarnings("unchecked")
    @Listener
    public void onServerStarting(GameStartingServerEvent event) {

        worldEditPlugin = (SpongeWorldEdit) pluginManager.getPlugin("worldedit").get().getInstance().get();
        boolean regionProviderFound = false;
        //Check for RedProtect
        Optional<PluginContainer> redprotect = pluginManager.getPlugin("redprotect");
        if (redprotect.isPresent() && redprotect.get().getInstance().isPresent()){
            console.sendMessage(Text.of(I18nSupport.getInternationalisedString("Startup - RedProtect detected")));
            redProtectPlugin = (Optional<RedProtect>) redprotect.get().getInstance();
            regionProviderFound = true;
        }
        //Check for GriefPrevention
        Optional<PluginContainer> griefprevention = pluginManager.getPlugin("griefprevention");
        if (griefprevention.isPresent() && griefprevention.get().getInstance().isPresent()) {
            console.sendMessage(Text.of(COMMAND_PREFIX + I18nSupport.getInternationalisedString("Startup - GriefPrevention detected")));
            griefPreventionPlugin = (Optional<GriefPrevention>) griefprevention.get().getInstance();
            regionProviderFound = true;
        }
        //Check for EagleFactions
        Optional<PluginContainer> eagleFactions = pluginManager.getPlugin("eaglefactions");
        if (eagleFactions.isPresent() && eagleFactions.get().getInstance().isPresent()) {
            console.sendMessage(Text.of(COMMAND_PREFIX + I18nSupport.getInternationalisedString("Startup - EagleFactions detected")));
            eagleFactionsPlugin = (Optional<EagleFactionsPlugin>) eagleFactions.get().getInstance();
            regionProviderFound = true;
        }
        //Check for PlotSquared
        Optional<PluginContainer> plotsquared = pluginManager.getPlugin("plotsquared");
        if (plotsquared.isPresent() && plotsquared.get().getInstance().isPresent()) {
            console.sendMessage(Text.of(COMMAND_PREFIX + I18nSupport.getInternationalisedString("Startup - PlotSquared detected")));
            plotSquaredPlugin = (Optional<IPlotMain>) plotsquared.get().getInstance();
            PlotSquaredUtils.initialize();
            regionProviderFound = true;
        }
        //Check for UniverseGuard
        Optional<PluginContainer> universeGuard = pluginManager.getPlugin("universeguard");
        if (universeGuard.isPresent() && universeGuard.get().getInstance().isPresent()) {
            console.sendMessage(Text.of(COMMAND_PREFIX + I18nSupport.getInternationalisedString("Startup - UniverseGuard detected")));
            universeGuardPlugin = (Optional<UniverseGuard>) universeGuard.get().getInstance();
            regionProviderFound = true;
        }
        //Check for Movecraft
        Optional<PluginContainer> movecraft = pluginManager.getPlugin("movecraft");
        if (movecraft.isPresent() && movecraft.get().getInstance().isPresent()) {
            console.sendMessage(Text.of(COMMAND_PREFIX + I18nSupport.getInternationalisedString("Startup - Movecraft detected")));
            Sponge.getEventManager().registerListeners(this, new MovecraftListener());
            movecraftPlugin = (Optional<Movecraft>) movecraft.get().getInstance();
        }
        Optional<PluginContainer> nations = pluginManager.getPlugin("nations-updated");
        if (nations.isPresent() && nations.get().getInstance().isPresent()) {
            console.sendMessage(Text.of(COMMAND_PREFIX + I18nSupport.getInternationalisedString("Startup - Nations Updated detected")));
            nationsPlugin = (Optional<NationsPlugin>) nations.get().getInstance();
            regionProviderFound = true;
        }
        if ((Settings.RestrictToRegionsEnabled || Settings.RestrictToRegionsEntireStructure) && !regionProviderFound) {
            console.sendMessage(Text.of(COMMAND_PREFIX + I18nSupport.getInternationalisedString("Startup - Restrict to regions no compatible protection plugin")));
            Settings.RestrictToRegionsEnabled = false;
            Settings.RestrictToRegionsEntireStructure = false;
            console.sendMessage(Text.of(COMMAND_PREFIX + I18nSupport.getInternationalisedString("Startup - Restrict to regions set to false")));

        }
        //Now read WorldEdit config
        weDir = Paths.get(configDir.getParent().toString(), "worldedit");
        final Path weConfig = Paths.get(weDir.toString(), "worldedit.conf");
        final ConfigurationLoader<CommentedConfigurationNode> weLoader = HoconConfigurationLoader.builder().setPath(weConfig).build();
        try {
            schematicDir = weLoader.load().getNode("saving").getNode("dir").getString();
            worldEditHandler = new IWorldEditHandler(new File(weDir.toFile(), schematicDir), this);
        } catch (IOException e) {
            logger.error(I18nSupport.getInternationalisedString("Startup - Error reading WE config"));
            e.printStackTrace();
        }
        final boolean noRegionProvider = !regionProviderFound;
        metrics.addCustomChart(new Metrics2.AdvancedPie("region_providers", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            if (getEagleFactionsPlugin().isPresent()) {
                valueMap.put("EagleFactions", 1);
            }
            if (plotSquaredInstalled) {
                valueMap.put("PlotSquared", 1);
            }
            if (getRedProtectPlugin().isPresent()) {
                valueMap.put("RedProtect", 1);
            }
            if (getGriefPreventionPlugin().isPresent()) {
                valueMap.put("GriefPrevention", 1);
            }
            if (getNationsPlugin().isPresent()) {
                valueMap.put("Nations Updated", 1);
            }
            if (noRegionProvider) {
                valueMap.put("None", 1);
            }
            return valueMap;
        }));
        metrics.addCustomChart(new Metrics2.SimplePie("localisation", () -> Settings.locale));

        if (!Sponge.getMetricsConfigManager().areMetricsEnabled(this) && !Settings.Metrics) {
            metrics.cancel();
        }
        //Register listener
        Sponge.getEventManager().registerListeners(this, new BlockListener());


    }

    @Listener
    public void onServerStarted(GameStartedServerEvent event) {
        Task.builder().async().execute(UpdateManager.getInstance()).interval(1, TimeUnit.HOURS).submit(this);
    }

    public WorldEditHandler getWorldEditHandler() {
        return worldEditHandler;
    }

    @Override
    public boolean structureWithinRegion(UUID playerID, String schematicID, Collection<Location> locations) {
        final Player p = Sponge.getServer().getPlayer(playerID).get();
        if (!Settings.RestrictToRegionsEntireStructure || p.hasPermission("structureboxes.bypassregionrestriction")) {
            return true;
        }
        for (Location location : locations) {
            if (RegionUtils.isWithinRegion(MathUtils.sbToSpongeLoc(location))) {
                continue;
            }
            p.sendMessage(Text.of(COMMAND_PREFIX + I18nSupport.getInternationalisedString("Place - Structure must be in region")));
            return false;
        }
        return true;
    }

    public Platform getPlatform() {
        return Platform.SPONGE;
    }

    @Override
    public void clearStructure(Structure structure) {
        final Task.Builder taskBuilder = Task
                .builder()
                .execute(
                        new StructureBoxUndoCommand.StructureUndoTask(
                                structure.getLocationsToRemove(),
                                structure.getOriginalBlocks())
                );
        if (Settings.IncrementalPlacement) {
            taskBuilder.intervalTicks(Settings.IncrementalPlacementDelay);
        }
        taskBuilder.submit(this);
    }

    public boolean isFreeSpace(UUID playerID, String schematicName, Collection<Location> locations) {
        final HashMap<Location, Object> originalBlocks = new HashMap<>();
        Player p = Sponge.getServer().getPlayer(playerID).get();
        for (Location loc : locations){
            org.spongepowered.api.world.Location<World> spongeLoc = MathUtils.sbToSpongeLoc(loc);
            originalBlocks.put(loc, spongeLoc.getBlockType());

            if (redProtectPlugin.isPresent()) {
                RedProtectAPI api = redProtectPlugin.get().getAPI();
                Region region = api.getRegion(spongeLoc);
                if (region != null && !region.canBuild(p)) {
                    p.sendMessage(Text.of(COMMAND_PREFIX + String.format(I18nSupport.getInternationalisedString("Place - Forbidden Region"), "RedProtect")));
                    return false;
                }
            }
            if (griefPreventionPlugin.isPresent()) {
                final Claim claim = GriefPrevention.getApi().getClaimManager(p.getWorld()).getClaimAt(spongeLoc);
                if (claim != null && !claim.isTrusted(p.getUniqueId())) {
                    p.sendMessage(Text.of(COMMAND_PREFIX + String.format(I18nSupport.getInternationalisedString("Place - Forbidden Region"), "GriefPrevention")));
                    return false;
                }

            }
            if (plotSquaredPlugin.isPresent()) {
                final PS ps = PS.get();
                final com.intellectualcrafters.plot.object.Location psLoc = new com.intellectualcrafters.plot.object.Location(spongeLoc.getExtent().getName(), spongeLoc.getBlockX(), spongeLoc.getBlockY(), spongeLoc.getBlockZ());
                final PlotArea pArea = ps.getApplicablePlotArea(psLoc);
                if (pArea != null) {
                    Plot plot = pArea.getPlot(psLoc);
                    if (plot != null && !plot.isOwner(p.getUniqueId()) && !plot.isAdded(p.getUniqueId())) {
                        p.sendMessage(Text.of(COMMAND_PREFIX + String.format(I18nSupport.getInternationalisedString("Place - Forbidden Region"), "PlotSquared")));
                        return false;
                    }
                }
            }
            if (eagleFactionsPlugin.isPresent()) {
                final Optional<Faction> optionalFaction = eagleFactionsPlugin.get().getFactionLogic().getFactionByChunk(spongeLoc.getExtent().getUniqueId(), spongeLoc.getChunkPosition());
                if (optionalFaction.isPresent() && !optionalFaction.get().containsPlayer(p.getUniqueId())) {
                    p.sendMessage(Text.of(COMMAND_PREFIX + String.format(I18nSupport.getInternationalisedString("Place - Forbidden Region"), "EagleFactions")));
                    return false;
                }
            }
            if (universeGuardPlugin.isPresent()) {
                final com.universeguard.region.Region region = com.universeguard.utils.RegionUtils.getRegion(spongeLoc);
                boolean forbidden = false;
                if (region instanceof GlobalRegion) {
                    GlobalRegion globalRegion = (GlobalRegion) region;
                    if (!globalRegion.getFlag(EnumRegionFlag.PLACE)) {
                        forbidden = true;
                    }
                }
                if (region instanceof LocalRegion) {
                    LocalRegion localRegion = (LocalRegion) region;
                    RegionMember owner = new RegionMember(p, RegionRole.OWNER);
                    RegionMember member = new RegionMember(p, RegionRole.MEMBER);
                    if (!localRegion.getOwner().equals(owner) && !localRegion.getMembers().contains(member)) {
                        forbidden = true;
                    }
                }
                if (forbidden) {
                    p.sendMessage(Text.of(COMMAND_PREFIX + String.format(I18nSupport.getInternationalisedString("Place - Forbidden Region"), "UniverseGuard")));
                    return false;
                }
            }
            if (nationsPlugin.isPresent() && NationsUtils.allowedToBuild(p, spongeLoc)) {
                p.sendMessage(Text.of(COMMAND_PREFIX + String.format(I18nSupport.getInternationalisedString("Place - Forbidden Region"), "Nations")));
                return false;
            }
            if (!Settings.CheckFreeSpace){
                continue;
            }
            if (!spongeLoc.getBlockType().equals(BlockTypes.AIR) && !Settings.blocksToIgnore.contains(spongeLoc.getBlockType())){
                p.sendMessage(Text.of(COMMAND_PREFIX + I18nSupport.getInternationalisedString("Place - No free space")));
                return false;
            }

        }
        StructureManager.getInstance().addStructureByPlayer(playerID, schematicName, originalBlocks);
        return true;
    }

    public void sendMessageToPlayer(UUID recipient, String message) {
        Sponge.getServer().getPlayer(recipient).get().sendMessage(Text.of(message));
    }

    @Override
    public void logMessage(Level level, String message) {
        switch (level.toString()) {
            case "SEVERE":
                logger.error(message);
                break;
            case "WARNING":
                logger.warn(message);
                break;
            case "INFO":
                logger.info(message);
                break;
        }
    }

    public Logger getLogger() {
        return logger;
    }

    @NotNull
    public SpongeWorldEdit getWorldEditPlugin() {
        return worldEditPlugin;
    }

    @NotNull
    public Optional<GriefPrevention> getGriefPreventionPlugin() {
        return griefPreventionPlugin;
    }

    @NotNull
    public Optional<RedProtect> getRedProtectPlugin() {
        return redProtectPlugin;
    }

    @NotNull
    public Optional<EagleFactionsPlugin> getEagleFactionsPlugin() {
        return eagleFactionsPlugin;
    }

    @NotNull
    public Optional<IPlotMain> getPlotSquaredPlugin() {
        return plotSquaredPlugin;
    }

    @NotNull
    public Optional<UniverseGuard> getUniverseGuardPlugin() {
        return universeGuardPlugin;
    }

    public void clearInterior(Collection<Location> interior) {
        for (Location loc : interior){
            MathUtils.sbToSpongeLoc(loc).setBlockType(BlockTypes.AIR, BlockChangeFlags.NONE);
        }
    }

    @Override
    public void scheduleSyncTask(Runnable runnable) {
        Task.builder().execute(runnable).submit(this);
    }

    @Override
    public void scheduleSyncTaskLater(Runnable runnable, long delay) {
        Task.builder()
                .delay(delay, TimeUnit.MILLISECONDS)
                .execute(runnable)
                .submit(this);
    }

    @Override
    public void scheduleAsyncTask(Runnable runnable) {
        Task.builder().async().execute(runnable).submit(this);
    }

    @Override
    public void broadcast(String s) {
        Sponge.getServer().getBroadcastChannel().send(Text.of(s));
    }

    public static synchronized StructureBoxes getInstance() {
        return instance;
    }

    public void readConfig() throws ObjectMappingException, IOException {
        loader = HoconConfigurationLoader.builder().setPath(defaultConfig).build();
        final ConfigurationNode node = loader.load();
        //Read general config
        Settings.locale = node.getNode("Locale").getString("en");
        Settings.Metrics = node.getNode("Metrics").getBoolean(false);
        Settings.StructureBoxItem = node.getNode("Structure Box Item").getValue(TypeToken.of(BlockType.class), BlockTypes.CHEST);
        Settings.StructureBoxLore = node.getNode("Structure Box Display Name").getString("§6Structure Box");
        Settings.MaxStructureSize = node.getNode("Max Structure Size").getInt(10000);
        Settings.MaxSessionTime = node.getNode("Max Session Time").getInt(300);
        Settings.PlaceCooldownTime = node.getNode("Place Cooldown Time").getInt(30);
        Settings.StructureBoxPrefix = node.getNode("Structure Box Prefix").getString("§6Structure Box: ");
        Settings.AlternativePrefixes.addAll(node.getNode("Alternative Prefixes").getList(TypeToken.of(String.class), Collections.emptyList()));
        Settings.StructureBoxInstruction.addAll(node.getNode("Structure Box Instruction Message").getList(TypeToken.of(String.class), Collections.emptyList()));
        Settings.RequirePermissionPerStructureBox = node.getNode("Require permission per structure box").getBoolean(false);

        //Read restrict to regions section
        final ConfigurationNode restrictToRegionsNode = node.getNode("Restrict to regions");
        Settings.RestrictToRegionsEnabled = restrictToRegionsNode.getNode("Enabled").getBoolean(false);
        Settings.RestrictToRegionsEntireStructure = restrictToRegionsNode.getNode("Entire structure").getBoolean(false);
        Settings.RestrictToRegionsExceptions.addAll(restrictToRegionsNode.getNode("Exceptions").getList(TypeToken.of(String.class), Collections.emptyList()));

        //Read free space
        final ConfigurationNode freeSpaceNode = node.getNode("Free space");
        Settings.CheckFreeSpace = freeSpaceNode.getNode("Enabled").getBoolean(true);
        Settings.blocksToIgnore.addAll(freeSpaceNode.getNode("Blocks to ignore").getList(TypeToken.of(BlockType.class), Collections.emptyList()));

    }

    public void loadLocales() throws IOException {
        final String[] LOCALES = {"en", "no", "it"};
        for (String locale : LOCALES){
            if (new File(configDir.toString() + "/localisation/lang_" + locale + ".properties").exists()){
                continue;
            }

            final Optional<Asset> asset = plugin.getAsset("localisation/lang_" + locale + ".properties");
            asset.get().copyToDirectory(Paths.get(configDir.toString(), "localisation"), false, true);

        }


    }

    public PluginContainer getPlugin() {
        return plugin;
    }

    public ConsoleSource getConsole() {
        return console;
    }

    public String getSchematicDir() {
        return schematicDir;
    }

    public Path getWeDir() {
        return weDir;
    }

    @NotNull
    public Optional<Movecraft> getMovecraftPlugin() {
        return movecraftPlugin;
    }

    public Optional<NationsPlugin> getNationsPlugin() {
        return nationsPlugin;
    }


}
