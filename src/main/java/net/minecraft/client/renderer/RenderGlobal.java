package net.minecraft.client.renderer;

import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.util.RenderDistanceSorter;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemRecord;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.*;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.MinecraftForgeClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ARBOcclusionQuery;
import org.lwjgl.opengl.GL11;
import patch.*;

import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.Callable;

public class RenderGlobal implements IWorldAccess {

    public static final Logger logger = LogManager.getLogger();
    public static final ResourceLocation locationMoonPhasesPng = new ResourceLocation("textures/environment/moon_phases.png");
    public static final ResourceLocation locationSunPng = new ResourceLocation("textures/environment/sun.png");
    public static final ResourceLocation locationCloudsPng = new ResourceLocation("textures/environment/clouds.png");
    public static final ResourceLocation locationEndSkyPng = new ResourceLocation("textures/environment/end_sky.png");
    public List tileEntities = new ArrayList();
    public WorldClient theWorld;
    public final TextureManager renderEngine;
    public CompactArrayList worldRenderersToUpdate = new CompactArrayList(100, 0.8F);
    public WorldRenderer[] sortedWorldRenderers;
    public WorldRenderer[] worldRenderers;
    public int renderChunksWide;
    public int renderChunksTall;
    public int renderChunksDeep;
    public int glRenderListBase;
    public Minecraft mc;
    public RenderBlocks renderBlocksRg;
    public IntBuffer glOcclusionQueryBase;
    public boolean occlusionEnabled;
    public int cloudTickCounter;
    public int starGLCallList;
    public int glSkyList;
    public int glSkyList2;
    public int minBlockX;
    public int minBlockY;
    public int minBlockZ;
    public int maxBlockX;
    public int maxBlockY;
    public int maxBlockZ;
    public final Map damagedBlocks = new HashMap();
    public final Map mapSoundPositions = Maps.newHashMap();
    public IIcon[] destroyBlockIcons;
    public boolean displayListEntitiesDirty;
    public int displayListEntities;
    public int renderDistanceChunks = -1;
    public int renderEntitiesStartupCounter = 2;
    public int countEntitiesTotal;
    public int countEntitiesRendered;
    public int countEntitiesHidden;
    public IntBuffer occlusionResult = GLAllocation.createDirectIntBuffer(64);
    public int renderersLoaded;
    public int renderersBeingClipped;
    public int renderersBeingOccluded;
    public int renderersBeingRendered;
    public int renderersSkippingRenderPass;
    public int dummyRenderInt;
    public int worldRenderersCheckIndex;
    public List glRenderLists = new ArrayList();
    public RenderList[] allRenderLists = new RenderList[]{new RenderList(), new RenderList(), new RenderList(), new RenderList()};
    public double prevSortX = -9999.0D;
    public double prevSortY = -9999.0D;
    public double prevSortZ = -9999.0D;
    public double prevRenderSortX = -9999.0D;
    public double prevRenderSortY = -9999.0D;
    public double prevRenderSortZ = -9999.0D;
    public int prevChunkSortX = -999;
    public int prevChunkSortY = -999;
    public int prevChunkSortZ = -999;
    public int frustumCheckOffset;
    public static final String __OBFID = "CL_00000954";
    public IntBuffer glListBuffer = BufferUtils.createIntBuffer(65536);
    public double prevReposX;
    public double prevReposY;
    public double prevReposZ;
    public Entity renderedEntity;
    public int glListClouds = -1;
    public boolean isFancyGlListClouds = false;
    public int cloudTickCounterGlList = -99999;
    public double cloudPlayerX = 0.0D;
    public double cloudPlayerY = 0.0D;
    public double cloudPlayerZ = 0.0D;
    public int countSortedWorldRenderers = 0;
    public int effectivePreloadedChunks = 0;
    public int vertexResortCounter = 30;
    public WrDisplayListAllocator displayListAllocator = new WrDisplayListAllocator();
    public EntityLivingBase renderViewEntity;
    public int countTileEntitiesRendered;
    public long lastMovedTime = System.currentTimeMillis();
    public long lastActionTime = System.currentTimeMillis();
    public static AxisAlignedBB AABB_INFINITE = AxisAlignedBB.getBoundingBox(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);


    public RenderGlobal(Minecraft par1Minecraft) {
        this.glListClouds = GLAllocation.generateDisplayLists(1);
        this.mc = par1Minecraft;
        this.renderEngine = par1Minecraft.getTextureManager();
        byte maxChunkDim = 65;
        byte maxChunkHeight = 16;
        int countWorldRenderers = maxChunkDim * maxChunkDim * maxChunkHeight;
        int countStandardRenderLists = countWorldRenderers * 3;
        this.glRenderListBase = GLAllocation.generateDisplayLists(countStandardRenderLists);
        this.displayListEntitiesDirty = false;
        this.displayListEntities = GLAllocation.generateDisplayLists(1);
        this.occlusionEnabled = OpenGlCapsChecker.checkARBOcclusion();
        if (this.occlusionEnabled) {
            this.occlusionResult.clear();
            this.glOcclusionQueryBase = GLAllocation.createDirectIntBuffer(maxChunkDim * maxChunkDim * maxChunkHeight);
            this.glOcclusionQueryBase.clear();
            this.glOcclusionQueryBase.position(0);
            this.glOcclusionQueryBase.limit(maxChunkDim * maxChunkDim * maxChunkHeight);
            ARBOcclusionQuery.glGenQueriesARB(this.glOcclusionQueryBase);
        }

        this.starGLCallList = GLAllocation.generateDisplayLists(3);
        GL11.glPushMatrix();
        GL11.glNewList(this.starGLCallList, 4864);
        this.renderStars();
        GL11.glEndList();
        GL11.glPopMatrix();
        Tessellator var4 = Tessellator.instance;
        this.glSkyList = this.starGLCallList + 1;
        GL11.glNewList(this.glSkyList, 4864);
        byte var6 = 64;
        int var7 = 256 / var6 + 2;
        float var5 = 16.0F;

        int var8;
        int var9;
        for (var8 = -var6 * var7; var8 <= var6 * var7; var8 += var6) {
            for (var9 = -var6 * var7; var9 <= var6 * var7; var9 += var6) {
                var4.startDrawingQuads();
                var4.addVertex((double) (var8 + 0), (double) var5, (double) (var9 + 0));
                var4.addVertex((double) (var8 + var6), (double) var5, (double) (var9 + 0));
                var4.addVertex((double) (var8 + var6), (double) var5, (double) (var9 + var6));
                var4.addVertex((double) (var8 + 0), (double) var5, (double) (var9 + var6));
                var4.draw();
            }
        }

        GL11.glEndList();
        this.glSkyList2 = this.starGLCallList + 2;
        GL11.glNewList(this.glSkyList2, 4864);
        var5 = -16.0F;
        var4.startDrawingQuads();

        for (var8 = -var6 * var7; var8 <= var6 * var7; var8 += var6) {
            for (var9 = -var6 * var7; var9 <= var6 * var7; var9 += var6) {
                var4.addVertex((double) (var8 + var6), (double) var5, (double) (var9 + 0));
                var4.addVertex((double) (var8 + 0), (double) var5, (double) (var9 + 0));
                var4.addVertex((double) (var8 + 0), (double) var5, (double) (var9 + var6));
                var4.addVertex((double) (var8 + var6), (double) var5, (double) (var9 + var6));
            }
        }

        var4.draw();
        GL11.glEndList();
    }

    public void renderStars() {
        Random var1 = new Random(10842L);
        Tessellator var2 = Tessellator.instance;
        var2.startDrawingQuads();

        for (int var3 = 0; var3 < 1500; ++var3) {
            double var4 = (double) (var1.nextFloat() * 2.0F - 1.0F);
            double var6 = (double) (var1.nextFloat() * 2.0F - 1.0F);
            double var8 = (double) (var1.nextFloat() * 2.0F - 1.0F);
            double var10 = (double) (0.15F + var1.nextFloat() * 0.1F);
            double var12 = var4 * var4 + var6 * var6 + var8 * var8;
            if (var12 < 1.0D && var12 > 0.01D) {
                var12 = 1.0D / Math.sqrt(var12);
                var4 *= var12;
                var6 *= var12;
                var8 *= var12;
                double var14 = var4 * 100.0D;
                double var16 = var6 * 100.0D;
                double var18 = var8 * 100.0D;
                double var20 = Math.atan2(var4, var8);
                double var22 = Math.sin(var20);
                double var24 = Math.cos(var20);
                double var26 = Math.atan2(Math.sqrt(var4 * var4 + var8 * var8), var6);
                double var28 = Math.sin(var26);
                double var30 = Math.cos(var26);
                double var32 = var1.nextDouble() * 3.141592653589793D * 2.0D;
                double var34 = Math.sin(var32);
                double var36 = Math.cos(var32);

                for (int var38 = 0; var38 < 4; ++var38) {
                    double var39 = 0.0D;
                    double var41 = (double) ((var38 & 2) - 1) * var10;
                    double var43 = (double) ((var38 + 1 & 2) - 1) * var10;
                    double var47 = var41 * var36 - var43 * var34;
                    double var49 = var43 * var36 + var41 * var34;
                    double var53 = var47 * var28 + var39 * var30;
                    double var55 = var39 * var28 - var47 * var30;
                    double var57 = var55 * var22 - var49 * var24;
                    double var61 = var49 * var22 + var55 * var24;
                    var2.addVertex(var14 + var57, var16 + var53, var18 + var61);
                }
            }
        }

        var2.draw();
    }

    public void setWorldAndLoadRenderers(WorldClient par1WorldClient) {
        if (this.theWorld != null) {
            this.theWorld.removeWorldAccess(this);
        }

        this.prevSortX = -9999.0D;
        this.prevSortY = -9999.0D;
        this.prevSortZ = -9999.0D;
        this.prevRenderSortX = -9999.0D;
        this.prevRenderSortY = -9999.0D;
        this.prevRenderSortZ = -9999.0D;
        this.prevChunkSortX = -9999;
        this.prevChunkSortY = -9999;
        this.prevChunkSortZ = -9999;
        RenderManager.instance.set(par1WorldClient);
        this.theWorld = par1WorldClient;
        this.renderBlocksRg = new RenderBlocks(par1WorldClient);
        if (Config.isDynamicLights()) {
            DynamicLights.clear();
        }

        if (par1WorldClient != null) {
            par1WorldClient.addWorldAccess(this);
            this.loadRenderers();
        }

    }

    public void loadRenderers() {
        if (this.theWorld != null) {
            Blocks.leaves.setGraphicsLevel(Config.isTreesFancy());
            Blocks.leaves2.setGraphicsLevel(Config.isTreesFancy());
            this.renderDistanceChunks = this.mc.gameSettings.renderDistanceChunks;
            WrUpdates.clearAllUpdates();
            int numChunks;
            if (this.worldRenderers != null) {
                for (numChunks = 0; numChunks < this.worldRenderers.length; ++numChunks) {
                    this.worldRenderers[numChunks].stopRendering();
                }
            }

            if (Config.isDynamicLights()) {
                DynamicLights.clear();
            }

            numChunks = this.mc.gameSettings.renderDistanceChunks;
            byte numChunksFar = 16;
            if (Config.isLoadChunksFar() && numChunks < numChunksFar) {
                numChunks = numChunksFar;
            }

            int maxPreloadedChunks = Config.limit(numChunksFar - numChunks, 0, 8);
            this.effectivePreloadedChunks = Config.limit(Config.getPreloadedChunks(), 0, maxPreloadedChunks);
            numChunks += this.effectivePreloadedChunks;
            byte limit = 32;
            if (numChunks > limit) {
                numChunks = limit;
            }

            this.prevReposX = -9999.0D;
            this.prevReposY = -9999.0D;
            this.prevReposZ = -9999.0D;
            int var1 = numChunks * 2 + 1;
            this.renderChunksWide = var1;
            this.renderChunksTall = 16;
            this.renderChunksDeep = var1;
            this.worldRenderers = new WorldRenderer[this.renderChunksWide * this.renderChunksTall * this.renderChunksDeep];
            this.sortedWorldRenderers = new WorldRenderer[this.renderChunksWide * this.renderChunksTall * this.renderChunksDeep];
            this.countSortedWorldRenderers = 0;
            this.displayListAllocator.resetAllocatedLists();
            int var2 = 0;
            int var3 = 0;
            this.minBlockX = 0;
            this.minBlockY = 0;
            this.minBlockZ = 0;
            this.maxBlockX = this.renderChunksWide;
            this.maxBlockY = this.renderChunksTall;
            this.maxBlockZ = this.renderChunksDeep;

            int var10;
            for (var10 = 0; var10 < this.worldRenderersToUpdate.size(); ++var10) {
                WorldRenderer esf = (WorldRenderer) this.worldRenderersToUpdate.get(var10);
                if (esf != null) {
                    esf.needsUpdate = false;
                }
            }

            this.worldRenderersToUpdate.clear();
            this.tileEntities.clear();
            this.onStaticEntitiesChanged();

            for (var10 = 0; var10 < this.renderChunksWide; ++var10) {
                for (int var13 = 0; var13 < this.renderChunksTall; ++var13) {
                    for (int cz = 0; cz < this.renderChunksDeep; ++cz) {
                        int wri = (cz * this.renderChunksTall + var13) * this.renderChunksWide + var10;
                        this.worldRenderers[wri] = WrUpdates.makeWorldRenderer(this.theWorld, this.tileEntities, var10 * 16, var13 * 16, cz * 16, this.glRenderListBase + var2);
                        if (this.occlusionEnabled) {
                            this.worldRenderers[wri].glOcclusionQuery = this.glOcclusionQueryBase.get(var3);
                        }

                        this.worldRenderers[wri].isWaitingOnOcclusionQuery = false;
                        this.worldRenderers[wri].isVisible = true;
                        this.worldRenderers[wri].isInFrustum = false;
                        this.worldRenderers[wri].chunkIndex = var3++;
                        if (this.theWorld.blockExists(var10 << 4, 0, cz << 4)) {
                            this.worldRenderers[wri].markDirty();
                            this.worldRenderersToUpdate.add(this.worldRenderers[wri]);
                        }

                        var2 += 3;
                    }
                }
            }

            if (this.theWorld != null) {
                Object var12 = this.mc.renderViewEntity;
                if (var12 == null) {
                    var12 = this.mc.thePlayer;
                }

                if (var12 != null) {
                    this.markRenderersForNewPosition(MathHelper.floor_double(((EntityLivingBase) var12).posX), MathHelper.floor_double(((EntityLivingBase) var12).posY), MathHelper.floor_double(((EntityLivingBase) var12).posZ));
                    EntitySorterFast var14 = new EntitySorterFast((Entity) var12);
                    var14.prepareToSort(this.sortedWorldRenderers, this.countSortedWorldRenderers);
                    Arrays.sort(this.sortedWorldRenderers, 0, this.countSortedWorldRenderers, var14);
                }
            }

            this.renderEntitiesStartupCounter = 2;
        }

    }

