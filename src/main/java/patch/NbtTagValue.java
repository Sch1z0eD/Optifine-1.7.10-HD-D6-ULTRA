package patch;

public class NbtTagValue {

   public String tag = null;
   public String value = null;


   public NbtTagValue(String tag, String value) {
      this.tag = tag;
      this.value = value;
   }

   public boolean matches(String key, String value) {
      return false;
   }
}
