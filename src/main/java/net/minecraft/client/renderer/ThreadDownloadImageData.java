package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patch.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadDownloadImageData extends SimpleTexture {

    public static final Logger logger = LogManager.getLogger();
    public static final AtomicInteger threadDownloadCounter = new AtomicInteger(0);
    public final File field_152434_e;
    public final String imageUrl;
    public final IImageBuffer imageBuffer;
    public BufferedImage bufferedImage;
    public Thread imageThread;
    public boolean textureUploaded;
    public static final String __OBFID = "CL_00001049";
    public Boolean imageFound = null;
    public boolean pipeline = false;


    public ThreadDownloadImageData(File par1GuiCreateFlatWorld, String p_i1049_2_, ResourceLocation p_i1049_3_, IImageBuffer p_i1049_4_) {
        super(p_i1049_3_);
        this.field_152434_e = par1GuiCreateFlatWorld;
        this.imageUrl = p_i1049_2_;
        this.imageBuffer = p_i1049_4_;
    }

    public void checkTextureUploaded() {
        if (!this.textureUploaded && this.bufferedImage != null) {
            if (this.textureLocation != null) {
                this.deleteGlTexture();
            }

            TextureUtil.uploadTextureImage(super.getGlTextureId(), this.bufferedImage);
            this.textureUploaded = true;
        }

    }

    public int getGlTextureId() {
        this.checkTextureUploaded();
        return super.getGlTextureId();
    }

    public void setBufferedImage(BufferedImage p_147641_1_) {
        this.bufferedImage = p_147641_1_;
        if (this.imageBuffer != null) {
            this.imageBuffer.func_152634_a();
        }

        this.imageFound = Boolean.valueOf(this.bufferedImage != null);
    }

    public void loadTexture(IResourceManager par1ResourceManager) throws IOException {
        if (this.bufferedImage == null && this.textureLocation != null) {
            super.loadTexture(par1ResourceManager);
        }

        if (this.imageThread == null) {
            if (this.field_152434_e != null && this.field_152434_e.isFile()) {
                logger.debug("Loading http texture from local cache ({})", new Object[]{this.field_152434_e});

                try {
                    this.bufferedImage = ImageIO.read(this.field_152434_e);
                    if (this.imageBuffer != null) {
                        this.setBufferedImage(this.imageBuffer.parseUserSkin(this.bufferedImage));
                    }

                    this.imageFound = Boolean.valueOf(this.bufferedImage != null);
                } catch (IOException var3) {
                    logger.error("Couldn\'t load skin " + this.field_152434_e, var3);
                    this.func_152433_a();
                }
            } else {
                this.func_152433_a();
            }
        }

    }

    public void func_152433_a() {
        this.imageThread = new Thread("Texture Downloader #" + threadDownloadCounter.incrementAndGet()) {

            public static final String __OBFID = "CL_00001050";

            public void run() {
                HttpURLConnection var1 = null;
                ThreadDownloadImageData.logger.debug("Downloading http texture from {} to {}", new Object[]{ThreadDownloadImageData.this.imageUrl, ThreadDownloadImageData.this.field_152434_e});
                if (ThreadDownloadImageData.this.shouldPipeline()) {
                    ThreadDownloadImageData.this.loadPipelined();
                } else {
                    try {
                        var1 = (HttpURLConnection) (new URL(ThreadDownloadImageData.this.imageUrl)).openConnection(Minecraft.getMinecraft().getProxy());
                        var1.setDoInput(true);
                        var1.setDoOutput(false);
                        var1.connect();
                        if (var1.getResponseCode() / 100 == 2) {
                            BufferedImage var6;
                            if (ThreadDownloadImageData.this.field_152434_e != null) {
                                FileUtils.copyInputStreamToFile(var1.getInputStream(), ThreadDownloadImageData.this.field_152434_e);
                                var6 = ImageIO.read(ThreadDownloadImageData.this.field_152434_e);
                            } else {
                                var6 = ImageIO.read(var1.getInputStream());
                            }

                            if (ThreadDownloadImageData.this.imageBuffer != null) {
                                var6 = ThreadDownloadImageData.this.imageBuffer.parseUserSkin(var6);
                            }

                            ThreadDownloadImageData.this.setBufferedImage(var6);
                            return;
                        }

                        if (var1.getErrorStream() != null) {
                            Config.readAll(var1.getErrorStream());
                        }
                    } catch (Exception var61) {
                        ThreadDownloadImageData.logger.error("Couldn\'t download http texture: " + var61.getClass().getName() + ": " + var61.getMessage());
                        return;
                    } finally {
                        if (var1 != null) {
                            var1.disconnect();
                        }

                        ThreadDownloadImageData.this.imageFound = Boolean.valueOf(ThreadDownloadImageData.this.bufferedImage != null);
                    }

                }
            }
        };
        this.imageThread.setDaemon(true);
        this.imageThread.start();
    }

    public boolean shouldPipeline() {
        if (!this.pipeline) {
            return false;
        } else {
            Proxy proxy = Minecraft.getMinecraft().getProxy();
            return proxy.type() != Type.DIRECT && proxy.type() != Type.SOCKS ? false : this.imageUrl.startsWith("http://");
        }
    }

    public void loadPipelined() {
        try {
            HttpRequest var6 = HttpPipeline.makeRequest(this.imageUrl, Minecraft.getMinecraft().getProxy());
            HttpResponse resp = HttpPipeline.executeRequest(var6);
            if (resp.getStatus() / 100 != 2) {
                return;
            }

            byte[] body = resp.getBody();
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            BufferedImage var2;
            if (this.field_152434_e != null) {
                FileUtils.copyInputStreamToFile(bais, this.field_152434_e);
                var2 = ImageIO.read(this.field_152434_e);
            } else {
                var2 = TextureUtils.readBufferedImage(bais);
            }

            if (this.imageBuffer != null) {
                var2 = this.imageBuffer.parseUserSkin(var2);
            }

            this.setBufferedImage(var2);
            return;
        } catch (Exception var9) {
            logger.error("Couldn\'t download http texture: " + var9.getClass().getName() + ": " + var9.getMessage());
        } finally {
            this.imageFound = Boolean.valueOf(this.bufferedImage != null);
        }

    }

}
