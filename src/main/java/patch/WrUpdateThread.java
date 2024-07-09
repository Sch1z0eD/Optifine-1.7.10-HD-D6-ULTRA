package patch;

import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import org.lwjgl.opengl.Pbuffer;

public class WrUpdateThread extends Thread {

   public Pbuffer pbuffer = null;
   public Object lock = new Object();
   public List updateList = new LinkedList();
   public List updatedList = new LinkedList();
   public int updateCount = 0;
   public Tessellator mainTessellator;
   public Tessellator threadTessellator;
   public boolean working;
   public WorldRendererThreaded currentRenderer;
   public boolean canWork;
   public boolean canWorkToEndOfUpdate;
   public boolean terminated;
   public static final int MAX_UPDATE_CAPACITY = 10;


   public WrUpdateThread(Pbuffer pbuffer) {
      super("patch.WrUpdateThread");
      this.mainTessellator = Tessellator.instance;
      this.threadTessellator = new Tessellator(2097152);
      this.working = false;
      this.currentRenderer = null;
      this.canWork = false;
      this.canWorkToEndOfUpdate = false;
      this.terminated = false;
      this.pbuffer = pbuffer;
   }

   public void run() {
      try {
         this.pbuffer.makeCurrent();
      } catch (Exception var8) {
         var8.printStackTrace();
      }

      ThreadUpdateListener updateListener = new ThreadUpdateListener(null);

      while(!Thread.interrupted() && !this.terminated) {
         try {
            WorldRendererThreaded e = this.getRendererToUpdate();
            if(e == null) {
               return;
            }

            this.checkCanWork((IWrUpdateControl)null);

            try {
               this.currentRenderer = e;
               Tessellator.instance = this.threadTessellator;
               e.updateRenderer((IWrUpdateListener) updateListener);
            } finally {
               Tessellator.instance = this.mainTessellator;
            }

            this.rendererUpdated(e);
         } catch (Exception var9) {
            var9.printStackTrace();
            if(this.currentRenderer != null) {
               this.currentRenderer.isUpdating = false;
               this.currentRenderer.needsUpdate = true;
            }

            this.currentRenderer = null;
            this.working = false;
         }
      }

   }

   public void addRendererToUpdate(WorldRenderer wr, boolean first) {
      Object var3 = this.lock;
      synchronized(this.lock) {
         if(wr.isUpdating) {
            throw new IllegalArgumentException("Renderer already updating");
         } else {
            if(first) {
               this.updateList.add(0, wr);
            } else {
               this.updateList.add(wr);
            }

            wr.isUpdating = true;
            this.lock.notifyAll();
         }
      }
   }

   public WorldRendererThreaded getRendererToUpdate() {
      Object var1 = this.lock;
      synchronized(this.lock) {
         while(this.updateList.size() <= 0) {
            try {
               this.lock.wait(2000L);
               if(this.terminated) {
                  Object var10000 = null;
                  return (WorldRendererThreaded)var10000;
               }
            } catch (InterruptedException var4) {
               ;
            }
         }

         WorldRendererThreaded wrt = (WorldRendererThreaded)this.updateList.remove(0);
         this.lock.notifyAll();
         return wrt;
      }
   }

   public boolean hasWorkToDo() {
      Object var1 = this.lock;
      synchronized(this.lock) {
         return this.updateList.size() > 0?true:(this.currentRenderer != null?true:this.working);
      }
   }

   public int getUpdateCapacity() {
      Object var1 = this.lock;
      synchronized(this.lock) {
         return this.updateList.size() > 10?0:10 - this.updateList.size();
      }
   }

   public void rendererUpdated(WorldRenderer wr) {
      Object var2 = this.lock;
      synchronized(this.lock) {
         this.updatedList.add(wr);
         ++this.updateCount;
         this.currentRenderer = null;
         this.working = false;
         this.lock.notifyAll();
      }
   }

   public void finishUpdatedRenderers() {
      Object var1 = this.lock;
      synchronized(this.lock) {
         for(int i = 0; i < this.updatedList.size(); ++i) {
            WorldRendererThreaded wr = (WorldRendererThreaded)this.updatedList.get(i);
            wr.finishUpdate();
            wr.isUpdating = false;
         }

         this.updatedList.clear();
      }
   }

