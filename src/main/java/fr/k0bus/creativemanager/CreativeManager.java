package fr.k0bus.creativemanager;

import fr.k0bus.creativemanager.commands.SubCommands;
import fr.k0bus.creativemanager.commands.cm.CreativeManagerCommands;
import fr.k0bus.creativemanager.commands.cm.CreativeManagerCommandTab;
import fr.k0bus.creativemanager.event.*;
import fr.k0bus.creativemanager.event.plugin.ChestShop;
import fr.k0bus.creativemanager.event.plugin.ItemsAdderListener;
import fr.k0bus.creativemanager.event.plugin.SlimeFun;
import fr.k0bus.creativemanager.log.DataManager;
import fr.k0bus.creativemanager.settings.Settings;
import fr.k0bus.creativemanager.task.SaveTask;
import fr.k0bus.k0buscore.K0busCore;
import fr.k0bus.k0buscore.config.Lang;
import fr.k0bus.k0buscore.utils.StringUtils;
import me.angeschossen.lands.api.integration.LandsIntegration;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.md_5.bungee.api.ChatColor;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;

public class CreativeManager extends K0busCore {

    public static String TAG = StringUtils.translateColor("&r[&cCreativeManager&r] ");
    private final String invTag = ChatColor.BOLD + "" + ChatColor.DARK_RED + "CM " + ChatColor.RESET + "> ";
    private static Settings settings;
    private static Lang lang;
    private DataManager dataManager;
    private int saveTask;
    private LandsIntegration landsIntegration;
    private GriefPrevention griefPrevention;

    @Override
    public void onEnable() {
        super.onEnable();
        getLog().log("&9=============================================================");
        checkUpdate(75097);
        new Metrics(this, 11481);
        getLog().log("&9=============================================================");
        getLog().log("&2Created by K0bus for AkuraGaming");
        getLog().log("&9=============================================================");
        getLog().log("&2Check config file for update");
        this.updateConfig();
        getLog().log("&2Loading config file");
        this.loadConfigManager();
        this.registerEvent(this.getServer().getPluginManager());
        getLog().log("&2Listener registered");
        this.registerCommand();
        getLog().log("&2Commands registered");
        this.registerPermissions();
        this.loadLog();
        this.saveTask = SaveTask.run(this);
        if(getSettings().getBoolean("stop-inventory-save"))
            getLog().log("&cWarning : &4'stop-inventory-save' set on 'true' then all features about inventory as been disabled !");
        getLog().log("&9=============================================================");
    }

    public void registerProtectionsAPI()
    {
        if(getServer().getPluginManager().isPluginEnabled("Lands"))
            landsIntegration = new LandsIntegration(this);
        if(getServer().getPluginManager().isPluginEnabled("GriefPrevention"))
            griefPrevention = GriefPrevention.instance;
    }

    public void loadConfigManager() {
        settings = new Settings(this);
        getLog().log("&2Configuration loaded");
        lang = new Lang(settings.getLang(), this);
        getLog().log("&2Language loaded &7[" + settings.getLang() + "]");
        TAG = settings.getTag();
        antiSpamTick = settings.getInt("antispam-tick");
    }

    public void updateConfig() {
        Settings.updateConfig("lang/en_EN.yml", this);
        Settings.updateConfig("lang/es_ES.yml", this);
        Settings.updateConfig("lang/fr_FR.yml", this);
        Settings.updateConfig("lang/it_IT.yml", this);
        Settings.updateConfig("lang/ru_RU.yml", this);
        Settings.updateConfig("config.yml", this);
    }

