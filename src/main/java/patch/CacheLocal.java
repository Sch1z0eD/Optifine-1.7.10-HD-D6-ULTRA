package patch;

public class CacheLocal {

   public int maxX = 18;
   public int maxY = 128;
   public int maxZ = 18;
   public int offsetX = 0;
   public int offsetY = 0;
   public int offsetZ = 0;
   public int[][][] cache = (int[][][])null;
   public int[] lastZs = null;
   public int lastDz = 0;


   public CacheLocal(int maxX, int maxY, int maxZ) {
      this.maxX = maxX;
      this.maxY = maxY;
      this.maxZ = maxZ;
      this.cache = new int[maxX][maxY][maxZ];
      this.resetCache();
   }

   public void resetCache() {
      for(int x = 0; x < this.maxX; ++x) {
         int[][] ys = this.cache[x];

         for(int y = 0; y < this.maxY; ++y) {
            int[] zs = ys[y];

            for(int z = 0; z < this.maxZ; ++z) {
               zs[z] = -1;
            }
         }
      }

   }

   public void setOffset(int x, int y, int z) {
      this.offsetX = x;
      this.offsetY = y;
      this.offsetZ = z;
      this.resetCache();
   }

   public int get(int x, int y, int z) {
      try {
         this.lastZs = this.cache[x - this.offsetX][y - this.offsetY];
         this.lastDz = z - this.offsetZ;
         return this.lastZs[this.lastDz];
      } catch (ArrayIndexOutOfBoundsException var5) {
         var5.printStackTrace();
         return -1;
      }
   }

   public void setLast(int val) {
      try {
         this.lastZs[this.lastDz] = val;
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }
}