    public void renderEntities(EntityLivingBase p_147589_1_, ICamera p_147589_2_, float p_147589_3_) {
        int pass = MinecraftForgeClient.getRenderPass();

        if (this.renderEntitiesStartupCounter > 0) {
            if (pass > 0) {
                return;
            }

            --this.renderEntitiesStartupCounter;
        } else {
            double var4 = p_147589_1_.prevPosX + (p_147589_1_.posX - p_147589_1_.prevPosX) * (double) p_147589_3_;
            double var6 = p_147589_1_.prevPosY + (p_147589_1_.posY - p_147589_1_.prevPosY) * (double) p_147589_3_;
            double var8 = p_147589_1_.prevPosZ + (p_147589_1_.posZ - p_147589_1_.prevPosZ) * (double) p_147589_3_;
            this.theWorld.theProfiler.startSection("prepare");
            TileEntityRendererDispatcher.instance.cacheActiveRenderInfo(this.theWorld, this.mc.getTextureManager(), this.mc.fontRenderer, this.mc.renderViewEntity, p_147589_3_);
            RenderManager.instance.cacheActiveRenderInfo(this.theWorld, this.mc.getTextureManager(), this.mc.fontRenderer, this.mc.renderViewEntity, this.mc.pointedEntity, this.mc.gameSettings, p_147589_3_);
            if (pass == 0) {
                this.countEntitiesTotal = 0;
                this.countEntitiesRendered = 0;
                this.countEntitiesHidden = 0;
                this.countTileEntitiesRendered = 0;
                EntityLivingBase var17 = this.mc.renderViewEntity;
                double var11 = var17.lastTickPosX + (var17.posX - var17.lastTickPosX) * (double) p_147589_3_;
                double var13 = var17.lastTickPosY + (var17.posY - var17.lastTickPosY) * (double) p_147589_3_;
                double var15 = var17.lastTickPosZ + (var17.posZ - var17.lastTickPosZ) * (double) p_147589_3_;
                TileEntityRendererDispatcher.staticPlayerX = var11;
                TileEntityRendererDispatcher.staticPlayerY = var13;
                TileEntityRendererDispatcher.staticPlayerZ = var15;
                this.theWorld.theProfiler.endStartSection("staticentities");
                if (this.displayListEntitiesDirty) {
                    RenderManager.renderPosX = 0.0D;
                    RenderManager.renderPosY = 0.0D;
                    RenderManager.renderPosZ = 0.0D;
                    this.rebuildDisplayListEntities();
                }

                GL11.glMatrixMode(5888);
                GL11.glPushMatrix();
                GL11.glTranslated(-var11, -var13, -var15);
                GL11.glCallList(this.displayListEntities);
                GL11.glPopMatrix();
                RenderManager.renderPosX = var11;
                RenderManager.renderPosY = var13;
                RenderManager.renderPosZ = var15;
            }

            this.mc.entityRenderer.enableLightmap((double) p_147589_3_);
            this.theWorld.theProfiler.endStartSection("global");
            List var30 = this.theWorld.getLoadedEntityList();
            if (pass == 0) {
                this.countEntitiesTotal = var30.size();
            }

            if (Config.isFogOff() && this.mc.entityRenderer.fogStandard) {
                GL11.glDisable(2912);
            }

            int var18;
            Entity var19;
            for (var18 = 0; var18 < this.theWorld.weatherEffects.size(); ++var18) {
                var19 = (Entity) this.theWorld.weatherEffects.get(var18);
                if (var19.shouldRenderInPass(pass)) {
                    ++this.countEntitiesRendered;
                    if (var19.isInRangeToRender3d(var4, var6, var8)) {
                        RenderManager.instance.renderEntitySimple(var19, p_147589_3_);
                    }
                }
            }

            this.theWorld.theProfiler.endStartSection("entities");
            boolean oldFancyGraphics = this.mc.gameSettings.fancyGraphics;
            this.mc.gameSettings.fancyGraphics = Config.isDroppedItemsFancy();

            for (var18 = 0; var18 < var30.size(); ++var18) {
                var19 = (Entity) var30.get(var18);
                if (var19.shouldRenderInPass(pass)) {
                    boolean te = var19.isInRangeToRender3d(var4, var6, var8) && (var19.ignoreFrustumCheck || p_147589_2_.isBoundingBoxInFrustum(var19.boundingBox) || var19.riddenByEntity == this.mc.thePlayer);
                    if (!te && var19 instanceof EntityLiving) {
                        EntityLiving aabb = (EntityLiving) var19;
                        if (aabb.getLeashed() && aabb.getLeashedToEntity() != null) {
                            Entity teClass = aabb.getLeashedToEntity();
                            te = p_147589_2_.isBoundingBoxInFrustum(teClass.boundingBox);
                        }
                    }

                    if (te && (var19 != this.mc.renderViewEntity || this.mc.gameSettings.thirdPersonView != 0 || this.mc.renderViewEntity.isPlayerSleeping()) && this.theWorld.blockExists(MathHelper.floor_double(var19.posX), 0, MathHelper.floor_double(var19.posZ))) {
                        ++this.countEntitiesRendered;
                        if (var19.getClass() == EntityItemFrame.class) {
                            var19.renderDistanceWeight = 0.06D;
                        }

                        this.renderedEntity = var19;
                        RenderManager.instance.renderEntitySimple(var19, p_147589_3_);
                        this.renderedEntity = null;
                    }
                }
            }

            this.mc.gameSettings.fancyGraphics = oldFancyGraphics;
            this.theWorld.theProfiler.endStartSection("blockentities");
            RenderHelper.enableStandardItemLighting();

            for (var18 = 0; var18 < this.tileEntities.size(); ++var18) {
                TileEntity var31 = (TileEntity) this.tileEntities.get(var18);
                if (var31.shouldRenderInPass(pass)) {
                    AxisAlignedBB var32 = this.getTileEntityBoundingBox(var31);
                    if (var32 == AABB_INFINITE || p_147589_2_.isBoundingBoxInFrustum(var32)) {
                        Class var33 = var31.getClass();
                        if (var33 == TileEntitySign.class && !Config.zoomMode) {
                            EntityClientPlayerMP block = this.mc.thePlayer;
                            double distSq = var31.getDistanceFrom(block.posX, block.posY, block.posZ);
                            if (distSq > 256.0D) {
                                FontRenderer fr = TileEntityRendererDispatcher.instance.getFontRenderer();
                                fr.enabled = false;
                                TileEntityRendererDispatcher.instance.renderTileEntity(var31, p_147589_3_);
                                ++this.countTileEntitiesRendered;
                                fr.enabled = true;
                                continue;
                            }
                        }

                        if (var33 == TileEntityChest.class) {
                            Block var34 = this.theWorld.getBlock(var31.xCoord, var31.yCoord, var31.zCoord);
                            if (!(var34 instanceof BlockChest)) {
                                continue;
                            }
                        }

                        TileEntityRendererDispatcher.instance.renderTileEntity(var31, p_147589_3_);
                        ++this.countTileEntitiesRendered;
                    }
                }
            }

            this.mc.entityRenderer.disableLightmap((double) p_147589_3_);
            this.theWorld.theProfiler.endSection();
        }

    }

    public String getDebugInfoRenders() {
        return "C: " + this.renderersBeingRendered + "/" + this.renderersLoaded + ". F: " + this.renderersBeingClipped + ", O: " + this.renderersBeingOccluded + ", E: " + this.renderersSkippingRenderPass;
    }

    public String getDebugInfoEntities() {
        return "E: " + this.countEntitiesRendered + "/" + this.countEntitiesTotal + ". B: " + this.countEntitiesHidden + ", I: " + (this.countEntitiesTotal - this.countEntitiesHidden - this.countEntitiesRendered) + ", " + Config.getVersionDebug();
    }

    public void onStaticEntitiesChanged() {
        this.displayListEntitiesDirty = true;
    }

    public void rebuildDisplayListEntities() {
        this.theWorld.theProfiler.startSection("staticentityrebuild");
        GL11.glPushMatrix();
        GL11.glNewList(this.displayListEntities, 4864);
        List var1 = this.theWorld.getLoadedEntityList();
        this.displayListEntitiesDirty = false;

        for (int var2 = 0; var2 < var1.size(); ++var2) {
            Entity var3 = (Entity) var1.get(var2);
            if (RenderManager.instance.getEntityRenderObject(var3).isStaticEntity()) {
                this.displayListEntitiesDirty = this.displayListEntitiesDirty || !RenderManager.instance.renderEntityStatic(var3, 0.0F, true);
            }
        }

        GL11.glEndList();
        GL11.glPopMatrix();
        this.theWorld.theProfiler.endSection();
    }

    public void markRenderersForNewPosition(int x, int y, int z) {
        x -= 8;
        y -= 8;
        z -= 8;
        this.minBlockX = Integer.MAX_VALUE;
        this.minBlockY = Integer.MAX_VALUE;
        this.minBlockZ = Integer.MAX_VALUE;
        this.maxBlockX = Integer.MIN_VALUE;
        this.maxBlockY = Integer.MIN_VALUE;
        this.maxBlockZ = Integer.MIN_VALUE;
        int blocksWide = this.renderChunksWide * 16;
        int blocksWide2 = blocksWide / 2;

        for (int ix = 0; ix < this.renderChunksWide; ++ix) {
            int blockX = ix * 16;
            int blockXAbs = blockX + blocksWide2 - x;
            if (blockXAbs < 0) {
                blockXAbs -= blocksWide - 1;
            }

            blockXAbs /= blocksWide;
            blockX -= blockXAbs * blocksWide;
            if (blockX < this.minBlockX) {
                this.minBlockX = blockX;
            }

            if (blockX > this.maxBlockX) {
                this.maxBlockX = blockX;
            }

            for (int iz = 0; iz < this.renderChunksDeep; ++iz) {
                int blockZ = iz * 16;
                int blockZAbs = blockZ + blocksWide2 - z;
                if (blockZAbs < 0) {
                    blockZAbs -= blocksWide - 1;
                }

                blockZAbs /= blocksWide;
                blockZ -= blockZAbs * blocksWide;
                if (blockZ < this.minBlockZ) {
                    this.minBlockZ = blockZ;
                }

                if (blockZ > this.maxBlockZ) {
                    this.maxBlockZ = blockZ;
                }

                for (int iy = 0; iy < this.renderChunksTall; ++iy) {
                    int blockY = iy * 16;
                    if (blockY < this.minBlockY) {
                        this.minBlockY = blockY;
                    }

                    if (blockY > this.maxBlockY) {
                        this.maxBlockY = blockY;
                    }

                    WorldRenderer worldrenderer = this.worldRenderers[(iz * this.renderChunksTall + iy) * this.renderChunksWide + ix];
                    boolean wasNeedingUpdate = worldrenderer.needsUpdate;
                    worldrenderer.setPosition(blockX, blockY, blockZ);
                    if (!wasNeedingUpdate && worldrenderer.needsUpdate) {
                        this.worldRenderersToUpdate.add(worldrenderer);
                    }
                }
            }
        }

    }

    public int sortAndRender(EntityLivingBase player, int renderPass, double partialTicks) {
        this.renderViewEntity = player;
        if (Config.isDynamicLights()) {
            DynamicLights.update(this);
        }

        Profiler profiler = this.theWorld.theProfiler;
        profiler.startSection("sortchunks");
        int num;
        if (this.worldRenderersToUpdate.size() < 10) {
            byte shouldSort = 10;

            for (num = 0; num < shouldSort; ++num) {
                this.worldRenderersCheckIndex = (this.worldRenderersCheckIndex + 1) % this.worldRenderers.length;
                WorldRenderer ocReq = this.worldRenderers[this.worldRenderersCheckIndex];
                if (ocReq.needsUpdate && !this.worldRenderersToUpdate.contains(ocReq)) {
                    this.worldRenderersToUpdate.add(ocReq);
                }
            }
        }

        if (this.mc.gameSettings.renderDistanceChunks != this.renderDistanceChunks && !Config.isLoadChunksFar()) {
            this.loadRenderers();
        }

        if (renderPass == 0) {
            this.renderersLoaded = 0;
            this.dummyRenderInt = 0;
            this.renderersBeingClipped = 0;
            this.renderersBeingOccluded = 0;
            this.renderersBeingRendered = 0;
            this.renderersSkippingRenderPass = 0;
        }

        boolean var45 = this.prevChunkSortX != player.chunkCoordX || this.prevChunkSortY != player.chunkCoordY || this.prevChunkSortZ != player.chunkCoordZ;
        double var17;
        double partialX;
        double partialY;
        double partialZ;
        if (!var45) {
            var17 = player.posX - this.prevSortX;
            partialX = player.posY - this.prevSortY;
            partialY = player.posZ - this.prevSortZ;
            partialZ = var17 * var17 + partialX * partialX + partialY * partialY;
            var45 = partialZ > 16.0D;
        }

        int endIndex;
        int stepNum;
        if (var45) {
            this.prevChunkSortX = player.chunkCoordX;
            this.prevChunkSortY = player.chunkCoordY;
            this.prevChunkSortZ = player.chunkCoordZ;
            this.prevSortX = player.posX;
            this.prevSortY = player.posY;
            this.prevSortZ = player.posZ;
            num = this.effectivePreloadedChunks * 16;
            double dReposX = player.posX - this.prevReposX;
            double dReposY = player.posY - this.prevReposY;
            double dReposZ = player.posZ - this.prevReposZ;
            double distSqRepos = dReposX * dReposX + dReposY * dReposY + dReposZ * dReposZ;
            if (distSqRepos > (double) (num * num) + 16.0D) {
                this.prevReposX = player.posX;
                this.prevReposY = player.posY;
                this.prevReposZ = player.posZ;
                this.markRenderersForNewPosition(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ));
            }

            EntitySorterFast lastIndex = new EntitySorterFast(player);
            lastIndex.prepareToSort(this.sortedWorldRenderers, this.countSortedWorldRenderers);
            Arrays.sort(this.sortedWorldRenderers, 0, this.countSortedWorldRenderers, lastIndex);
            if (Config.isFastRender()) {
                endIndex = (int) player.posX;
                stepNum = (int) player.posZ;
                short step = 2000;
                if (Math.abs(endIndex - WorldRenderer.globalChunkOffsetX) > step || Math.abs(stepNum - WorldRenderer.globalChunkOffsetZ) > step) {
                    WorldRenderer.globalChunkOffsetX = endIndex;
                    WorldRenderer.globalChunkOffsetZ = stepNum;
                    this.loadRenderers();
                }
            }
        }