    private void registerEvent(PluginManager pm) {
        pm.registerEvents(new PlayerBuild(this), this);
        pm.registerEvents(new PlayerBreak(this), this);
        pm.registerEvents(new PlayerInteract(), this);
        pm.registerEvents(new PlayerInteractEntity(), this);
        pm.registerEvents(new PlayerInteractAtEntity(), this);
        pm.registerEvents(new PlayerDrop(), this);
        pm.registerEvents(new PlayerGamemodeChange(this), this);
        pm.registerEvents(new PlayerQuit(this), this);
        pm.registerEvents(new PlayerLogin(this), this);
        pm.registerEvents(new PistonEvent(this), this);
        pm.registerEvents(new MonsterSpawnEvent(this), this);
        pm.registerEvents(new ProjectileThrow(), this);
        pm.registerEvents(new InventoryOpen(), this);
        pm.registerEvents(new PlayerPreCommand(this), this);
        pm.registerEvents(new ExplodeEvent(this), this);
        pm.registerEvents(new PlayerDeath(), this);
        pm.registerEvents(new CreativeCopy(this), this);
        /*  Add event checked for old version */
        try {
            ItemMeta.class.getMethod("getPersistentDataContainer", (Class<?>[]) null);
            pm.registerEvents(new InventoryMove(), this);
        } catch (NoSuchMethodException | SecurityException e) {
            getLog().log("NBT Protection disabled on your Minecraft version");
            pm.registerEvents(new InventoryMove(false), this);
        }
        try {
            ProjectileHitEvent.class.getMethod("getHitEntity", (Class<?>[]) null);
            ProjectileHitEvent.class.getMethod("setCancelled", (Class<?>[]) null);
            pm.registerEvents(new PlayerHitEvent(true), this);
        } catch (NoSuchMethodException | SecurityException e) {
            getLog().log("PvP / PvE Protection can't protect from projectile on this Spigot version !");
            pm.registerEvents(new PlayerHitEvent(false), this);
        }
        try {
            Class.forName( "org.bukkit.event.entity.EntityPickupItemEvent" );
            pm.registerEvents(new PlayerPickup(), this);
        } catch( ClassNotFoundException e ) {
            getLog().log("Player pickup protection not enabled on this Spigot version !");
        }
        /* Add plugin event */
        if(getServer().getPluginManager().isPluginEnabled("Slimefun"))
            pm.registerEvents(new SlimeFun(this), this);
        if(getServer().getPluginManager().isPluginEnabled("ChestShop"))
            pm.registerEvents(new ChestShop(this), this);
        if(getServer().getPluginManager().isPluginEnabled("ItemsAdder"))
            pm.registerEvents(new ItemsAdderListener(), this);
    }

    private void registerCommand() {
        PluginCommand mainCommand = this.getCommand("cm");
        if (mainCommand != null) {
            mainCommand.setExecutor(new CreativeManagerCommands(this));
            mainCommand.setTabCompleter(new CreativeManagerCommandTab((SubCommands) mainCommand.getExecutor()));
        }
    }

    private void registerPermissions() {
        PluginManager pm = getServer().getPluginManager();
        int n = 0;
        for (EntityType entityType : EntityType.values()) {
            registerPerm("creativemanager.bypass.entity." + entityType.name(), pm);
            n++;
        }
        registerPerm("creativemanager.bypass.deathdrop", pm);
        getLog().log("&2Entities permissions registered ! &7[" + n + "]");

        /* Add plugin permissions */
        if(getServer().getPluginManager().isPluginEnabled("ChestShop")) {
            registerPerm("creativemanager.bypass.chestshop", pm);
            getLog().log("&2ChestShop permissions registered !");
        }
        if(getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
            registerPerm("creativemanager.bypass.itemsadder.furnituresplace", pm);
            registerPerm("creativemanager.bypass.itemsadder.blockplace", pm);
            registerPerm("creativemanager.bypass.itemsadder.blockbreak", pm);
            registerPerm("creativemanager.bypass.itemsadder.blockinteract", pm);
            registerPerm("creativemanager.bypass.itemsadder.furnituresinteract", pm);
            registerPerm("creativemanager.bypass.itemsadder.killentity", pm);
            getLog().log("&2ItemsAdder permissions registered !");
        }
        if(getServer().getPluginManager().isPluginEnabled("Slimefun")) {
            registerPerm("creativemanager.bypass.slimefun", pm);
            getLog().log("&2Slimefun permissions registered !");
        }
    }

    private void registerPerm(String permission, PluginManager pm)
    {
        if (!pm.getPermissions().contains(new Permission(permission))) {
            try {
                pm.addPermission(new Permission(permission));
            }catch (IllegalArgumentException ignored) {}
        }
    }

    private void loadLog() {
        dataManager = new DataManager("data", this);
        getLog().log("&2Log loaded from database ! &7[" + dataManager.getBlockLogHashMap().size() + "]");
    }

    public static Settings getSettings() {
        return settings;
    }

    public static Lang getLang() {
        return lang;
    }

    public String getInvTag() {
        return invTag;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public LandsIntegration getLandsIntegration() {
        return landsIntegration;
    }

    public GriefPrevention getGriefPrevention() {
        return griefPrevention;
    }



    @Override
    public void onDisable() {
        if (Bukkit.getScheduler().isCurrentlyRunning(saveTask) || Bukkit.getScheduler().isQueued(saveTask))
            Bukkit.getScheduler().cancelTask(saveTask);
        if(dataManager != null)
            dataManager.saveSync();
    }
}