   public void pause() {
      Object var1 = this.lock;
      synchronized(this.lock) {
         this.canWork = false;
         this.canWorkToEndOfUpdate = false;
         this.lock.notifyAll();

         while(this.working) {
            try {
               this.lock.wait();
            } catch (InterruptedException var4) {
               ;
            }
         }

         this.finishUpdatedRenderers();
      }
   }

   public void unpause() {
      Object var1 = this.lock;
      synchronized(this.lock) {
         if(this.working) {
            Config.warn("UpdateThread still working in unpause()!!!");
         }

         this.canWork = true;
         this.canWorkToEndOfUpdate = false;
         this.lock.notifyAll();
      }
   }

   public void unpauseToEndOfUpdate() {
      Object var1 = this.lock;
      synchronized(this.lock) {
         if(this.working) {
            Config.warn("UpdateThread still working in unpause()!!!");
         }

         if(this.currentRenderer != null) {
            while(this.currentRenderer != null) {
               this.canWork = false;
               this.canWorkToEndOfUpdate = true;
               this.lock.notifyAll();

               try {
                  this.lock.wait();
               } catch (InterruptedException var4) {
                  ;
               }
            }

            this.pause();
         }
      }
   }

   public void checkCanWork(IWrUpdateControl uc) {
      Thread.yield();
      Object var2 = this.lock;
      synchronized(this.lock) {
         while(!this.canWork && (!this.canWorkToEndOfUpdate || this.currentRenderer == null)) {
            if(uc != null) {
               uc.pause();
            }

            this.working = false;
            this.lock.notifyAll();

            try {
               this.lock.wait();
            } catch (InterruptedException var5) {
               ;
            }
         }

         this.working = true;
         if(uc != null) {
            uc.resume();
         }

         this.lock.notifyAll();
      }
   }

   public void clearAllUpdates() {
      Object var1 = this.lock;
      synchronized(this.lock) {
         this.unpauseToEndOfUpdate();

         for(int i = 0; i < this.updateList.size(); ++i) {
            WorldRenderer wr = (WorldRenderer)this.updateList.get(i);
            wr.needsUpdate = true;
            wr.isUpdating = false;
         }

         this.updateList.clear();
         this.lock.notifyAll();
      }
   }

   public int getPendingUpdatesCount() {
      Object var1 = this.lock;
      synchronized(this.lock) {
         int count = this.updateList.size();
         if(this.currentRenderer != null) {
            ++count;
         }

         return count;
      }
   }

   public int resetUpdateCount() {
      Object var1 = this.lock;
      synchronized(this.lock) {
         int count = this.updateCount;
         this.updateCount = 0;
         return count;
      }
   }

   public void terminate() {
      this.terminated = true;
   }

   // $FF: synthetic class
   public static class NamelessClass1974951930 {
   }

   public class ThreadUpdateControl implements IWrUpdateControl {

      public IWrUpdateControl updateControl;
      public boolean paused;


      public ThreadUpdateControl() {
         this.updateControl = null;
         this.paused = false;
      }

      public void pause() {
         if(!this.paused) {
            this.paused = true;
            this.updateControl.pause();
            Tessellator.instance = WrUpdateThread.this.mainTessellator;
         }

      }

      public void resume() {
         if(this.paused) {
            this.paused = false;
            Tessellator.instance = WrUpdateThread.this.threadTessellator;
            this.updateControl.resume();
         }

      }

      public void setUpdateControl(IWrUpdateControl updateControl) {
         this.updateControl = updateControl;
      }

      // $FF: synthetic method
      public ThreadUpdateControl(WrUpdateThread x1) {
         this();
      }
   }

   public class ThreadUpdateListener implements IWrUpdateListener {

      public ThreadUpdateControl tuc;


      public ThreadUpdateListener() {
         this.tuc = new ThreadUpdateControl((WrUpdateThread)null);
      }

      public void updating(IWrUpdateControl uc) {
         this.tuc.setUpdateControl(uc);
         WrUpdateThread.this.checkCanWork(this.tuc);
      }

      // $FF: synthetic method
      public ThreadUpdateListener(WrUpdateThread x1) {
         this();
      }
   }
}