        if (Config.isTranslucentBlocksFancy()) {
            var17 = player.posX - this.prevRenderSortX;
            partialX = player.posY - this.prevRenderSortY;
            partialY = player.posZ - this.prevRenderSortZ;
            int countResort = Math.min(27, this.countSortedWorldRenderers);
            WorldRenderer firstIndex;
            if (var17 * var17 + partialX * partialX + partialY * partialY > 1.0D) {
                this.prevRenderSortX = player.posX;
                this.prevRenderSortY = player.posY;
                this.prevRenderSortZ = player.posZ;

                for (int var23 = 0; var23 < countResort; ++var23) {
                    firstIndex = this.sortedWorldRenderers[var23];
                    if (firstIndex.vertexState != null && firstIndex.sortDistanceToEntitySquared < 1152.0F) {
                        firstIndex.needVertexStateResort = true;
                        if (this.vertexResortCounter > var23) {
                            this.vertexResortCounter = var23;
                        }
                    }
                }
            }

            if (this.vertexResortCounter < countResort) {
                while (this.vertexResortCounter < countResort) {
                    firstIndex = this.sortedWorldRenderers[this.vertexResortCounter];
                    ++this.vertexResortCounter;
                    if (firstIndex.needVertexStateResort) {
                        firstIndex.updateRendererSort(player);
                        break;
                    }
                }
            }
        }

        RenderHelper.disableStandardItemLighting();
        WrUpdates.preRender(this, player);
        if (this.mc.gameSettings.ofSmoothFps && renderPass == 0) {
            GL11.glFinish();
        }

        byte var46 = 0;
        int var47 = 0;
        if (this.occlusionEnabled && this.mc.gameSettings.advancedOpengl && !this.mc.gameSettings.anaglyph && renderPass == 0) {
            partialX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
            partialY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
            partialZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
            byte var50 = 0;
            int var48 = Math.min(20, this.countSortedWorldRenderers);
            this.checkOcclusionQueryResult(var50, var48, player.posX, player.posY, player.posZ);

            for (endIndex = var50; endIndex < var48; ++endIndex) {
                this.sortedWorldRenderers[endIndex].isVisible = true;
            }

            profiler.endStartSection("render");
            num = var46 + this.renderSortedRenderers(var50, var48, renderPass, partialTicks);
            endIndex = var48;
            stepNum = 0;
            byte var49 = 40;

            int startIndex;
            for (int switchStep = this.renderChunksWide; endIndex < this.countSortedWorldRenderers; num += this.renderSortedRenderers(startIndex, endIndex, renderPass, partialTicks)) {
                profiler.endStartSection("occ");
                startIndex = endIndex;
                if (stepNum < switchStep) {
                    ++stepNum;
                } else {
                    --stepNum;
                }

                endIndex += stepNum * var49;
                if (endIndex <= startIndex) {
                    endIndex = startIndex + 10;
                }

                if (endIndex > this.countSortedWorldRenderers) {
                    endIndex = this.countSortedWorldRenderers;
                }

                GL11.glDisable(3553);
                GL11.glDisable(2896);
                GL11.glDisable(3008);
                GL11.glDisable(2912);
                GL11.glColorMask(false, false, false, false);
                GL11.glDepthMask(false);
                profiler.startSection("check");
                this.checkOcclusionQueryResult(startIndex, endIndex, player.posX, player.posY, player.posZ);
                profiler.endSection();
                GL11.glPushMatrix();
                float sumTX = 0.0F;
                float sumTY = 0.0F;
                float sumTZ = 0.0F;

                for (int k = startIndex; k < endIndex; ++k) {
                    WorldRenderer wr = this.sortedWorldRenderers[k];
                    if (wr.skipAllRenderPasses()) {
                        wr.isInFrustum = false;
                    } else if (!wr.isUpdating && !wr.needsBoxUpdate) {
                        if (wr.isInFrustum) {
                            if (Config.isOcclusionFancy() && !wr.isInFrustrumFully) {
                                wr.isVisible = true;
                            } else if (wr.isInFrustum && !wr.isWaitingOnOcclusionQuery) {
                                float bbX;
                                float bbY;
                                float bbZ;
                                float tX;
                                if (wr.isVisibleFromPosition) {
                                    bbX = Math.abs((float) (wr.visibleFromX - player.posX));
                                    bbY = Math.abs((float) (wr.visibleFromY - player.posY));
                                    bbZ = Math.abs((float) (wr.visibleFromZ - player.posZ));
                                    tX = bbX + bbY + bbZ;
                                    if ((double) tX < 10.0D + (double) k / 1000.0D) {
                                        wr.isVisible = true;
                                        continue;
                                    }

                                    wr.isVisibleFromPosition = false;
                                }

                                bbX = (float) ((double) wr.posXMinus - partialX);
                                bbY = (float) ((double) wr.posYMinus - partialY);
                                bbZ = (float) ((double) wr.posZMinus - partialZ);
                                tX = bbX - sumTX;
                                float tY = bbY - sumTY;
                                float tZ = bbZ - sumTZ;
                                if (tX != 0.0F || tY != 0.0F || tZ != 0.0F) {
                                    GL11.glTranslatef(tX, tY, tZ);
                                    sumTX += tX;
                                    sumTY += tY;
                                    sumTZ += tZ;
                                }

                                profiler.startSection("bb");
                                ARBOcclusionQuery.glBeginQueryARB('\u8914', wr.glOcclusionQuery);
                                wr.callOcclusionQueryList();
                                ARBOcclusionQuery.glEndQueryARB('\u8914');
                                profiler.endSection();
                                wr.isWaitingOnOcclusionQuery = true;
                                ++var47;
                            }
                        }
                    } else {
                        wr.isVisible = true;
                    }
                }

                GL11.glPopMatrix();
                if (this.mc.gameSettings.anaglyph) {
                    if (EntityRenderer.anaglyphField == 0) {
                        GL11.glColorMask(false, true, true, true);
                    } else {
                        GL11.glColorMask(true, false, false, true);
                    }
                } else {
                    GL11.glColorMask(true, true, true, true);
                }

                GL11.glDepthMask(true);
                GL11.glEnable(3553);
                GL11.glEnable(3008);
                GL11.glEnable(2912);
                profiler.endStartSection("render");
            }
        } else {
            profiler.endStartSection("render");
            num = var46 + this.renderSortedRenderers(0, this.countSortedWorldRenderers, renderPass, partialTicks);
        }

