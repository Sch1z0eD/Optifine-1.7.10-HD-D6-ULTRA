package patch;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

public class RenderPlayerOF extends RenderPlayer {

   public void renderEquippedItems(EntityLivingBase entityLiving, float partialTicks) {
      super.renderEquippedItems(entityLiving, partialTicks);
      this.renderEquippedItems(entityLiving, 0.0625F, partialTicks);
   }

   public void renderEquippedItems(EntityLivingBase entityLiving, float scale, float partialTicks) {
      if(Config.isShowCapes()) {
         if(entityLiving instanceof AbstractClientPlayer) {
            AbstractClientPlayer player = (AbstractClientPlayer)entityLiving;
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDisable('\u803a');
            ModelBiped modelBipedMain = (ModelBiped)this.mainModel;
            PlayerConfigurations.renderPlayerItems(modelBipedMain, player, scale, partialTicks);
         }
      }
   }

   public static void register() {
      RenderManager rm = RenderManager.instance;
      Map mapRenderTypes = getMapRenderTypes(rm);
      if(mapRenderTypes == null) {
         Config.warn("patch.RenderPlayerOF init() failed: RenderManager.MapRenderTypes not found");
      } else {
         RenderPlayerOF rpof = new RenderPlayerOF();
         rpof.setRenderManager(rm);
         mapRenderTypes.put(EntityPlayer.class, rpof);
      }
   }

   public static Map getMapRenderTypes(RenderManager rm) {

         return rm.entityRenderMap;

   }
}
