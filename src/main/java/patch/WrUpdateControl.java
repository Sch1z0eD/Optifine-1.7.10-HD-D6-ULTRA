package patch;

public class WrUpdateControl implements IWrUpdateControl {

   public int renderPass;


   public WrUpdateControl() {
      this.renderPass = 0;
   }

   public void resume() {}

   public void pause() {}

   public void setRenderPass(int renderPass) {
      this.renderPass = renderPass;
   }
}
