package net.minecraft.server.integrated;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ThreadLanServerPing;
import net.minecraft.crash.CrashReport;
import net.minecraft.profiler.PlayerUsageSnooper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.CryptManager;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.*;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.world.demo.DemoWorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patch.WorldServerMultiOF;
import patch.WorldServerOF;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Callable;

public class IntegratedServer extends MinecraftServer {

    public static final Logger logger = LogManager.getLogger();
    public final Minecraft mc;
    public final WorldSettings theWorldSettings;
    public boolean isGamePaused;
    public boolean isPublic;
    public ThreadLanServerPing lanServerPing;



    public IntegratedServer(Minecraft par1Minecraft, String par2Str, String par3Str, WorldSettings par4WorldSettings) {
        super(new File(par1Minecraft.mcDataDir, "saves"), par1Minecraft.getProxy());
        this.setServerOwner(par1Minecraft.getSession().getUsername());
        this.setFolderName(par2Str);
        this.setWorldName(par3Str);
        this.setDemo(par1Minecraft.isDemo());
        this.canCreateBonusChest(par4WorldSettings.isBonusChestEnabled());
        this.setBuildLimit(256);
        this.func_152361_a(new IntegratedPlayerList(this));
        this.mc = par1Minecraft;
        this.theWorldSettings = par4WorldSettings;
       // Reflector.callVoid(Reflector.ModLoader_registerServer, new Object[]{this});

    }

    public void loadAllWorlds(String par1Str, String par2Str, long par3, WorldType par5WorldType, String par6Str) {
        this.convertMapIfNeeded(par1Str);
        ISaveHandler var7 = this.getActiveAnvilConverter().getSaveLoader(par1Str, true);

            World var8 = this.isDemo() ? new DemoWorldServer(this, var7, par2Str, 0, this.theProfiler) : new WorldServerOF(this, var7, par2Str, 0, this.theWorldSettings, this.theProfiler);
            Integer[] var9 = DimensionManager.getStaticDimensionIDs();
            for (Integer integer : var9) {
                int dim = integer;
                World world = dim == 0 ? var8 : new WorldServerMultiOF(this, var7, par2Str, dim, this.theWorldSettings, (WorldServer) var8, this.theProfiler);
                ((WorldServer) world).addWorldAccess(new WorldManager(this, (WorldServer) world));
                if (!this.isSinglePlayer()) {
                    ((WorldServer) world).getWorldInfo().setGameType(this.getGameType());
                }



                MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(world));
            }

            this.getConfigurationManager().setPlayerManager(new WorldServer[]{(WorldServer) var8});


