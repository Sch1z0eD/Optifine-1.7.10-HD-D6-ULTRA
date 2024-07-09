package patch;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;

public class EntityUtils {

   public static ReflectorClass ForgeEntityLivingBase = new ReflectorClass(EntityLivingBase.class);
   public static ReflectorField ForgeEntityLivingBase_entityAge = new ReflectorField(ForgeEntityLivingBase, "field_70708_bq");
   public static boolean directEntityAge = true;
   public static ReflectorClass ForgeEntityLiving = new ReflectorClass(EntityLiving.class);
   public static ReflectorMethod ForgeEntityLiving_despawnEntity = new ReflectorMethod(ForgeEntityLiving, "func_70623_bb");
   public static boolean directDespawnEntity = true;


   public static int getEntityAge(EntityLivingBase elb) {
      if(directEntityAge) {
         try {
            return elb.entityAge;
         } catch (IllegalAccessError var2) {
            directEntityAge = false;
            if(!ForgeEntityLivingBase_entityAge.exists()) {
               throw var2;
            }
         }
      }

      return ((Integer)Reflector.getFieldValue(elb, ForgeEntityLivingBase_entityAge)).intValue();
   }

   public static void setEntityAge(EntityLivingBase elb, int age) {
      if(directEntityAge) {
         try {
            elb.entityAge = age;
            return;
         } catch (IllegalAccessError var3) {
            directEntityAge = false;
            if(!ForgeEntityLivingBase_entityAge.exists()) {
               throw var3;
            }
         }
      }

      Reflector.setFieldValue(elb, ForgeEntityLivingBase_entityAge, Integer.valueOf(age));
   }

   public static void despawnEntity(EntityLiving el) {
      if(directDespawnEntity) {
         try {
            el.despawnEntity();
            return;
         } catch (IllegalAccessError var2) {
            directDespawnEntity = false;
            if(!ForgeEntityLiving_despawnEntity.exists()) {
               throw var2;
            }
         }
      }

      Reflector.callVoid(el, ForgeEntityLiving_despawnEntity, new Object[0]);
   }

}
