package patch;

public class IntegerCache {

   public static final int CACHE_SIZE = 65535;
   public static final Integer[] cache = makeCache('\uffff');


   public static Integer[] makeCache(int size) {
      Integer[] arr = new Integer[size];

      for(int i = 0; i < size; ++i) {
         arr[i] = new Integer(i);
      }

      return arr;
   }

   public static Integer valueOf(int value) {
      return value >= 0 && value < '\uffff'?cache[value]:new Integer(value);
   }

}