        this.func_147139_a(this.func_147135_j());
        this.initialWorldChunkLoad();
    }

    public boolean startServer() throws IOException {
        logger.info("Starting integrated minecraft server version 1.7.10");
        this.setOnlineMode(true);
        this.setCanSpawnAnimals(true);
        this.setCanSpawnNPCs(true);
        this.setAllowPvp(true);
        this.setAllowFlight(true);
        logger.info("Generating keypair");
        this.setKeyPair(CryptManager.createNewKeyPair());
        FMLCommonHandler inst = FMLCommonHandler.instance();


            if (!inst.handleServerAboutToStart(this)) {
                return false;
            }


        this.loadAllWorlds(this.getFolderName(), this.getWorldName(), this.theWorldSettings.getSeed(), this.theWorldSettings.getTerrainType(), this.theWorldSettings.func_82749_j());
        this.setMOTD(this.getServerOwner() + " - " + this.worldServers[0].getWorldInfo().getWorldName());



         return inst.handleServerStarting(this);

    }

    public void tick() {
        boolean var1 = this.isGamePaused;
        this.isGamePaused = Minecraft.getMinecraft().getNetHandler() != null && Minecraft.getMinecraft().isGamePaused();
        if (!var1 && this.isGamePaused) {
            logger.info("Saving and pausing game...");
            this.getConfigurationManager().saveAllPlayerData();
            this.saveAllWorlds(false);
        }

        if (!this.isGamePaused) {
            super.tick();
            if (this.mc.gameSettings.renderDistanceChunks != this.getConfigurationManager().getViewDistance()) {
                logger.info("Changing view distance to {}, from {}", new Object[]{Integer.valueOf(this.mc.gameSettings.renderDistanceChunks), Integer.valueOf(this.getConfigurationManager().getViewDistance())});
                this.getConfigurationManager().func_152611_a(this.mc.gameSettings.renderDistanceChunks);
            }
        }

    }

    public boolean canStructuresSpawn() {
        return false;
    }

    public GameType getGameType() {
        return this.theWorldSettings.getGameType();
    }

    public EnumDifficulty func_147135_j() {
        return this.mc.gameSettings.difficulty;
    }

    public boolean isHardcore() {
        return this.theWorldSettings.getHardcoreEnabled();
    }

    public boolean func_152363_m() {
        return false;
    }

    public File getDataDirectory() {
        return this.mc.mcDataDir;
    }

    public boolean isDedicatedServer() {
        return false;
    }

    public void finalTick(CrashReport par1CrashReport) {
        this.mc.crashed(par1CrashReport);
    }

    public CrashReport addServerInfoToCrashReport(CrashReport par1CrashReport) {
        par1CrashReport = super.addServerInfoToCrashReport(par1CrashReport);
        par1CrashReport.getCategory().addCrashSectionCallable("Type", new Callable() {

            public static final String __OBFID = "CL_00001130";

            public String call() {
                return "Integrated Server (map_client.txt)";
            }
        });
        par1CrashReport.getCategory().addCrashSectionCallable("Is Modded", new Callable() {

            public static final String __OBFID = "CL_00001131";

            public String call() {
                String var1 = ClientBrandRetriever.getClientModName();
                if (!var1.equals("vanilla")) {
                    return "Definitely; Client brand changed to \'" + var1 + "\'";
                } else {
                    var1 = IntegratedServer.this.getServerModName();
                    return !var1.equals("vanilla") ? "Definitely; Server brand changed to \'" + var1 + "\'" : (Minecraft.class.getSigners() == null ? "Very likely; Jar signature invalidated" : "Probably not. Jar signature remains and both client + server brands are untouched.");
                }
            }
        });
        return par1CrashReport;
    }

    public void addServerStatsToSnooper(PlayerUsageSnooper par1PlayerUsageSnooper) {
        super.addServerStatsToSnooper(par1PlayerUsageSnooper);
        par1PlayerUsageSnooper.func_152768_a("snooper_partner", this.mc.getPlayerUsageSnooper().getUniqueID());
    }

    public boolean isSnooperEnabled() {
        return Minecraft.getMinecraft().isSnooperEnabled();
    }

    public String shareToLAN(GameType par1EnumGameType, boolean par2) {
        try {
            int var6 = -1;

            try {
                var6 = HttpUtil.func_76181_a();
            } catch (IOException var5) {
                ;
            }

            if (var6 <= 0) {
                var6 = 25564;
            }

            this.func_147137_ag().addLanEndpoint((InetAddress) null, var6);
            logger.info("Started on " + var6);
            this.isPublic = true;
            this.lanServerPing = new ThreadLanServerPing(this.getMOTD(), var6 + "");
            this.lanServerPing.start();
            this.getConfigurationManager().func_152604_a(par1EnumGameType);
            this.getConfigurationManager().setCommandsAllowedForAll(par2);
            return var6 + "";
        } catch (IOException var61) {
            return null;
        }
    }

    public void stopServer() {
        super.stopServer();
        if (this.lanServerPing != null) {
            this.lanServerPing.interrupt();
            this.lanServerPing = null;
        }

    }

    public void initiateShutdown() {
        super.initiateShutdown();
        if (this.lanServerPing != null) {
            this.lanServerPing.interrupt();
            this.lanServerPing = null;
        }

    }

    public boolean getPublic() {
        return this.isPublic;
    }

    public void setGameType(GameType par1EnumGameType) {
        this.getConfigurationManager().func_152604_a(par1EnumGameType);
    }

    public boolean isCommandBlockEnabled() {
        return true;
    }

    public int getOpPermissionLevel() {
        return 4;
    }

}
