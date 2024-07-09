package patch;

import java.lang.reflect.Field;

public class ReflectorField {

   public ReflectorClass reflectorClass = null;
   public String targetFieldName = null;
   public boolean checked = false;
   public Field targetField = null;


   public ReflectorField(ReflectorClass reflectorClass, String targetFieldName) {
      this.reflectorClass = reflectorClass;
      this.targetFieldName = targetFieldName;
      Field f = this.getTargetField();
   }

   public Field getTargetField() {
      if(this.checked) {
         return this.targetField;
      } else {
         this.checked = true;
         Class cls = this.reflectorClass.getTargetClass();
         if(cls == null) {
            return null;
         } else {
            try {
               this.targetField = cls.getDeclaredField(this.targetFieldName);
               this.targetField.setAccessible(true);
            } catch (NoSuchFieldException var3) {
               Config.log("(patch.Reflector) Field not present: " + cls.getName() + "." + this.targetFieldName);
            } catch (SecurityException var4) {
               var4.printStackTrace();
            } catch (Throwable var5) {
               var5.printStackTrace();
            }

            return this.targetField;
         }
      }
   }

   public Object getValue() {
      return Reflector.getFieldValue((Object)null, this);
   }

   public void setValue(Object value) {
      Reflector.setFieldValue((Object)null, this, value);
   }

   public boolean exists() {
      return this.checked?this.targetField != null:this.getTargetField() != null;
   }
}