        profiler.endSection();
        WrUpdates.postRender();
        return num;
    }

    public void checkOcclusionQueryResult(int startIndex, int endIndex, double px, double py, double pz) {
        for (int k = startIndex; k < endIndex; ++k) {
            WorldRenderer wr = this.sortedWorldRenderers[k];
            if (wr.isWaitingOnOcclusionQuery) {
                this.occlusionResult.clear();
                ARBOcclusionQuery.glGetQueryObjectuARB(wr.glOcclusionQuery, '\u8867', this.occlusionResult);
                if (this.occlusionResult.get(0) != 0) {
                    wr.isWaitingOnOcclusionQuery = false;
                    this.occlusionResult.clear();
                    ARBOcclusionQuery.glGetQueryObjectuARB(wr.glOcclusionQuery, '\u8866', this.occlusionResult);
                    if (!wr.isUpdating && !wr.needsBoxUpdate) {
                        boolean wasVisible = wr.isVisible;
                        wr.isVisible = this.occlusionResult.get(0) > 0;
                        if (wasVisible && wr.isVisible) {
                            wr.isVisibleFromPosition = true;
                            wr.visibleFromX = px;
                            wr.visibleFromY = py;
                            wr.visibleFromZ = pz;
                        }
                    } else {
                        wr.isVisible = true;
                    }
                }
            }
        }

    }

    public int renderSortedRenderers(int par1, int par2, int par3, double par4) {
        if (Config.isFastRender()) {
            return this.renderSortedRenderersFast(par1, par2, par3, par4);
        } else {
            this.glRenderLists.clear();
            int var6 = 0;
            int var7 = par1;
            int var8 = par2;
            byte var9 = 1;
            if (par3 == 1) {
                var7 = this.countSortedWorldRenderers - 1 - par1;
                var8 = this.countSortedWorldRenderers - 1 - par2;
                var9 = -1;
            }

            for (int var22 = var7; var22 != var8; var22 += var9) {
                if (par3 == 0) {
                    ++this.renderersLoaded;
                    if (this.sortedWorldRenderers[var22].skipRenderPass[par3]) {
                        ++this.renderersSkippingRenderPass;
                    } else if (!this.sortedWorldRenderers[var22].isInFrustum) {
                        ++this.renderersBeingClipped;
                    } else if (this.occlusionEnabled && !this.sortedWorldRenderers[var22].isVisible) {
                        ++this.renderersBeingOccluded;
                    } else {
                        ++this.renderersBeingRendered;
                    }
                }

                if (!this.sortedWorldRenderers[var22].skipRenderPass[par3] && this.sortedWorldRenderers[var22].isInFrustum && (!this.occlusionEnabled || this.sortedWorldRenderers[var22].isVisible)) {
                    int var11 = this.sortedWorldRenderers[var22].getGLCallListForPass(par3);
                    if (var11 >= 0) {
                        this.glRenderLists.add(this.sortedWorldRenderers[var22]);
                        ++var6;
                    }
                }
            }

            if (var6 == 0) {
                return 0;
            } else {
                EntityLivingBase var231 = this.mc.renderViewEntity;
                double var23 = var231.lastTickPosX + (var231.posX - var231.lastTickPosX) * par4;
                double var13 = var231.lastTickPosY + (var231.posY - var231.lastTickPosY) * par4;
                double var15 = var231.lastTickPosZ + (var231.posZ - var231.lastTickPosZ) * par4;
                int var17 = 0;

                int var18;
                for (var18 = 0; var18 < this.allRenderLists.length; ++var18) {
                    this.allRenderLists[var18].resetList();
                }

                int var20;
                int var21;
                for (var18 = 0; var18 < this.glRenderLists.size(); ++var18) {
                    WorldRenderer var24 = (WorldRenderer) this.glRenderLists.get(var18);
                    var20 = -1;

                    for (var21 = 0; var21 < var17; ++var21) {
                        if (this.allRenderLists[var21].rendersChunk(var24.posXMinus, var24.posYMinus, var24.posZMinus)) {
                            var20 = var21;
                        }
                    }

                    if (var20 < 0) {
                        var20 = var17++;
                        this.allRenderLists[var20].setupRenderList(var24.posXMinus, var24.posYMinus, var24.posZMinus, var23, var13, var15);
                    }

                    this.allRenderLists[var20].addGLRenderList(var24.getGLCallListForPass(par3));
                }

                if (Config.isFogOff() && this.mc.entityRenderer.fogStandard) {
                    GL11.glDisable(2912);
                }

                var18 = MathHelper.floor_double(var23);
                int var241 = MathHelper.floor_double(var15);
                var20 = var18 - (var18 & 1023);
                var21 = var241 - (var241 & 1023);
                Arrays.sort(this.allRenderLists, new RenderDistanceSorter(var20, var21));
                this.renderAllRenderLists(par3, par4);
                return var6;
            }
        }
    }

    public int renderSortedRenderersFast(int startIndex, int endIndex, int renderPass, double partialTicks) {
        this.glListBuffer.clear();
        int l = 0;
        boolean debug = this.mc.gameSettings.showDebugInfo;
        int loopStart = startIndex;
        int loopEnd = endIndex;
        byte loopInc = 1;
        if (renderPass == 1) {
            loopStart = this.countSortedWorldRenderers - 1 - startIndex;
            loopEnd = this.countSortedWorldRenderers - 1 - endIndex;
            loopInc = -1;
        }

        for (int entityliving = loopStart; entityliving != loopEnd; entityliving += loopInc) {
            WorldRenderer wr = this.sortedWorldRenderers[entityliving];
            if (debug && renderPass == 0) {
                ++this.renderersLoaded;
                if (wr.skipRenderPass[renderPass]) {
                    ++this.renderersSkippingRenderPass;
                } else if (!wr.isInFrustum) {
                    ++this.renderersBeingClipped;
                } else if (this.occlusionEnabled && !wr.isVisible) {
                    ++this.renderersBeingOccluded;
                } else {
                    ++this.renderersBeingRendered;
                }
            }

            if (wr.isInFrustum && !wr.skipRenderPass[renderPass] && (!this.occlusionEnabled || wr.isVisible)) {
                int glCallList = wr.getGLCallListForPass(renderPass);
                if (glCallList >= 0) {
                    this.glListBuffer.put(glCallList);
                    ++l;
                }
            }
        }

        if (l == 0) {
            return 0;
        } else {
            if (Config.isFogOff() && this.mc.entityRenderer.fogStandard) {
                GL11.glDisable(2912);
            }

            this.glListBuffer.flip();
            EntityLivingBase var20 = this.mc.renderViewEntity;
            double partialX = var20.lastTickPosX + (var20.posX - var20.lastTickPosX) * partialTicks - (double) WorldRenderer.globalChunkOffsetX;
            double partialY = var20.lastTickPosY + (var20.posY - var20.lastTickPosY) * partialTicks;
            double partialZ = var20.lastTickPosZ + (var20.posZ - var20.lastTickPosZ) * partialTicks - (double) WorldRenderer.globalChunkOffsetZ;
            this.mc.entityRenderer.enableLightmap(partialTicks);
            GL11.glTranslatef((float) (-partialX), (float) (-partialY), (float) (-partialZ));
            GL11.glCallLists(this.glListBuffer);
            GL11.glTranslatef((float) partialX, (float) partialY, (float) partialZ);
            this.mc.entityRenderer.disableLightmap(partialTicks);
            return l;
        }
    }

    public void renderAllRenderLists(int par1, double par2) {
        this.mc.entityRenderer.enableLightmap(par2);

        for (int var4 = 0; var4 < this.allRenderLists.length; ++var4) {
            this.allRenderLists[var4].callLists();
        }

        this.mc.entityRenderer.disableLightmap(par2);
    }

    public void updateClouds() {
        ++this.cloudTickCounter;
        if (this.cloudTickCounter % 20 == 0) {
            Iterator var1 = this.damagedBlocks.values().iterator();

            while (var1.hasNext()) {
                DestroyBlockProgress var2 = (DestroyBlockProgress) var1.next();
                int var3 = var2.getCreationCloudUpdateTick();
                if (this.cloudTickCounter - var3 > 400) {
                    var1.remove();
                }
            }
        }

    }

    public void renderSky(float par1) {

            WorldProvider var2 = this.mc.theWorld.provider;
            IRenderHandler var3 = var2.getSkyRenderer();
            if (var3 != null) {
                var3.render(par1, this.theWorld, this.mc);
                return;
            }


        if (this.mc.theWorld.provider.dimensionId == 1) {
            if (!Config.isSkyEnabled()) {
                return;
            }

            GL11.glDisable(2912);
            GL11.glDisable(3008);
            GL11.glEnable(3042);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            RenderHelper.disableStandardItemLighting();
            GL11.glDepthMask(false);
            this.renderEngine.bindTexture(locationEndSkyPng);
            Tessellator var22 = Tessellator.instance;

            for (int var241 = 0; var241 < 6; ++var241) {
                GL11.glPushMatrix();
                if (var241 == 1) {
                    GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                }

                if (var241 == 2) {
                    GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                }

                if (var241 == 3) {
                    GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
                }

                if (var241 == 4) {
                    GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
                }

                if (var241 == 5) {
                    GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);
                }

                var22.startDrawingQuads();
                var22.setColorOpaque_I(2631720);
                var22.addVertexWithUV(-100.0D, -100.0D, -100.0D, 0.0D, 0.0D);
                var22.addVertexWithUV(-100.0D, -100.0D, 100.0D, 0.0D, 16.0D);
                var22.addVertexWithUV(100.0D, -100.0D, 100.0D, 16.0D, 16.0D);
                var22.addVertexWithUV(100.0D, -100.0D, -100.0D, 16.0D, 0.0D);
                var22.draw();
                GL11.glPopMatrix();
            }

            GL11.glDepthMask(true);
            GL11.glEnable(3553);
            GL11.glEnable(3008);
        } else if (this.mc.theWorld.provider.isSurfaceWorld()) {
            GL11.glDisable(3553);
            Vec3 var231 = this.theWorld.getSkyColor(this.mc.renderViewEntity, par1);
            var231 = CustomColorizer.getSkyColor(var231, this.mc.theWorld, this.mc.renderViewEntity.posX, this.mc.renderViewEntity.posY + 1.0D, this.mc.renderViewEntity.posZ);
            float var251 = (float) var231.xCoord;
            float var4 = (float) var231.yCoord;
            float var5 = (float) var231.zCoord;
            float var8;
            if (this.mc.gameSettings.anaglyph) {
                float var23 = (var251 * 30.0F + var4 * 59.0F + var5 * 11.0F) / 100.0F;
                float var24 = (var251 * 30.0F + var4 * 70.0F) / 100.0F;
                var8 = (var251 * 30.0F + var5 * 70.0F) / 100.0F;
                var251 = var23;
                var4 = var24;
                var5 = var8;
            }

            GL11.glColor3f(var251, var4, var5);
            Tessellator var26 = Tessellator.instance;
            GL11.glDepthMask(false);
            GL11.glEnable(2912);
            GL11.glColor3f(var251, var4, var5);
            if (Config.isSkyEnabled()) {
                GL11.glCallList(this.glSkyList);
            }

            GL11.glDisable(2912);
            GL11.glDisable(3008);
            GL11.glEnable(3042);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            RenderHelper.disableStandardItemLighting();
            float[] var27 = this.theWorld.provider.calcSunriseSunsetColors(this.theWorld.getCelestialAngle(par1), par1);
            float var9;
            float var10;
            float var11;
            float var12;
            float var20;
            int var30;
            float var16;
            float var17;
            if (var27 != null && Config.isSunMoonEnabled()) {
                GL11.glDisable(3553);
                GL11.glShadeModel(7425);
                GL11.glPushMatrix();
                GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(MathHelper.sin(this.theWorld.getCelestialAngleRadians(par1)) < 0.0F ? 180.0F : 0.0F, 0.0F, 0.0F, 1.0F);
                GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
                var8 = var27[0];
                var9 = var27[1];
                var10 = var27[2];
                if (this.mc.gameSettings.anaglyph) {
                    var11 = (var8 * 30.0F + var9 * 59.0F + var10 * 11.0F) / 100.0F;
                    var12 = (var8 * 30.0F + var9 * 70.0F) / 100.0F;
                    var20 = (var8 * 30.0F + var10 * 70.0F) / 100.0F;
                    var8 = var11;
                    var9 = var12;
                    var10 = var20;
                }

                var26.startDrawing(6);
                var26.setColorRGBA_F(var8, var9, var10, var27[3]);
                var26.addVertex(0.0D, 100.0D, 0.0D);
                byte var29 = 16;
                var26.setColorRGBA_F(var27[0], var27[1], var27[2], 0.0F);

                for (var30 = 0; var30 <= var29; ++var30) {
                    var20 = (float) var30 * 3.1415927F * 2.0F / (float) var29;
                    var16 = MathHelper.sin(var20);
                    var17 = MathHelper.cos(var20);
                    var26.addVertex((double) (var16 * 120.0F), (double) (var17 * 120.0F), (double) (-var17 * 40.0F * var27[3]));
                }

                var26.draw();
                GL11.glPopMatrix();
                GL11.glShadeModel(7424);
            }

            GL11.glEnable(3553);
            OpenGlHelper.glBlendFunc(770, 1, 1, 0);
            GL11.glPushMatrix();
            var8 = 1.0F - this.theWorld.getRainStrength(par1);
            var9 = 0.0F;
            var10 = 0.0F;
            var11 = 0.0F;
            GL11.glColor4f(1.0F, 1.0F, 1.0F, var8);
            GL11.glTranslatef(var9, var10, var11);
            GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
            CustomSky.renderSky(this.theWorld, this.renderEngine, this.theWorld.getCelestialAngle(par1), var8);
            GL11.glRotatef(this.theWorld.getCelestialAngle(par1) * 360.0F, 1.0F, 0.0F, 0.0F);
            if (Config.isSunMoonEnabled()) {
                var12 = 30.0F;
                this.renderEngine.bindTexture(locationSunPng);
                var26.startDrawingQuads();
                var26.addVertexWithUV((double) (-var12), 100.0D, (double) (-var12), 0.0D, 0.0D);
                var26.addVertexWithUV((double) var12, 100.0D, (double) (-var12), 1.0D, 0.0D);
                var26.addVertexWithUV((double) var12, 100.0D, (double) var12, 1.0D, 1.0D);
                var26.addVertexWithUV((double) (-var12), 100.0D, (double) var12, 0.0D, 1.0D);
                var26.draw();
                var12 = 20.0F;
                this.renderEngine.bindTexture(locationMoonPhasesPng);
                int var28 = this.theWorld.getMoonPhase();
                int var291 = var28 % 4;
                var30 = var28 / 4 % 2;
                var16 = (float) (var291 + 0) / 4.0F;
                var17 = (float) (var30 + 0) / 2.0F;
                float var18 = (float) (var291 + 1) / 4.0F;
                float var19 = (float) (var30 + 1) / 2.0F;
                var26.startDrawingQuads();
                var26.addVertexWithUV((double) (-var12), -100.0D, (double) var12, (double) var18, (double) var19);
                var26.addVertexWithUV((double) var12, -100.0D, (double) var12, (double) var16, (double) var19);
                var26.addVertexWithUV((double) var12, -100.0D, (double) (-var12), (double) var16, (double) var17);
                var26.addVertexWithUV((double) (-var12), -100.0D, (double) (-var12), (double) var18, (double) var17);
                var26.draw();
            }

            GL11.glDisable(3553);
            var20 = this.theWorld.getStarBrightness(par1) * var8;
            if (var20 > 0.0F && Config.isStarsEnabled() && !CustomSky.hasSkyLayers(this.theWorld)) {
                GL11.glColor4f(var20, var20, var20, var20);
                GL11.glCallList(this.starGLCallList);
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDisable(3042);
            GL11.glEnable(3008);
            GL11.glEnable(2912);
            GL11.glPopMatrix();
            GL11.glDisable(3553);
            GL11.glColor3f(0.0F, 0.0F, 0.0F);
            double var25 = this.mc.thePlayer.getPosition(par1).yCoord - this.theWorld.getHorizon();
            if (var25 < 0.0D) {
                GL11.glPushMatrix();
                GL11.glTranslatef(0.0F, 12.0F, 0.0F);
                GL11.glCallList(this.glSkyList2);
                GL11.glPopMatrix();
                var10 = 1.0F;
                var11 = -((float) (var25 + 65.0D));
                var12 = -var10;
                var26.startDrawingQuads();
                var26.setColorRGBA_I(0, 255);
                var26.addVertex((double) (-var10), (double) var11, (double) var10);
                var26.addVertex((double) var10, (double) var11, (double) var10);
                var26.addVertex((double) var10, (double) var12, (double) var10);
                var26.addVertex((double) (-var10), (double) var12, (double) var10);
                var26.addVertex((double) (-var10), (double) var12, (double) (-var10));
                var26.addVertex((double) var10, (double) var12, (double) (-var10));
                var26.addVertex((double) var10, (double) var11, (double) (-var10));
                var26.addVertex((double) (-var10), (double) var11, (double) (-var10));
                var26.addVertex((double) var10, (double) var12, (double) (-var10));
                var26.addVertex((double) var10, (double) var12, (double) var10);
                var26.addVertex((double) var10, (double) var11, (double) var10);
                var26.addVertex((double) var10, (double) var11, (double) (-var10));
                var26.addVertex((double) (-var10), (double) var11, (double) (-var10));
                var26.addVertex((double) (-var10), (double) var11, (double) var10);
                var26.addVertex((double) (-var10), (double) var12, (double) var10);
                var26.addVertex((double) (-var10), (double) var12, (double) (-var10));
                var26.addVertex((double) (-var10), (double) var12, (double) (-var10));
                var26.addVertex((double) (-var10), (double) var12, (double) var10);
                var26.addVertex((double) var10, (double) var12, (double) var10);
                var26.addVertex((double) var10, (double) var12, (double) (-var10));
                var26.draw();
            }

            if (this.theWorld.provider.isSkyColored()) {
                GL11.glColor3f(var251 * 0.2F + 0.04F, var4 * 0.2F + 0.04F, var5 * 0.6F + 0.1F);
            } else {
                GL11.glColor3f(var251, var4, var5);
            }

            if (this.mc.gameSettings.renderDistanceChunks <= 4) {
                GL11.glColor3f(this.mc.entityRenderer.fogColorRed, this.mc.entityRenderer.fogColorGreen, this.mc.entityRenderer.fogColorBlue);
            }

            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, -((float) (var25 - 16.0D)), 0.0F);
            if (Config.isSkyEnabled()) {
                GL11.glCallList(this.glSkyList2);
            }

            GL11.glPopMatrix();
            GL11.glEnable(3553);
            GL11.glDepthMask(true);
        }

    }

    public void renderClouds(float par1) {
        if (!Config.isCloudsOff()) {

                WorldProvider worldProvider = this.mc.theWorld.provider;
            IRenderHandler var2 = worldProvider.getCloudRenderer();
                if (var2 != null) {
                    var2.render(par1, this.theWorld, this.mc);
                    return;
                }


            if (this.mc.theWorld.provider.isSurfaceWorld()) {
                if (Config.isCloudsFancy()) {
                    this.renderCloudsFancy(par1);
                } else {
                    float partialTicks1 = par1;
                    par1 = 0.0F;
                    GL11.glDisable(2884);
                    float var21 = (float) (this.mc.renderViewEntity.lastTickPosY + (this.mc.renderViewEntity.posY - this.mc.renderViewEntity.lastTickPosY) * (double) par1);
                    byte var3 = 32;
                    int var4 = 256 / var3;
                    Tessellator var5 = Tessellator.instance;
                    this.renderEngine.bindTexture(locationCloudsPng);
                    GL11.glEnable(3042);
                    OpenGlHelper.glBlendFunc(770, 771, 1, 0);
                    double exactPlayerZ;
                    double dc;
                    if (this.isFancyGlListClouds || this.cloudTickCounter >= this.cloudTickCounterGlList + 20) {
                        GL11.glNewList(this.glListClouds, 4864);
                        Vec3 entityliving = this.theWorld.getCloudColour(par1);
                        float var7 = (float) entityliving.xCoord;
                        float var8 = (float) entityliving.yCoord;
                        float var9 = (float) entityliving.zCoord;
                        float var10;
                        if (this.mc.gameSettings.anaglyph) {
                            var10 = (var7 * 30.0F + var8 * 59.0F + var9 * 11.0F) / 100.0F;
                            float var11 = (var7 * 30.0F + var8 * 70.0F) / 100.0F;
                            float var12 = (var7 * 30.0F + var9 * 70.0F) / 100.0F;
                            var7 = var10;
                            var8 = var11;
                            var9 = var12;
                        }

                        var10 = 4.8828125E-4F;
                        exactPlayerZ = (double) ((float) this.cloudTickCounter + par1);
                        dc = this.mc.renderViewEntity.prevPosX + (this.mc.renderViewEntity.posX - this.mc.renderViewEntity.prevPosX) * (double) par1 + exactPlayerZ * 0.029999999329447746D;
                        double var15 = this.mc.renderViewEntity.prevPosZ + (this.mc.renderViewEntity.posZ - this.mc.renderViewEntity.prevPosZ) * (double) par1;
                        int cdz = MathHelper.floor_double(dc / 2048.0D);
                        int var18 = MathHelper.floor_double(var15 / 2048.0D);
                        dc -= (double) (cdz * 2048);
                        var15 -= (double) (var18 * 2048);
                        float var19 = this.theWorld.provider.getCloudHeight() - var21 + 0.33F;
                        var19 += this.mc.gameSettings.ofCloudsHeight * 128.0F;
                        float var20 = (float) (dc * (double) var10);
                        var21 = (float) (var15 * (double) var10);  // maybe fix this.
                        var5.startDrawingQuads();
                        var5.setColorRGBA_F(var7, var8, var9, 0.8F);

                        for (int var22 = -var3 * var4; var22 < var3 * var4; var22 += var3) {
                            for (int var23 = -var3 * var4; var23 < var3 * var4; var23 += var3) {
                                var5.addVertexWithUV((double) (var22 + 0), (double) var19, (double) (var23 + var3), (double) ((float) (var22 + 0) * var10 + var20), (double) ((float) (var23 + var3) * var10 + var21));
                                var5.addVertexWithUV((double) (var22 + var3), (double) var19, (double) (var23 + var3), (double) ((float) (var22 + var3) * var10 + var20), (double) ((float) (var23 + var3) * var10 + var21));
                                var5.addVertexWithUV((double) (var22 + var3), (double) var19, (double) (var23 + 0), (double) ((float) (var22 + var3) * var10 + var20), (double) ((float) (var23 + 0) * var10 + var21));
                                var5.addVertexWithUV((double) (var22 + 0), (double) var19, (double) (var23 + 0), (double) ((float) (var22 + 0) * var10 + var20), (double) ((float) (var23 + 0) * var10 + var21));
                            }
                        }

                        var5.draw();
                        GL11.glEndList();
                        this.isFancyGlListClouds = false;
                        this.cloudTickCounterGlList = this.cloudTickCounter;
                        this.cloudPlayerX = this.mc.renderViewEntity.prevPosX;
                        this.cloudPlayerY = this.mc.renderViewEntity.prevPosY;
                        this.cloudPlayerZ = this.mc.renderViewEntity.prevPosZ;
                    }

                    EntityLivingBase entityliving1 = this.mc.renderViewEntity;
                    double exactPlayerX = entityliving1.prevPosX + (entityliving1.posX - entityliving1.prevPosX) * (double) partialTicks1;
                    double exactPlayerY = entityliving1.prevPosY + (entityliving1.posY - entityliving1.prevPosY) * (double) partialTicks1;
                    exactPlayerZ = entityliving1.prevPosZ + (entityliving1.posZ - entityliving1.prevPosZ) * (double) partialTicks1;
                    dc = (double) ((float) (this.cloudTickCounter - this.cloudTickCounterGlList) + partialTicks1);
                    float cdx = (float) (exactPlayerX - this.cloudPlayerX + dc * 0.03D);
                    float cdy = (float) (exactPlayerY - this.cloudPlayerY);
                    float cdz1 = (float) (exactPlayerZ - this.cloudPlayerZ);
                    GL11.glTranslatef(-cdx, -cdy, -cdz1);
                    GL11.glCallList(this.glListClouds);
                    GL11.glTranslatef(cdx, cdy, cdz1);
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    GL11.glDisable(3042);
                    GL11.glEnable(2884);
                }
            }

        }
    }

    public boolean hasCloudFog(double par1, double par3, double par5, float par7) {
        return false;
    }

    public void renderCloudsFancy(float par1) {
        float partialTicks = par1;
        par1 = 0.0F;
        GL11.glDisable(2884);
        float var2 = (float) (this.mc.renderViewEntity.lastTickPosY + (this.mc.renderViewEntity.posY - this.mc.renderViewEntity.lastTickPosY) * (double) par1);
        Tessellator var3 = Tessellator.instance;
        float var4 = 12.0F;
        float var5 = 4.0F;
        double var6 = (double) ((float) this.cloudTickCounter + par1);
        double var8 = (this.mc.renderViewEntity.prevPosX + (this.mc.renderViewEntity.posX - this.mc.renderViewEntity.prevPosX) * (double) par1 + var6 * 0.029999999329447746D) / (double) var4;
        double var10 = (this.mc.renderViewEntity.prevPosZ + (this.mc.renderViewEntity.posZ - this.mc.renderViewEntity.prevPosZ) * (double) par1) / (double) var4 + 0.33000001311302185D;
        float var12 = this.theWorld.provider.getCloudHeight() - var2 + 0.33F;
        var12 += this.mc.gameSettings.ofCloudsHeight * 128.0F;
        int var13 = MathHelper.floor_double(var8 / 2048.0D);
        int var14 = MathHelper.floor_double(var10 / 2048.0D);
        var8 -= (double) (var13 * 2048);
        var10 -= (double) (var14 * 2048);
        this.renderEngine.bindTexture(locationCloudsPng);
        GL11.glEnable(3042);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        float cdz;
        if (!this.isFancyGlListClouds || this.cloudTickCounter >= this.cloudTickCounterGlList + 20) {
            GL11.glNewList(this.glListClouds, 4864);
            Vec3 entityliving = this.theWorld.getCloudColour(par1);
            float var16 = (float) entityliving.xCoord;
            float var17 = (float) entityliving.yCoord;
            float var18 = (float) entityliving.zCoord;
            float var19;
            float var20;
            float var21;
            if (this.mc.gameSettings.anaglyph) {
                var19 = (var16 * 30.0F + var17 * 59.0F + var18 * 11.0F) / 100.0F;
                var20 = (var16 * 30.0F + var17 * 70.0F) / 100.0F;
                var21 = (var16 * 30.0F + var18 * 70.0F) / 100.0F;
                var16 = var19;
                var17 = var20;
                var18 = var21;
            }

            var19 = (float) (var8 * 0.0D);
            var20 = (float) (var10 * 0.0D);
            var21 = 0.00390625F;
            var19 = (float) MathHelper.floor_double(var8) * var21;
            var20 = (float) MathHelper.floor_double(var10) * var21;
            float var22 = (float) (var8 - (double) MathHelper.floor_double(var8));
            float var23 = (float) (var10 - (double) MathHelper.floor_double(var10));
            byte cdx = 8;
            byte cdy = 4;
            cdz = 9.765625E-4F;
            GL11.glScalef(var4, 1.0F, var4);

            for (int var27 = 0; var27 < 2; ++var27) {
                if (var27 == 0) {
                    GL11.glColorMask(false, false, false, false);
                } else if (this.mc.gameSettings.anaglyph) {
                    if (EntityRenderer.anaglyphField == 0) {
                        GL11.glColorMask(false, true, true, true);
                    } else {
                        GL11.glColorMask(true, false, false, true);
                    }
                } else {
                    GL11.glColorMask(true, true, true, true);
                }

                for (int var28 = -cdy + 1; var28 <= cdy; ++var28) {
                    for (int var29 = -cdy + 1; var29 <= cdy; ++var29) {
                        var3.startDrawingQuads();
                        float var30 = (float) (var28 * cdx);
                        float var31 = (float) (var29 * cdx);
                        float var32 = var30 - var22;
                        float var33 = var31 - var23;
                        if (var12 > -var5 - 1.0F) {
                            var3.setColorRGBA_F(var16 * 0.7F, var17 * 0.7F, var18 * 0.7F, 0.8F);
                            var3.setNormal(0.0F, -1.0F, 0.0F);
                            var3.addVertexWithUV((double) (var32 + 0.0F), (double) (var12 + 0.0F), (double) (var33 + (float) cdx), (double) ((var30 + 0.0F) * var21 + var19), (double) ((var31 + (float) cdx) * var21 + var20));
                            var3.addVertexWithUV((double) (var32 + (float) cdx), (double) (var12 + 0.0F), (double) (var33 + (float) cdx), (double) ((var30 + (float) cdx) * var21 + var19), (double) ((var31 + (float) cdx) * var21 + var20));
                            var3.addVertexWithUV((double) (var32 + (float) cdx), (double) (var12 + 0.0F), (double) (var33 + 0.0F), (double) ((var30 + (float) cdx) * var21 + var19), (double) ((var31 + 0.0F) * var21 + var20));
                            var3.addVertexWithUV((double) (var32 + 0.0F), (double) (var12 + 0.0F), (double) (var33 + 0.0F), (double) ((var30 + 0.0F) * var21 + var19), (double) ((var31 + 0.0F) * var21 + var20));
                        }

                        if (var12 <= var5 + 1.0F) {
                            var3.setColorRGBA_F(var16, var17, var18, 0.8F);
                            var3.setNormal(0.0F, 1.0F, 0.0F);
                            var3.addVertexWithUV((double) (var32 + 0.0F), (double) (var12 + var5 - cdz), (double) (var33 + (float) cdx), (double) ((var30 + 0.0F) * var21 + var19), (double) ((var31 + (float) cdx) * var21 + var20));
                            var3.addVertexWithUV((double) (var32 + (float) cdx), (double) (var12 + var5 - cdz), (double) (var33 + (float) cdx), (double) ((var30 + (float) cdx) * var21 + var19), (double) ((var31 + (float) cdx) * var21 + var20));
                            var3.addVertexWithUV((double) (var32 + (float) cdx), (double) (var12 + var5 - cdz), (double) (var33 + 0.0F), (double) ((var30 + (float) cdx) * var21 + var19), (double) ((var31 + 0.0F) * var21 + var20));
                            var3.addVertexWithUV((double) (var32 + 0.0F), (double) (var12 + var5 - cdz), (double) (var33 + 0.0F), (double) ((var30 + 0.0F) * var21 + var19), (double) ((var31 + 0.0F) * var21 + var20));
                        }

                        var3.setColorRGBA_F(var16 * 0.9F, var17 * 0.9F, var18 * 0.9F, 0.8F);
                        int var34;
                        if (var28 > -1) {
                            var3.setNormal(-1.0F, 0.0F, 0.0F);

                            for (var34 = 0; var34 < cdx; ++var34) {
                                var3.addVertexWithUV((double) (var32 + (float) var34 + 0.0F), (double) (var12 + 0.0F), (double) (var33 + (float) cdx), (double) ((var30 + (float) var34 + 0.5F) * var21 + var19), (double) ((var31 + (float) cdx) * var21 + var20));
                                var3.addVertexWithUV((double) (var32 + (float) var34 + 0.0F), (double) (var12 + var5), (double) (var33 + (float) cdx), (double) ((var30 + (float) var34 + 0.5F) * var21 + var19), (double) ((var31 + (float) cdx) * var21 + var20));
                                var3.addVertexWithUV((double) (var32 + (float) var34 + 0.0F), (double) (var12 + var5), (double) (var33 + 0.0F), (double) ((var30 + (float) var34 + 0.5F) * var21 + var19), (double) ((var31 + 0.0F) * var21 + var20));
                                var3.addVertexWithUV((double) (var32 + (float) var34 + 0.0F), (double) (var12 + 0.0F), (double) (var33 + 0.0F), (double) ((var30 + (float) var34 + 0.5F) * var21 + var19), (double) ((var31 + 0.0F) * var21 + var20));
                            }
                        }

                        if (var28 <= 1) {
                            var3.setNormal(1.0F, 0.0F, 0.0F);

                            for (var34 = 0; var34 < cdx; ++var34) {
                                var3.addVertexWithUV((double) (var32 + (float) var34 + 1.0F - cdz), (double) (var12 + 0.0F), (double) (var33 + (float) cdx), (double) ((var30 + (float) var34 + 0.5F) * var21 + var19), (double) ((var31 + (float) cdx) * var21 + var20));
                                var3.addVertexWithUV((double) (var32 + (float) var34 + 1.0F - cdz), (double) (var12 + var5), (double) (var33 + (float) cdx), (double) ((var30 + (float) var34 + 0.5F) * var21 + var19), (double) ((var31 + (float) cdx) * var21 + var20));
                                var3.addVertexWithUV((double) (var32 + (float) var34 + 1.0F - cdz), (double) (var12 + var5), (double) (var33 + 0.0F), (double) ((var30 + (float) var34 + 0.5F) * var21 + var19), (double) ((var31 + 0.0F) * var21 + var20));
                                var3.addVertexWithUV((double) (var32 + (float) var34 + 1.0F - cdz), (double) (var12 + 0.0F), (double) (var33 + 0.0F), (double) ((var30 + (float) var34 + 0.5F) * var21 + var19), (double) ((var31 + 0.0F) * var21 + var20));
                            }
                        }

                        var3.setColorRGBA_F(var16 * 0.8F, var17 * 0.8F, var18 * 0.8F, 0.8F);
                        if (var29 > -1) {
                            var3.setNormal(0.0F, 0.0F, -1.0F);

                            for (var34 = 0; var34 < cdx; ++var34) {
                                var3.addVertexWithUV((double) (var32 + 0.0F), (double) (var12 + var5), (double) (var33 + (float) var34 + 0.0F), (double) ((var30 + 0.0F) * var21 + var19), (double) ((var31 + (float) var34 + 0.5F) * var21 + var20));
                                var3.addVertexWithUV((double) (var32 + (float) cdx), (double) (var12 + var5), (double) (var33 + (float) var34 + 0.0F), (double) ((var30 + (float) cdx) * var21 + var19), (double) ((var31 + (float) var34 + 0.5F) * var21 + var20));
                                var3.addVertexWithUV((double) (var32 + (float) cdx), (double) (var12 + 0.0F), (double) (var33 + (float) var34 + 0.0F), (double) ((var30 + (float) cdx) * var21 + var19), (double) ((var31 + (float) var34 + 0.5F) * var21 + var20));
                                var3.addVertexWithUV((double) (var32 + 0.0F), (double) (var12 + 0.0F), (double) (var33 + (float) var34 + 0.0F), (double) ((var30 + 0.0F) * var21 + var19), (double) ((var31 + (float) var34 + 0.5F) * var21 + var20));
                            }
                        }

                        if (var29 <= 1) {
                            var3.setNormal(0.0F, 0.0F, 1.0F);

                            for (var34 = 0; var34 < cdx; ++var34) {
                                var3.addVertexWithUV((double) (var32 + 0.0F), (double) (var12 + var5), (double) (var33 + (float) var34 + 1.0F - cdz), (double) ((var30 + 0.0F) * var21 + var19), (double) ((var31 + (float) var34 + 0.5F) * var21 + var20));
                                var3.addVertexWithUV((double) (var32 + (float) cdx), (double) (var12 + var5), (double) (var33 + (float) var34 + 1.0F - cdz), (double) ((var30 + (float) cdx) * var21 + var19), (double) ((var31 + (float) var34 + 0.5F) * var21 + var20));
                                var3.addVertexWithUV((double) (var32 + (float) cdx), (double) (var12 + 0.0F), (double) (var33 + (float) var34 + 1.0F - cdz), (double) ((var30 + (float) cdx) * var21 + var19), (double) ((var31 + (float) var34 + 0.5F) * var21 + var20));
                                var3.addVertexWithUV((double) (var32 + 0.0F), (double) (var12 + 0.0F), (double) (var33 + (float) var34 + 1.0F - cdz), (double) ((var30 + 0.0F) * var21 + var19), (double) ((var31 + (float) var34 + 0.5F) * var21 + var20));
                            }
                        }

                        var3.draw();
                    }
                }
            }

            GL11.glEndList();
            this.isFancyGlListClouds = true;
            this.cloudTickCounterGlList = this.cloudTickCounter;
            this.cloudPlayerX = this.mc.renderViewEntity.prevPosX;
            this.cloudPlayerY = this.mc.renderViewEntity.prevPosY;
            this.cloudPlayerZ = this.mc.renderViewEntity.prevPosZ;
        }

        EntityLivingBase var44 = this.mc.renderViewEntity;
        double exactPlayerX = var44.prevPosX + (var44.posX - var44.prevPosX) * (double) partialTicks;
        double exactPlayerY = var44.prevPosY + (var44.posY - var44.prevPosY) * (double) partialTicks;
        double exactPlayerZ = var44.prevPosZ + (var44.posZ - var44.prevPosZ) * (double) partialTicks;
        double dc = (double) ((float) (this.cloudTickCounter - this.cloudTickCounterGlList) + partialTicks);
        float var45 = (float) (exactPlayerX - this.cloudPlayerX + dc * 0.03D);
        float var46 = (float) (exactPlayerY - this.cloudPlayerY);
        cdz = (float) (exactPlayerZ - this.cloudPlayerZ);
        GL11.glTranslatef(-var45, -var46, -cdz);
        GL11.glCallList(this.glListClouds);
        GL11.glTranslatef(var45, var46, cdz);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(3042);
        GL11.glEnable(2884);
    }

    public boolean updateRenderers(EntityLivingBase entityliving, boolean flag) {
        this.renderViewEntity = entityliving;
        if (WrUpdates.hasWrUpdater()) {
            return WrUpdates.updateRenderers(this, entityliving, flag);
        } else if (this.worldRenderersToUpdate.size() <= 0) {
            return false;
        } else {
            int num = 0;
            int maxNum = Config.getUpdatesPerFrame();
            if (Config.isDynamicUpdates() && !this.isMoving(entityliving)) {
                maxNum *= 3;
            }

            byte NOT_IN_FRUSTRUM_MUL = 4;
            int numValid = 0;
            WorldRenderer wrBest = null;
            float distSqBest = Float.MAX_VALUE;
            int indexBest = -1;

            for (int maxDiffDistSq = 0; maxDiffDistSq < this.worldRenderersToUpdate.size(); ++maxDiffDistSq) {
                WorldRenderer i = (WorldRenderer) this.worldRenderersToUpdate.get(maxDiffDistSq);
                if (i != null) {
                    ++numValid;
                    if (!i.needsUpdate) {
                        this.worldRenderersToUpdate.set(maxDiffDistSq, (Object) null);
                    } else {
                        float wr = i.distanceToEntitySquared(entityliving);
                        if (wr <= 256.0F && this.isActingNow()) {
                            i.updateRenderer(entityliving);
                            i.needsUpdate = false;
                            this.worldRenderersToUpdate.set(maxDiffDistSq, (Object) null);
                            ++num;
                        } else {
                            if (wr > 256.0F && num >= maxNum) {
                                break;
                            }

                            if (!i.isInFrustum) {
                                wr *= (float) NOT_IN_FRUSTRUM_MUL;
                            }

                            if (wrBest == null) {
                                wrBest = i;
                                distSqBest = wr;
                                indexBest = maxDiffDistSq;
                            } else if (wr < distSqBest) {
                                wrBest = i;
                                distSqBest = wr;
                                indexBest = maxDiffDistSq;
                            }
                        }
                    }
                }
            }

            if (wrBest != null) {
                wrBest.updateRenderer(entityliving);
                wrBest.needsUpdate = false;
                this.worldRenderersToUpdate.set(indexBest, (Object) null);
                ++num;
                float var15 = distSqBest / 5.0F;

                for (int var16 = 0; var16 < this.worldRenderersToUpdate.size() && num < maxNum; ++var16) {
                    WorldRenderer var17 = (WorldRenderer) this.worldRenderersToUpdate.get(var16);
                    if (var17 != null) {
                        float distSq = var17.distanceToEntitySquared(entityliving);
                        if (!var17.isInFrustum) {
                            distSq *= (float) NOT_IN_FRUSTRUM_MUL;
                        }

                        float diffDistSq = Math.abs(distSq - distSqBest);
                        if (diffDistSq < var15) {
                            var17.updateRenderer(entityliving);
                            var17.needsUpdate = false;
                            this.worldRenderersToUpdate.set(var16, (Object) null);
                            ++num;
                        }
                    }
                }
            }

            if (numValid == 0) {
                this.worldRenderersToUpdate.clear();
            }

            this.worldRenderersToUpdate.compact();
            return true;
        }
    }

    public void drawBlockDamageTexture(Tessellator par1Tessellator, EntityPlayer par2EntityPlayer, float par3) {
        this.drawBlockDamageTexture(par1Tessellator, (EntityLivingBase) par2EntityPlayer, par3);
    }

    public void drawBlockDamageTexture(Tessellator par1Tessellator, EntityLivingBase par2EntityPlayer, float par3) {
        double var4 = par2EntityPlayer.lastTickPosX + (par2EntityPlayer.posX - par2EntityPlayer.lastTickPosX) * (double) par3;
        double var6 = par2EntityPlayer.lastTickPosY + (par2EntityPlayer.posY - par2EntityPlayer.lastTickPosY) * (double) par3;
        double var8 = par2EntityPlayer.lastTickPosZ + (par2EntityPlayer.posZ - par2EntityPlayer.lastTickPosZ) * (double) par3;
        if (!this.damagedBlocks.isEmpty()) {
            OpenGlHelper.glBlendFunc(774, 768, 1, 0);
            this.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.5F);
            GL11.glPushMatrix();
            GL11.glPolygonOffset(-3.0F, -3.0F);
            GL11.glEnable('\u8037');
            GL11.glAlphaFunc(516, 0.1F);
            GL11.glEnable(3008);
            par1Tessellator.startDrawingQuads();
            par1Tessellator.setTranslation(-var4, -var6, -var8);
            par1Tessellator.disableColor();
            Iterator var10 = this.damagedBlocks.values().iterator();

            while (var10.hasNext()) {
                DestroyBlockProgress var11 = (DestroyBlockProgress) var10.next();
                double var12 = (double) var11.getPartialBlockX() - var4;
                double var14 = (double) var11.getPartialBlockY() - var6;
                double var16 = (double) var11.getPartialBlockZ() - var8;
                if (var12 * var12 + var14 * var14 + var16 * var16 > 1024.0D) {
                    var10.remove();
                } else {
                    Block var18 = this.theWorld.getBlock(var11.getPartialBlockX(), var11.getPartialBlockY(), var11.getPartialBlockZ());
                    if (var18.getMaterial() != Material.air) {
                        this.renderBlocksRg.renderBlockUsingTexture(var18, var11.getPartialBlockX(), var11.getPartialBlockY(), var11.getPartialBlockZ(), this.destroyBlockIcons[var11.getPartialBlockDamage()]);
                    }
                }
            }

            par1Tessellator.draw();
            par1Tessellator.setTranslation(0.0D, 0.0D, 0.0D);
            GL11.glDisable(3008);
            GL11.glPolygonOffset(0.0F, 0.0F);
            GL11.glDisable('\u8037');
            GL11.glEnable(3008);
            GL11.glDepthMask(true);
            GL11.glPopMatrix();
        }

    }

    public void drawSelectionBox(EntityPlayer par1EntityPlayer, MovingObjectPosition par2MovingObjectPosition, int par3, float par4) {
        if (par3 == 0 && par2MovingObjectPosition.typeOfHit == MovingObjectType.BLOCK) {
            GL11.glEnable(3042);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
            GL11.glLineWidth(2.0F);
            GL11.glDisable(3553);
            GL11.glDepthMask(false);
            float var5 = 0.002F;
            Block var6 = this.theWorld.getBlock(par2MovingObjectPosition.blockX, par2MovingObjectPosition.blockY, par2MovingObjectPosition.blockZ);
            if (var6.getMaterial() != Material.air) {
                var6.setBlockBoundsBasedOnState(this.theWorld, par2MovingObjectPosition.blockX, par2MovingObjectPosition.blockY, par2MovingObjectPosition.blockZ);
                double var7 = par1EntityPlayer.lastTickPosX + (par1EntityPlayer.posX - par1EntityPlayer.lastTickPosX) * (double) par4;
                double var9 = par1EntityPlayer.lastTickPosY + (par1EntityPlayer.posY - par1EntityPlayer.lastTickPosY) * (double) par4;
                double var11 = par1EntityPlayer.lastTickPosZ + (par1EntityPlayer.posZ - par1EntityPlayer.lastTickPosZ) * (double) par4;
                drawOutlinedBoundingBox(var6.getSelectedBoundingBoxFromPool(this.theWorld, par2MovingObjectPosition.blockX, par2MovingObjectPosition.blockY, par2MovingObjectPosition.blockZ).expand((double) var5, (double) var5, (double) var5).getOffsetBoundingBox(-var7, -var9, -var11), -1);
            }

            GL11.glDepthMask(true);
            GL11.glEnable(3553);
            GL11.glDisable(3042);
        }

    }

    public static void drawOutlinedBoundingBox(AxisAlignedBB p_147590_0_, int p_147590_1_) {
        Tessellator var2 = Tessellator.instance;
        var2.startDrawing(3);
        if (p_147590_1_ != -1) {
            var2.setColorOpaque_I(p_147590_1_);
        }

        var2.addVertex(p_147590_0_.minX, p_147590_0_.minY, p_147590_0_.minZ);
        var2.addVertex(p_147590_0_.maxX, p_147590_0_.minY, p_147590_0_.minZ);
        var2.addVertex(p_147590_0_.maxX, p_147590_0_.minY, p_147590_0_.maxZ);
        var2.addVertex(p_147590_0_.minX, p_147590_0_.minY, p_147590_0_.maxZ);
        var2.addVertex(p_147590_0_.minX, p_147590_0_.minY, p_147590_0_.minZ);
        var2.draw();
        var2.startDrawing(3);
        if (p_147590_1_ != -1) {
            var2.setColorOpaque_I(p_147590_1_);
        }

        var2.addVertex(p_147590_0_.minX, p_147590_0_.maxY, p_147590_0_.minZ);
        var2.addVertex(p_147590_0_.maxX, p_147590_0_.maxY, p_147590_0_.minZ);
        var2.addVertex(p_147590_0_.maxX, p_147590_0_.maxY, p_147590_0_.maxZ);
        var2.addVertex(p_147590_0_.minX, p_147590_0_.maxY, p_147590_0_.maxZ);
        var2.addVertex(p_147590_0_.minX, p_147590_0_.maxY, p_147590_0_.minZ);
        var2.draw();
        var2.startDrawing(1);
        if (p_147590_1_ != -1) {
            var2.setColorOpaque_I(p_147590_1_);
        }

        var2.addVertex(p_147590_0_.minX, p_147590_0_.minY, p_147590_0_.minZ);
        var2.addVertex(p_147590_0_.minX, p_147590_0_.maxY, p_147590_0_.minZ);
        var2.addVertex(p_147590_0_.maxX, p_147590_0_.minY, p_147590_0_.minZ);
        var2.addVertex(p_147590_0_.maxX, p_147590_0_.maxY, p_147590_0_.minZ);
        var2.addVertex(p_147590_0_.maxX, p_147590_0_.minY, p_147590_0_.maxZ);
        var2.addVertex(p_147590_0_.maxX, p_147590_0_.maxY, p_147590_0_.maxZ);
        var2.addVertex(p_147590_0_.minX, p_147590_0_.minY, p_147590_0_.maxZ);
        var2.addVertex(p_147590_0_.minX, p_147590_0_.maxY, p_147590_0_.maxZ);
        var2.draw();
    }

    public void markBlocksForUpdate(int par1, int par2, int par3, int par4, int par5, int par6) {
        int var7 = MathHelper.bucketInt(par1, 16);
        int var8 = MathHelper.bucketInt(par2, 16);
        int var9 = MathHelper.bucketInt(par3, 16);
        int var10 = MathHelper.bucketInt(par4, 16);
        int var11 = MathHelper.bucketInt(par5, 16);
        int var12 = MathHelper.bucketInt(par6, 16);

        for (int var13 = var7; var13 <= var10; ++var13) {
            int var14 = var13 % this.renderChunksWide;
            if (var14 < 0) {
                var14 += this.renderChunksWide;
            }

            for (int var15 = var8; var15 <= var11; ++var15) {
                int var16 = var15 % this.renderChunksTall;
                if (var16 < 0) {
                    var16 += this.renderChunksTall;
                }

                for (int var17 = var9; var17 <= var12; ++var17) {
                    int var18 = var17 % this.renderChunksDeep;
                    if (var18 < 0) {
                        var18 += this.renderChunksDeep;
                    }

                    int var19 = (var18 * this.renderChunksTall + var16) * this.renderChunksWide + var14;
                    WorldRenderer var20 = this.worldRenderers[var19];
                    if (var20 != null && !var20.needsUpdate) {
                        this.worldRenderersToUpdate.add(var20);
                        var20.markDirty();
                    }
                }
            }
        }

    }

    public void markBlockForUpdate(int p_147586_1_, int p_147586_2_, int p_147586_3_) {
        this.markBlocksForUpdate(p_147586_1_ - 1, p_147586_2_ - 1, p_147586_3_ - 1, p_147586_1_ + 1, p_147586_2_ + 1, p_147586_3_ + 1);
    }

    public void markBlockForRenderUpdate(int p_147588_1_, int p_147588_2_, int p_147588_3_) {
        this.markBlocksForUpdate(p_147588_1_ - 1, p_147588_2_ - 1, p_147588_3_ - 1, p_147588_1_ + 1, p_147588_2_ + 1, p_147588_3_ + 1);
    }

    public void markBlockRangeForRenderUpdate(int p_147585_1_, int p_147585_2_, int p_147585_3_, int p_147585_4_, int p_147585_5_, int p_147585_6_) {
        this.markBlocksForUpdate(p_147585_1_ - 1, p_147585_2_ - 1, p_147585_3_ - 1, p_147585_4_ + 1, p_147585_5_ + 1, p_147585_6_ + 1);
    }

    public void clipRenderersByFrustum(ICamera par1ICamera, float par2) {
        boolean checkDistanceXz = !Config.isFogOff();
        double renderDistSq = (double) (this.renderDistanceChunks * 16 * this.renderDistanceChunks * 16);

        for (int var3 = 0; var3 < this.countSortedWorldRenderers; ++var3) {
            WorldRenderer wr = this.sortedWorldRenderers[var3];
            if (!wr.skipAllRenderPasses()) {
                if (checkDistanceXz && wr.distanceToEntityXzSq > renderDistSq) {
                    wr.isInFrustum = false;
                } else {
                    wr.updateInFrustum(par1ICamera);
                }
            }
        }

    }

    public void playRecord(String par1Str, int par2, int par3, int par4) {
        ChunkCoordinates var5 = new ChunkCoordinates(par2, par3, par4);
        ISound var6 = (ISound) this.mapSoundPositions.get(var5);
        if (var6 != null) {
            this.mc.getSoundHandler().stopSound(var6);
            this.mapSoundPositions.remove(var5);
        }

        if (par1Str != null) {
            ItemRecord var7 = ItemRecord.getRecord(par1Str);
            if (var7 != null) {
                this.mc.ingameGUI.setRecordPlayingMessage(var7.getRecordNameLocal());
            }

            ResourceLocation resource = null;
            if (var7 != null) {
                resource = var7.getRecordResource(par1Str);
            }

            if (resource == null) {
                resource = new ResourceLocation(par1Str);
            }

            PositionedSoundRecord var8 = PositionedSoundRecord.func_147675_a(resource, (float) par2, (float) par3, (float) par4);
            this.mapSoundPositions.put(var5, var8);
            this.mc.getSoundHandler().playSound(var8);
        }

    }

    public void playSound(String par1Str, double par2, double par4, double par6, float par8, float par9) {
    }

    public void playSoundToNearExcept(EntityPlayer par1EntityPlayer, String par2Str, double par3, double par5, double par7, float par9, float par10) {
    }

    public void spawnParticle(String par1Str, final double par2, final double par4, final double par6, double par8, double par10, double par12) {
        try {
            this.doSpawnParticle(par1Str, par2, par4, par6, par8, par10, par12);
        } catch (Throwable var17) {
            CrashReport var15 = CrashReport.makeCrashReport(var17, "Exception while adding particle");
            CrashReportCategory var16 = var15.makeCategory("Particle being added");
            var16.addCrashSection("Name", par1Str);
            var16.addCrashSectionCallable("Position", new Callable() {

                public static final String __OBFID = "CL_00000955";

                public String call() {
                    return CrashReportCategory.func_85074_a(par2, par4, par6);
                }
            });
            throw new ReportedException(var15);
        }
    }

    public EntityFX doSpawnParticle(String par1Str, double par2, double par4, double par6, double par8, double par10, double par12) {
        if (this.mc != null && this.mc.renderViewEntity != null && this.mc.effectRenderer != null) {
            int var14 = this.mc.gameSettings.particleSetting;
            if (var14 == 1 && this.theWorld.rand.nextInt(3) == 0) {
                var14 = 2;
            }

            double var15 = this.mc.renderViewEntity.posX - par2;
            double var17 = this.mc.renderViewEntity.posY - par4;
            double var19 = this.mc.renderViewEntity.posZ - par6;
            Object var21 = null;
            if (par1Str.equals("hugeexplosion")) {
                if (Config.isAnimatedExplosion()) {
                    this.mc.effectRenderer.addEffect((EntityFX) (var21 = new EntityHugeExplodeFX(this.theWorld, par2, par4, par6, par8, par10, par12)));
                }
            } else if (par1Str.equals("largeexplode")) {
                if (Config.isAnimatedExplosion()) {
                    this.mc.effectRenderer.addEffect((EntityFX) (var21 = new EntityLargeExplodeFX(this.renderEngine, this.theWorld, par2, par4, par6, par8, par10, par12)));
                }
            } else if (par1Str.equals("fireworksSpark")) {
                this.mc.effectRenderer.addEffect((EntityFX) (var21 = new EntityFireworkSparkFX(this.theWorld, par2, par4, par6, par8, par10, par12, this.mc.effectRenderer)));
            }

            if (var21 != null) {
                return (EntityFX) var21;
            } else {
                double var22 = 16.0D;
                double d3 = 16.0D;
                if (par1Str.equals("crit")) {
                    var22 = 196.0D;
                }

                if (var15 * var15 + var17 * var17 + var19 * var19 > var22 * var22) {
                    return null;
                } else if (var14 > 1) {
                    return null;
                } else {
                    if (par1Str.equals("bubble")) {
                        var21 = new EntityBubbleFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                        CustomColorizer.updateWaterFX((EntityFX) var21, this.theWorld);
                    } else if (par1Str.equals("suspended")) {
                        if (Config.isWaterParticles()) {
                            var21 = new EntitySuspendFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                        }
                    } else if (par1Str.equals("depthsuspend")) {
                        if (Config.isVoidParticles()) {
                            var21 = new EntityAuraFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                        }
                    } else if (par1Str.equals("townaura")) {
                        var21 = new EntityAuraFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                        CustomColorizer.updateMyceliumFX((EntityFX) var21);
                    } else if (par1Str.equals("crit")) {
                        var21 = new EntityCritFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                    } else if (par1Str.equals("magicCrit")) {
                        var21 = new EntityCritFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                        ((EntityFX) var21).setRBGColorF(((EntityFX) var21).getRedColorF() * 0.3F, ((EntityFX) var21).getGreenColorF() * 0.8F, ((EntityFX) var21).getBlueColorF());
                        ((EntityFX) var21).nextTextureIndexX();
                    } else if (par1Str.equals("smoke")) {
                        if (Config.isAnimatedSmoke()) {
                            var21 = new EntitySmokeFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                        }
                    } else if (par1Str.equals("mobSpell")) {
                        if (Config.isPotionParticles()) {
                            var21 = new EntitySpellParticleFX(this.theWorld, par2, par4, par6, 0.0D, 0.0D, 0.0D);
                            ((EntityFX) var21).setRBGColorF((float) par8, (float) par10, (float) par12);
                        }
                    } else if (par1Str.equals("mobSpellAmbient")) {
                        if (Config.isPotionParticles()) {
                            var21 = new EntitySpellParticleFX(this.theWorld, par2, par4, par6, 0.0D, 0.0D, 0.0D);
                            ((EntityFX) var21).setAlphaF(0.15F);
                            ((EntityFX) var21).setRBGColorF((float) par8, (float) par10, (float) par12);
                        }
                    } else if (par1Str.equals("spell")) {
                        if (Config.isPotionParticles()) {
                            var21 = new EntitySpellParticleFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                        }
                    } else if (par1Str.equals("instantSpell")) {
                        if (Config.isPotionParticles()) {
                            var21 = new EntitySpellParticleFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                            ((EntitySpellParticleFX) var21).setBaseSpellTextureIndex(144);
                        }
                    } else if (par1Str.equals("witchMagic")) {
                        if (Config.isPotionParticles()) {
                            var21 = new EntitySpellParticleFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                            ((EntitySpellParticleFX) var21).setBaseSpellTextureIndex(144);
                            float var26 = this.theWorld.rand.nextFloat() * 0.5F + 0.35F;
                            ((EntityFX) var21).setRBGColorF(1.0F * var26, 0.0F * var26, 1.0F * var26);
                        }
                    } else if (par1Str.equals("note")) {
                        var21 = new EntityNoteFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                    } else if (par1Str.equals("portal")) {
                        if (Config.isPortalParticles()) {
                            var21 = new EntityPortalFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                            CustomColorizer.updatePortalFX((EntityFX) var21);
                        }
                    } else if (par1Str.equals("enchantmenttable")) {
                        var21 = new EntityEnchantmentTableParticleFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                    } else if (par1Str.equals("explode")) {
                        if (Config.isAnimatedExplosion()) {
                            var21 = new EntityExplodeFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                        }
                    } else if (par1Str.equals("flame")) {
                        if (Config.isAnimatedFlame()) {
                            var21 = new EntityFlameFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                        }
                    } else if (par1Str.equals("lava")) {
                        var21 = new EntityLavaFX(this.theWorld, par2, par4, par6);
                    } else if (par1Str.equals("footstep")) {
                        var21 = new EntityFootStepFX(this.renderEngine, this.theWorld, par2, par4, par6);
                    } else if (par1Str.equals("splash")) {
                        var21 = new EntitySplashFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                        CustomColorizer.updateWaterFX((EntityFX) var21, this.theWorld);
                    } else if (par1Str.equals("wake")) {
                        var21 = new EntityFishWakeFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                    } else if (par1Str.equals("largesmoke")) {
                        if (Config.isAnimatedSmoke()) {
                            var21 = new EntitySmokeFX(this.theWorld, par2, par4, par6, par8, par10, par12, 2.5F);
                        }
                    } else if (par1Str.equals("cloud")) {
                        var21 = new EntityCloudFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                    } else if (par1Str.equals("reddust")) {
                        if (Config.isAnimatedRedstone()) {
                            var21 = new EntityReddustFX(this.theWorld, par2, par4, par6, (float) par8, (float) par10, (float) par12);
                            CustomColorizer.updateReddustFX((EntityFX) var21, this.theWorld, var15, var17, var19);
                        }
                    } else if (par1Str.equals("snowballpoof")) {
                        var21 = new EntityBreakingFX(this.theWorld, par2, par4, par6, Items.snowball);
                    } else if (par1Str.equals("dripWater")) {
                        if (Config.isDrippingWaterLava()) {
                            var21 = new EntityDropParticleFX(this.theWorld, par2, par4, par6, Material.water);
                        }
                    } else if (par1Str.equals("dripLava")) {
                        if (Config.isDrippingWaterLava()) {
                            var21 = new EntityDropParticleFX(this.theWorld, par2, par4, par6, Material.lava);
                        }
                    } else if (par1Str.equals("snowshovel")) {
                        var21 = new EntitySnowShovelFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                    } else if (par1Str.equals("slime")) {
                        var21 = new EntityBreakingFX(this.theWorld, par2, par4, par6, Items.slime_ball);
                    } else if (par1Str.equals("heart")) {
                        var21 = new EntityHeartFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                    } else if (par1Str.equals("angryVillager")) {
                        var21 = new EntityHeartFX(this.theWorld, par2, par4 + 0.5D, par6, par8, par10, par12);
                        ((EntityFX) var21).setParticleTextureIndex(81);
                        ((EntityFX) var21).setRBGColorF(1.0F, 1.0F, 1.0F);
                    } else if (par1Str.equals("happyVillager")) {
                        var21 = new EntityAuraFX(this.theWorld, par2, par4, par6, par8, par10, par12);
                        ((EntityFX) var21).setParticleTextureIndex(82);
                        ((EntityFX) var21).setRBGColorF(1.0F, 1.0F, 1.0F);
                    } else {
                        String[] var27;
                        int var261;
                        if (par1Str.startsWith("iconcrack_")) {
                            var27 = par1Str.split("_", 3);
                            int var28 = Integer.parseInt(var27[1]);
                            if (var27.length > 2) {
                                var261 = Integer.parseInt(var27[2]);
                                var21 = new EntityBreakingFX(this.theWorld, par2, par4, par6, par8, par10, par12, Item.getItemById(var28), var261);
                            } else {
                                var21 = new EntityBreakingFX(this.theWorld, par2, par4, par6, par8, par10, par12, Item.getItemById(var28), 0);
                            }
                        } else {
                            Block var281;
                            if (par1Str.startsWith("blockcrack_")) {
                                var27 = par1Str.split("_", 3);
                                var281 = Block.getBlockById(Integer.parseInt(var27[1]));
                                var261 = Integer.parseInt(var27[2]);
                                var21 = (new EntityDiggingFX(this.theWorld, par2, par4, par6, par8, par10, par12, var281, var261)).applyRenderColor(var261);
                            } else if (par1Str.startsWith("blockdust_")) {
                                var27 = par1Str.split("_", 3);
                                var281 = Block.getBlockById(Integer.parseInt(var27[1]));
                                var261 = Integer.parseInt(var27[2]);
                                var21 = (new EntityBlockDustFX(this.theWorld, par2, par4, par6, par8, par10, par12, var281, var261)).applyRenderColor(var261);
                            }
                        }
                    }

                    if (var21 != null) {
                        this.mc.effectRenderer.addEffect((EntityFX) var21);
                    }

                    return (EntityFX) var21;
                }
            }
        } else {
            return null;
        }
    }

    public void onEntityCreate(Entity par1Entity) {
        RandomMobs.entityLoaded(par1Entity, this.theWorld);
        if (Config.isDynamicLights()) {
            DynamicLights.entityAdded(par1Entity, this);
        }

    }

    public void onEntityDestroy(Entity par1Entity) {
        if (Config.isDynamicLights()) {
            DynamicLights.entityRemoved(par1Entity, this);
        }

    }

    public void deleteAllDisplayLists() {
        GLAllocation.deleteDisplayLists(this.glRenderListBase);
        this.displayListAllocator.deleteDisplayLists();
    }

    public void broadcastSound(int par1, int par2, int par3, int par4, int par5) {
        Random var6 = this.theWorld.rand;
        switch (par1) {
            case 1013:
            case 1018:
                if (this.mc.renderViewEntity != null) {
                    double var7 = (double) par2 - this.mc.renderViewEntity.posX;
                    double var9 = (double) par3 - this.mc.renderViewEntity.posY;
                    double var11 = (double) par4 - this.mc.renderViewEntity.posZ;
                    double var13 = Math.sqrt(var7 * var7 + var9 * var9 + var11 * var11);
                    double var15 = this.mc.renderViewEntity.posX;
                    double var17 = this.mc.renderViewEntity.posY;
                    double var19 = this.mc.renderViewEntity.posZ;
                    if (var13 > 0.0D) {
                        var15 += var7 / var13 * 2.0D;
                        var17 += var9 / var13 * 2.0D;
                        var19 += var11 / var13 * 2.0D;
                    }

                    if (par1 == 1013) {
                        this.theWorld.playSound(var15, var17, var19, "mob.wither.spawn", 1.0F, 1.0F, false);
                    } else if (par1 == 1018) {
                        this.theWorld.playSound(var15, var17, var19, "mob.enderdragon.end", 5.0F, 1.0F, false);
                    }
                }
            default:
        }
    }

    public void playAuxSFX(EntityPlayer par1EntityPlayer, int par2, int par3, int par4, int par5, int par6) {
        Random var7 = this.theWorld.rand;
        Block var8 = null;
        double var11;
        double var13;
        double var22;
        double var41;
        double var26;
        double var28;
        double var30;
        double var9;
        String var15;
        int var16;
        int var40;
        double var21;
        switch (par2) {
            case 1000:
                this.theWorld.playSound((double) par3, (double) par4, (double) par5, "random.click", 1.0F, 1.0F, false);
                break;
            case 1001:
                this.theWorld.playSound((double) par3, (double) par4, (double) par5, "random.click", 1.0F, 1.2F, false);
                break;
            case 1002:
                this.theWorld.playSound((double) par3, (double) par4, (double) par5, "random.bow", 1.0F, 1.2F, false);
                break;
            case 1003:
                if (Math.random() < 0.5D) {
                    this.theWorld.playSound((double) par3 + 0.5D, (double) par4 + 0.5D, (double) par5 + 0.5D, "random.door_open", 1.0F, this.theWorld.rand.nextFloat() * 0.1F + 0.9F, false);
                } else {
                    this.theWorld.playSound((double) par3 + 0.5D, (double) par4 + 0.5D, (double) par5 + 0.5D, "random.door_close", 1.0F, this.theWorld.rand.nextFloat() * 0.1F + 0.9F, false);
                }
                break;
            case 1004:
                this.theWorld.playSound((double) ((float) par3 + 0.5F), (double) ((float) par4 + 0.5F), (double) ((float) par5 + 0.5F), "random.fizz", 0.5F, 2.6F + (var7.nextFloat() - var7.nextFloat()) * 0.8F, false);
                break;
            case 1005:
                if (Item.getItemById(par6) instanceof ItemRecord) {
                    this.theWorld.playRecord("records." + ((ItemRecord) Item.getItemById(par6)).recordName, par3, par4, par5);
                } else {
                    this.theWorld.playRecord((String) null, par3, par4, par5);
                }
                break;
            case 1007:
                this.theWorld.playSound((double) par3 + 0.5D, (double) par4 + 0.5D, (double) par5 + 0.5D, "mob.ghast.charge", 10.0F, (var7.nextFloat() - var7.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1008:
                this.theWorld.playSound((double) par3 + 0.5D, (double) par4 + 0.5D, (double) par5 + 0.5D, "mob.ghast.fireball", 10.0F, (var7.nextFloat() - var7.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1009:
                this.theWorld.playSound((double) par3 + 0.5D, (double) par4 + 0.5D, (double) par5 + 0.5D, "mob.ghast.fireball", 2.0F, (var7.nextFloat() - var7.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1010:
                this.theWorld.playSound((double) par3 + 0.5D, (double) par4 + 0.5D, (double) par5 + 0.5D, "mob.zombie.wood", 2.0F, (var7.nextFloat() - var7.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1011:
                this.theWorld.playSound((double) par3 + 0.5D, (double) par4 + 0.5D, (double) par5 + 0.5D, "mob.zombie.metal", 2.0F, (var7.nextFloat() - var7.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1012:
                this.theWorld.playSound((double) par3 + 0.5D, (double) par4 + 0.5D, (double) par5 + 0.5D, "mob.zombie.woodbreak", 2.0F, (var7.nextFloat() - var7.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1014:
                this.theWorld.playSound((double) par3 + 0.5D, (double) par4 + 0.5D, (double) par5 + 0.5D, "mob.wither.shoot", 2.0F, (var7.nextFloat() - var7.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1015:
                this.theWorld.playSound((double) par3 + 0.5D, (double) par4 + 0.5D, (double) par5 + 0.5D, "mob.bat.takeoff", 0.05F, (var7.nextFloat() - var7.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1016:
                this.theWorld.playSound((double) par3 + 0.5D, (double) par4 + 0.5D, (double) par5 + 0.5D, "mob.zombie.infect", 2.0F, (var7.nextFloat() - var7.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1017:
                this.theWorld.playSound((double) par3 + 0.5D, (double) par4 + 0.5D, (double) par5 + 0.5D, "mob.zombie.unfect", 2.0F, (var7.nextFloat() - var7.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1020:
                this.theWorld.playSound((double) ((float) par3 + 0.5F), (double) ((float) par4 + 0.5F), (double) ((float) par5 + 0.5F), "random.anvil_break", 1.0F, this.theWorld.rand.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1021:
                this.theWorld.playSound((double) ((float) par3 + 0.5F), (double) ((float) par4 + 0.5F), (double) ((float) par5 + 0.5F), "random.anvil_use", 1.0F, this.theWorld.rand.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1022:
                this.theWorld.playSound((double) ((float) par3 + 0.5F), (double) ((float) par4 + 0.5F), (double) ((float) par5 + 0.5F), "random.anvil_land", 0.3F, this.theWorld.rand.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 2000:
                int var34 = par6 % 3 - 1;
                int var10 = par6 / 3 % 3 - 1;
                var11 = (double) par3 + (double) var34 * 0.6D + 0.5D;
                var13 = (double) par4 + 0.5D;
                double var35 = (double) par5 + (double) var10 * 0.6D + 0.5D;

                for (int var49 = 0; var49 < 10; ++var49) {
                    double var38 = var7.nextDouble() * 0.2D + 0.01D;
                    double var39 = var11 + (double) var34 * 0.01D + (var7.nextDouble() - 0.5D) * (double) var10 * 0.5D;
                    var22 = var13 + (var7.nextDouble() - 0.5D) * 0.5D;
                    var41 = var35 + (double) var10 * 0.01D + (var7.nextDouble() - 0.5D) * (double) var34 * 0.5D;
                    var26 = (double) var34 * var38 + var7.nextGaussian() * 0.01D;
                    var28 = -0.03D + var7.nextGaussian() * 0.01D;
                    var30 = (double) var10 * var38 + var7.nextGaussian() * 0.01D;
                    this.spawnParticle("smoke", var39, var22, var41, var26, var28, var30);
                }

                return;
            case 2001:
                var8 = Block.getBlockById(par6 & 4095);
                if (var8.getMaterial() != Material.air) {
                    this.mc.getSoundHandler().playSound(new PositionedSoundRecord(new ResourceLocation(var8.stepSound.getBreakSound()), (var8.stepSound.getVolume() + 1.0F) / 2.0F, var8.stepSound.getPitch() * 0.8F, (float) par3 + 0.5F, (float) par4 + 0.5F, (float) par5 + 0.5F));
                }

                this.mc.effectRenderer.addBlockDestroyEffects(par3, par4, par5, var8, par6 >> 12 & 255);
                break;
            case 2002:
                var9 = (double) par3;
                var11 = (double) par4;
                var13 = (double) par5;
                var15 = "iconcrack_" + Item.getIdFromItem(Items.potionitem) + "_" + par6;

                for (var16 = 0; var16 < 8; ++var16) {
                    this.spawnParticle(var15, var9, var11, var13, var7.nextGaussian() * 0.15D, var7.nextDouble() * 0.2D, var7.nextGaussian() * 0.15D);
                }

                var16 = Items.potionitem.getColorFromDamage(par6);
                float var17 = (float) (var16 >> 16 & 255) / 255.0F;
                float var18 = (float) (var16 >> 8 & 255) / 255.0F;
                float var19 = (float) (var16 >> 0 & 255) / 255.0F;
                String var20 = "spell";
                if (Items.potionitem.isEffectInstant(par6)) {
                    var20 = "instantSpell";
                }

                for (var40 = 0; var40 < 100; ++var40) {
                    var22 = var7.nextDouble() * 4.0D;
                    var41 = var7.nextDouble() * 3.141592653589793D * 2.0D;
                    var26 = Math.cos(var41) * var22;
                    var28 = 0.01D + var7.nextDouble() * 0.5D;
                    var30 = Math.sin(var41) * var22;
                    EntityFX var42 = this.doSpawnParticle(var20, var9 + var26 * 0.1D, var11 + 0.3D, var13 + var30 * 0.1D, var26, var28, var30);
                    if (var42 != null) {
                        float var33 = 0.75F + var7.nextFloat() * 0.25F;
                        var42.setRBGColorF(var17 * var33, var18 * var33, var19 * var33);
                        var42.multiplyVelocity((float) var22);
                    }
                }

                this.theWorld.playSound((double) par3 + 0.5D, (double) par4 + 0.5D, (double) par5 + 0.5D, "game.potion.smash", 1.0F, this.theWorld.rand.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 2003:
                var9 = (double) par3 + 0.5D;
                var11 = (double) par4;
                var13 = (double) par5 + 0.5D;
                var15 = "iconcrack_" + Item.getIdFromItem(Items.ender_eye);

                for (var16 = 0; var16 < 8; ++var16) {
                    this.spawnParticle(var15, var9, var11, var13, var7.nextGaussian() * 0.15D, var7.nextDouble() * 0.2D, var7.nextGaussian() * 0.15D);
                }

                for (var21 = 0.0D; var21 < 6.283185307179586D; var21 += 0.15707963267948966D) {
                    this.spawnParticle("portal", var9 + Math.cos(var21) * 5.0D, var11 - 0.4D, var13 + Math.sin(var21) * 5.0D, Math.cos(var21) * -5.0D, 0.0D, Math.sin(var21) * -5.0D);
                    this.spawnParticle("portal", var9 + Math.cos(var21) * 5.0D, var11 - 0.4D, var13 + Math.sin(var21) * 5.0D, Math.cos(var21) * -7.0D, 0.0D, Math.sin(var21) * -7.0D);
                }

                return;
            case 2004:
                for (var40 = 0; var40 < 20; ++var40) {
                    var22 = (double) par3 + 0.5D + ((double) this.theWorld.rand.nextFloat() - 0.5D) * 2.0D;
                    var41 = (double) par4 + 0.5D + ((double) this.theWorld.rand.nextFloat() - 0.5D) * 2.0D;
                    var26 = (double) par5 + 0.5D + ((double) this.theWorld.rand.nextFloat() - 0.5D) * 2.0D;
                    this.theWorld.spawnParticle("smoke", var22, var41, var26, 0.0D, 0.0D, 0.0D);
                    this.theWorld.spawnParticle("flame", var22, var41, var26, 0.0D, 0.0D, 0.0D);
                }

                return;
            case 2005:
                ItemDye.func_150918_a(this.theWorld, par3, par4, par5, par6);
                break;
            case 2006:
                var8 = this.theWorld.getBlock(par3, par4, par5);
                if (var8.getMaterial() != Material.air) {
                    var21 = (double) Math.min(0.2F + (float) par6 / 15.0F, 10.0F);
                    if (var21 > 2.5D) {
                        var21 = 2.5D;
                    }

                    int var23 = (int) (150.0D * var21);

                    for (int var24 = 0; var24 < var23; ++var24) {
                        float var25 = MathHelper.randomFloatClamp(var7, 0.0F, 6.2831855F);
                        var26 = (double) MathHelper.randomFloatClamp(var7, 0.75F, 1.0F);
                        var28 = 0.20000000298023224D + var21 / 100.0D;
                        var30 = (double) (MathHelper.cos(var25) * 0.2F) * var26 * var26 * (var21 + 0.2D);
                        double var32 = (double) (MathHelper.sin(var25) * 0.2F) * var26 * var26 * (var21 + 0.2D);
                        this.theWorld.spawnParticle("blockdust_" + Block.getIdFromBlock(var8) + "_" + this.theWorld.getBlockMetadata(par3, par4, par5), (double) ((float) par3 + 0.5F), (double) ((float) par4 + 1.0F), (double) ((float) par5 + 0.5F), var30, var28, var32);
                    }
                }
        }

    }

    public void destroyBlockPartially(int p_147587_1_, int p_147587_2_, int p_147587_3_, int p_147587_4_, int p_147587_5_) {
        if (p_147587_5_ >= 0 && p_147587_5_ < 10) {
            DestroyBlockProgress var6 = (DestroyBlockProgress) this.damagedBlocks.get(Integer.valueOf(p_147587_1_));
            if (var6 == null || var6.getPartialBlockX() != p_147587_2_ || var6.getPartialBlockY() != p_147587_3_ || var6.getPartialBlockZ() != p_147587_4_) {
                var6 = new DestroyBlockProgress(p_147587_1_, p_147587_2_, p_147587_3_, p_147587_4_);
                this.damagedBlocks.put(Integer.valueOf(p_147587_1_), var6);
            }

            var6.setPartialBlockDamage(p_147587_5_);
            var6.setCloudUpdateTick(this.cloudTickCounter);
        } else {
            this.damagedBlocks.remove(Integer.valueOf(p_147587_1_));
        }

    }

    public void registerDestroyBlockIcons(IIconRegister par1IconRegister) {
        this.destroyBlockIcons = new IIcon[10];

        for (int var2 = 0; var2 < this.destroyBlockIcons.length; ++var2) {
            this.destroyBlockIcons[var2] = par1IconRegister.registerIcon("destroy_stage_" + var2);
        }

    }

    public void setAllRenderersVisible() {
        if (this.worldRenderers != null) {
            for (int i = 0; i < this.worldRenderers.length; ++i) {
                this.worldRenderers[i].isVisible = true;
            }

        }
    }

    public boolean isMoving(EntityLivingBase entityliving) {
        boolean moving = this.isMovingNow(entityliving);
        if (moving) {
            this.lastMovedTime = System.currentTimeMillis();
            return true;
        } else {
            return System.currentTimeMillis() - this.lastMovedTime < 2000L;
        }
    }

    public boolean isMovingNow(EntityLivingBase entityliving) {
        double maxDiff = 0.001D;
        return entityliving.isSneaking() ? true : ((double) entityliving.prevSwingProgress > maxDiff ? true : (this.mc.mouseHelper.deltaX != 0 ? true : (this.mc.mouseHelper.deltaY != 0 ? true : (Math.abs(entityliving.posX - entityliving.prevPosX) > maxDiff ? true : (Math.abs(entityliving.posY - entityliving.prevPosY) > maxDiff ? true : Math.abs(entityliving.posZ - entityliving.prevPosZ) > maxDiff)))));
    }

    public boolean isActing() {
        boolean acting = this.isActingNow();
        if (acting) {
            this.lastActionTime = System.currentTimeMillis();
            return true;
        } else {
            return System.currentTimeMillis() - this.lastActionTime < 500L;
        }
    }

    public boolean isActingNow() {
        return Mouse.isButtonDown(0) ? true : Mouse.isButtonDown(1);
    }

    public int renderAllSortedRenderers(int renderPass, double partialTicks) {
        return this.renderSortedRenderers(0, this.countSortedWorldRenderers, renderPass, partialTicks);
    }

    public void updateCapes() {
    }

    public AxisAlignedBB getTileEntityBoundingBox(TileEntity te) {
        if (!te.hasWorldObj()) {
            return AABB_INFINITE;
        } else {
            Block blockType = te.getBlockType();
            if (blockType == Blocks.enchanting_table) {
                return AxisAlignedBB.getBoundingBox((double) te.xCoord, (double) te.yCoord, (double) te.zCoord, (double) (te.xCoord + 1), (double) (te.yCoord + 1), (double) (te.zCoord + 1));
            } else if (blockType != Blocks.chest && blockType != Blocks.trapped_chest) {
                AxisAlignedBB blockAabb = te.getRenderBoundingBox();
                    if (blockAabb != null) {
                        return blockAabb;
                    }


                if (blockType != null && blockType != Blocks.beacon) {
                    blockAabb = blockType.getCollisionBoundingBoxFromPool(te.getWorldObj(), te.xCoord, te.yCoord, te.zCoord);
                    if (blockAabb != null) {
                        return blockAabb;
                    }
                }

                return AABB_INFINITE;
            } else {
                return AxisAlignedBB.getBoundingBox((double) (te.xCoord - 1), (double) te.yCoord, (double) (te.zCoord - 1), (double) (te.xCoord + 2), (double) (te.yCoord + 2), (double) (te.zCoord + 2));
            }
        }
    }

    public void addToSortedWorldRenderers(WorldRenderer wr) {
        if (!wr.inSortedWorldRenderers) {
            int pos = this.countSortedWorldRenderers;
            wr.updateDistanceToEntitySquared(this.renderViewEntity);
            float distSq = wr.sortDistanceToEntitySquared;
            int countGreater;
            if (this.countSortedWorldRenderers > 0) {
                countGreater = 0;
                int high = this.countSortedWorldRenderers - 1;
                int mid = (countGreater + high) / 2;

                while (countGreater <= high) {
                    mid = (countGreater + high) / 2;
                    WorldRenderer wrMid = this.sortedWorldRenderers[mid];
                    if (distSq < wrMid.sortDistanceToEntitySquared) {
                        high = mid - 1;
                    } else {
                        countGreater = mid + 1;
                    }
                }

                if (countGreater > mid) {
                    pos = mid + 1;
                } else {
                    pos = mid;
                }
            }

            countGreater = this.countSortedWorldRenderers - pos;
            if (countGreater > 0) {
                System.arraycopy(this.sortedWorldRenderers, pos, this.sortedWorldRenderers, pos + 1, countGreater);
            }

            this.sortedWorldRenderers[pos] = wr;
            wr.inSortedWorldRenderers = true;
            ++this.countSortedWorldRenderers;
        }
    }

    public int getCountRenderers() {
        return this.renderersLoaded;
    }

    public int getCountActiveRenderers() {
        return this.renderersBeingRendered;
    }

    public int getCountEntitiesRendered() {
        return this.countEntitiesRendered;
    }

    public int getCountTileEntitiesRendered() {
        return this.countTileEntitiesRendered;
    }

}
