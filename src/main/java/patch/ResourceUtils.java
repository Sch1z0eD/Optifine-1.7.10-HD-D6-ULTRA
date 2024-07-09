package patch;

import java.io.File;
import net.minecraft.client.resources.AbstractResourcePack;

public class ResourceUtils {

   public static ReflectorClass ForgeAbstractResourcePack = new ReflectorClass(AbstractResourcePack.class);
   public static ReflectorField ForgeAbstractResourcePack_resourcePackFile = new ReflectorField(ForgeAbstractResourcePack, "field_110597_b");
   public static boolean directAccessValid = true;


   public static File getResourcePackFile(AbstractResourcePack arp) {
      if(directAccessValid) {
         try {
            return arp.resourcePackFile;
         } catch (IllegalAccessError var2) {
            directAccessValid = false;
            if(!ForgeAbstractResourcePack_resourcePackFile.exists()) {
               throw var2;
            }
         }
      }

      return (File)Reflector.getFieldValue(arp, ForgeAbstractResourcePack_resourcePackFile);
   }

}
