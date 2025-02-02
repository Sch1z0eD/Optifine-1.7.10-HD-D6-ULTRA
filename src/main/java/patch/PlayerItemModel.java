package patch;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class PlayerItemModel {

   public Dimension textureSize = null;
   public boolean usePlayerTexture = false;
   public PlayerItemRenderer[] modelRenderers = new PlayerItemRenderer[0];
   public ResourceLocation textureLocation = null;
   public BufferedImage textureImage = null;
   public DynamicTexture texture = null;
   public ResourceLocation locationMissing = new ResourceLocation("textures/blocks/wool_colored_red.png");
   public static final int ATTACH_BODY = 0;
   public static final int ATTACH_HEAD = 1;
   public static final int ATTACH_LEFT_ARM = 2;
   public static final int ATTACH_RIGHT_ARM = 3;
   public static final int ATTACH_LEFT_LEG = 4;
   public static final int ATTACH_RIGHT_LEG = 5;
   public static final int ATTACH_CAPE = 6;


   public PlayerItemModel(Dimension textureSize, boolean usePlayerTexture, PlayerItemRenderer[] modelRenderers) {
      this.textureSize = textureSize;
      this.usePlayerTexture = usePlayerTexture;
      this.modelRenderers = modelRenderers;
   }

   public void render(ModelBiped modelBiped, AbstractClientPlayer player, float scale, float partialTicks) {
      TextureManager textureManager = Config.getTextureManager();
      if(this.usePlayerTexture) {
         textureManager.bindTexture(player.getLocationSkin());
      } else if(this.textureLocation != null) {
         if(this.texture == null && this.textureImage != null) {
            this.texture = new DynamicTexture(this.textureImage);
            Minecraft.getMinecraft().getTextureManager().loadTexture(this.textureLocation, this.texture);
         }

         textureManager.bindTexture(this.textureLocation);
      } else {
         textureManager.bindTexture(this.locationMissing);
      }

      for(int i = 0; i < this.modelRenderers.length; ++i) {
         PlayerItemRenderer pir = this.modelRenderers[i];
         GL11.glPushMatrix();
         if(player.isSneaking()) {
            GL11.glTranslatef(0.0F, 0.2F, 0.0F);
         }

         pir.render(modelBiped, scale);
         GL11.glPopMatrix();
      }

   }

   public static ModelRenderer getAttachModel(ModelBiped modelBiped, int attachTo) {
      switch(attachTo) {
      case 0:
         return modelBiped.bipedBody;
      case 1:
         return modelBiped.bipedHead;
      case 2:
         return modelBiped.bipedLeftArm;
      case 3:
         return modelBiped.bipedRightArm;
      case 4:
         return modelBiped.bipedLeftLeg;
      case 5:
         return modelBiped.bipedRightLeg;
      default:
         return null;
      }
   }

   public BufferedImage getTextureImage() {
      return this.textureImage;
   }

   public void setTextureImage(BufferedImage textureImage) {
      this.textureImage = textureImage;
   }

   public DynamicTexture getTexture() {
      return this.texture;
   }

   public ResourceLocation getTextureLocation() {
      return this.textureLocation;
   }

   public void setTextureLocation(ResourceLocation textureLocation) {
      this.textureLocation = textureLocation;
   }

   public boolean isUsePlayerTexture() {
      return this.usePlayerTexture;
   }
}
