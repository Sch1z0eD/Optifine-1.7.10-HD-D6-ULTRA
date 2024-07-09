package patch;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import cpw.mods.fml.client.FMLClientHandler;
import net.minecraft.block.Block;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import optifine.OptiFineClassTransformer;

public class ReflectorForge {

   public static void FMLClientHandler_trackBrokenTexture(ResourceLocation loc, String message) {

      FMLClientHandler instance = FMLClientHandler.instance();
        // Reflector.call(instance, Reflector.FMLClientHandler_trackBrokenTexture, new Object[]{loc, message});
      instance.trackBrokenTexture(loc, message);

   }

   public static void FMLClientHandler_trackMissingTexture(ResourceLocation loc) {

      FMLClientHandler instance = FMLClientHandler.instance();
         //Reflector.call(instance, Reflector.FMLClientHandler_trackMissingTexture, new Object[]{loc});
      instance.trackMissingTexture(loc);
   }

   public static void putLaunchBlackboard(String key, Object value) {
      Map blackboard = Launch.blackboard;

      if(blackboard != null) {
         blackboard.put(key, value);
      }
   }

   public static InputStream getOptiFineResourceStream(String path) {

      OptiFineClassTransformer instance = OptiFineClassTransformer.instance;
         if(instance == null) {
            return null;
         } else {
            if(path.startsWith("/")) {
               path = path.substring(1);
            }

            byte[] bytes = instance.getOptiFineResource(path);
            if(bytes == null) {
               return null;
            } else {
               ByteArrayInputStream in = new ByteArrayInputStream(bytes);
               return in;
            }
         }

   }

   public static boolean blockHasTileEntity(World world, int x, int y, int z) {
      Block block = world.getBlock(x, y, z);

         int metadata = world.getBlockMetadata(x, y, z);
         return block.hasTileEntity(metadata);
      }

}
