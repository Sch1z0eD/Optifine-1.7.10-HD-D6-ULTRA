package net.minecraft.client.gui;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import patch.Config;
import patch.CustomColorizer;
import patch.FontUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class FontRenderer implements IResourceManagerReloadListener {

    public static final ResourceLocation[] unicodePageLocations = new ResourceLocation[256];
    public float[] charWidth = new float[256];
    public int FONT_HEIGHT = 9;
    public Random fontRandom = new Random();
    public byte[] glyphWidth = new byte[65536];
    public int[] colorCode = new int[32];
    public ResourceLocation locationFontTexture;
    public final TextureManager renderEngine;
    public float posX;
    public float posY;
    public boolean unicodeFlag;
    public boolean bidiFlag;
    public float red;
    public float blue;
    public float green;
    public float alpha;
    public int textColor;
    public boolean randomStyle;
    public boolean boldStyle;
    public boolean italicStyle;
    public boolean underlineStyle;
    public boolean strikethroughStyle;
    public static final String __OBFID = "CL_00000660";
    public GameSettings gameSettings;
    public ResourceLocation locationFontTextureBase;
    public boolean enabled = true;
    public float offsetBold = 1.0F;


    public FontRenderer(GameSettings par1GameSettings, ResourceLocation par2ResourceLocation, TextureManager par3TextureManager, boolean par4) {
        this.gameSettings = par1GameSettings;
        this.locationFontTextureBase = par2ResourceLocation;
        this.locationFontTexture = par2ResourceLocation;
        this.renderEngine = par3TextureManager;
        this.unicodeFlag = par4;
        this.locationFontTexture = FontUtils.getHdFontLocation(this.locationFontTextureBase);
        this.bindTexture(this.locationFontTexture);

        for (int var5 = 0; var5 < 32; ++var5) {
            int var6 = (var5 >> 3 & 1) * 85;
            int var7 = (var5 >> 2 & 1) * 170 + var6;
            int var8 = (var5 >> 1 & 1) * 170 + var6;
            int var9 = (var5 >> 0 & 1) * 170 + var6;
            if (var5 == 6) {
                var7 += 85;
            }

            if (par1GameSettings.anaglyph) {
                int var10 = (var7 * 30 + var8 * 59 + var9 * 11) / 100;
                int var11 = (var7 * 30 + var8 * 70) / 100;
                int var12 = (var7 * 30 + var9 * 70) / 100;
                var7 = var10;
                var8 = var11;
                var9 = var12;
            }

            if (var5 >= 16) {
                var7 /= 4;
                var8 /= 4;
                var9 /= 4;
            }

            this.colorCode[var5] = (var7 & 255) << 16 | (var8 & 255) << 8 | var9 & 255;
        }

        this.readGlyphSizes();
    }

    public void onResourceManagerReload(IResourceManager par1ResourceManager) {
        this.locationFontTexture = FontUtils.getHdFontLocation(this.locationFontTextureBase);

        for (int i = 0; i < unicodePageLocations.length; ++i) {
            unicodePageLocations[i] = null;
        }

        this.readFontTexture();
        this.readGlyphSizes();
    }

    public void readFontTexture() {
        BufferedImage bufferedimage;
        try {
            bufferedimage = ImageIO.read(this.getResourceInputStream(this.locationFontTexture));
        } catch (IOException var21) {
            throw new RuntimeException(var21);
        }

        Properties props = FontUtils.readFontProperties(this.locationFontTexture);
        int imgWidth = bufferedimage.getWidth();
        int imgHeight = bufferedimage.getHeight();
        int charW = imgWidth / 16;
        int charH = imgHeight / 16;
        float kx = (float) imgWidth / 128.0F;
        float boldScaleFactor = Config.limit(kx, 1.0F, 2.0F);
        this.offsetBold = 1.0F / boldScaleFactor;
        float offsetBoldConfig = FontUtils.readFloat(props, "offsetBold", -1.0F);
        if (offsetBoldConfig >= 0.0F) {
            this.offsetBold = offsetBoldConfig;
        }

        int[] ai = new int[imgWidth * imgHeight];
        bufferedimage.getRGB(0, 0, imgWidth, imgHeight, ai, 0, imgWidth);
        int k = 0;

        while (k < 256) {
            int cx = k % 16;
            int cy = k / 16;
            boolean px = false;
            int var22 = charW - 1;

            while (true) {
                if (var22 >= 0) {
                    int x = cx * charW + var22;
                    boolean flag = true;

                    for (int py = 0; py < charH && flag; ++py) {
                        int ypos = (cy * charH + py) * imgWidth;
                        int col = ai[x + ypos];
                        int al = col >> 24 & 255;
                        if (al > 16) {
                            flag = false;
                        }
                    }

                    if (flag) {
                        --var22;
                        continue;
                    }
                }

                if (k == 65) {
                    k = k;
                }

                if (k == 32) {
                    if (charW <= 8) {
                        var22 = (int) (2.0F * kx);
                    } else {
                        var22 = (int) (1.5F * kx);
                    }
                }

                this.charWidth[k] = (float) (var22 + 1) / kx + 1.0F;
                ++k;
                break;
            }
        }

        FontUtils.readCustomCharWidths(props, this.charWidth);
    }

    public void readGlyphSizes() {
        try {
            InputStream var2 = this.getResourceInputStream(new ResourceLocation("font/glyph_sizes.bin"));
            var2.read(this.glyphWidth);
        } catch (IOException var21) {
            throw new RuntimeException(var21);
        }
    }

    public float renderCharAtPos(int par1, char par2, boolean par3) {
        return par2 == 32 ? (!this.unicodeFlag ? this.charWidth[par2] : 4.0F) : (par2 == 32 ? 4.0F : ("ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ        !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■ ".indexOf(par2) != -1 && !this.unicodeFlag ? this.renderDefaultChar(par1, par3) : this.renderUnicodeChar(par2, par3)));
    }

    public float renderDefaultChar(int par1, boolean par2) {
        float var3 = (float) (par1 % 16 * 8);
        float var4 = (float) (par1 / 16 * 8);
        float var5 = par2 ? 1.0F : 0.0F;
        this.bindTexture(this.locationFontTexture);
        float var6 = 7.99F;
        GL11.glBegin(5);
        GL11.glTexCoord2f(var3 / 128.0F, var4 / 128.0F);
        GL11.glVertex3f(this.posX + var5, this.posY, 0.0F);
        GL11.glTexCoord2f(var3 / 128.0F, (var4 + 7.99F) / 128.0F);
        GL11.glVertex3f(this.posX - var5, this.posY + 7.99F, 0.0F);
        GL11.glTexCoord2f((var3 + var6 - 1.0F) / 128.0F, var4 / 128.0F);
        GL11.glVertex3f(this.posX + var6 - 1.0F + var5, this.posY, 0.0F);
        GL11.glTexCoord2f((var3 + var6 - 1.0F) / 128.0F, (var4 + 7.99F) / 128.0F);
        GL11.glVertex3f(this.posX + var6 - 1.0F - var5, this.posY + 7.99F, 0.0F);
        GL11.glEnd();
        return this.charWidth[par1];
    }

    public ResourceLocation getUnicodePageLocation(int par1) {
        if (unicodePageLocations[par1] == null) {
            unicodePageLocations[par1] = new ResourceLocation(String.format("textures/font/unicode_page_%02x.png", new Object[]{Integer.valueOf(par1)}));
            unicodePageLocations[par1] = FontUtils.getHdFontLocation(unicodePageLocations[par1]);
        }

        return unicodePageLocations[par1];
    }

    public void loadGlyphTexture(int par1) {
        this.bindTexture(this.getUnicodePageLocation(par1));
    }

    public float renderUnicodeChar(char par1, boolean par2) {
        if (this.glyphWidth[par1] == 0) {
            return 0.0F;
        } else {
            int var3 = par1 / 256;
            this.loadGlyphTexture(var3);
            int var4 = this.glyphWidth[par1] >>> 4;
            int var5 = this.glyphWidth[par1] & 15;
            var4 &= 15;
            float var6 = (float) var4;
            float var7 = (float) (var5 + 1);
            float var8 = (float) (par1 % 16 * 16) + var6;
            float var9 = (float) ((par1 & 255) / 16 * 16);
            float var10 = var7 - var6 - 0.02F;
            float var11 = par2 ? 1.0F : 0.0F;
            GL11.glBegin(5);
            GL11.glTexCoord2f(var8 / 256.0F, var9 / 256.0F);
            GL11.glVertex3f(this.posX + var11, this.posY, 0.0F);
            GL11.glTexCoord2f(var8 / 256.0F, (var9 + 15.98F) / 256.0F);
            GL11.glVertex3f(this.posX - var11, this.posY + 7.99F, 0.0F);
            GL11.glTexCoord2f((var8 + var10) / 256.0F, var9 / 256.0F);
            GL11.glVertex3f(this.posX + var10 / 2.0F + var11, this.posY, 0.0F);
            GL11.glTexCoord2f((var8 + var10) / 256.0F, (var9 + 15.98F) / 256.0F);
            GL11.glVertex3f(this.posX + var10 / 2.0F - var11, this.posY + 7.99F, 0.0F);
            GL11.glEnd();
            return (var7 - var6) / 2.0F + 1.0F;
        }
    }

    public int drawStringWithShadow(String par1Str, int par2, int par3, int par4) {
        return this.drawString(par1Str, par2, par3, par4, true);
    }

    public int drawString(String par1Str, int par2, int par3, int par4) {
        return !this.enabled ? 0 : this.drawString(par1Str, par2, par3, par4, false);
    }

    public int drawString(String par1Str, int par2, int par3, int par4, boolean par5) {
        this.enableAlpha();
        this.resetStyles();
        int var6;
        if (par5) {
            var6 = this.renderString(par1Str, par2 + 1, par3 + 1, par4, true);
            var6 = Math.max(var6, this.renderString(par1Str, par2, par3, par4, false));
        } else {
            var6 = this.renderString(par1Str, par2, par3, par4, false);
        }

        return var6;
    }

    public String bidiReorder(String p_147647_1_) {
        try {
            Bidi var3 = new Bidi((new ArabicShaping(8)).shape(p_147647_1_), 127);
            var3.setReorderingMode(0);
            return var3.writeReordered(2);
        } catch (ArabicShapingException var31) {
            return p_147647_1_;
        }
    }

    public void resetStyles() {
        this.randomStyle = false;
        this.boldStyle = false;
        this.italicStyle = false;
        this.underlineStyle = false;
        this.strikethroughStyle = false;
    }

    public void renderStringAtPos(String par1Str, boolean par2) {
        for (int var3 = 0; var3 < par1Str.length(); ++var3) {
            char var4 = par1Str.charAt(var3);
            int var5;
            int var6;
            if (var4 == 167 && var3 + 1 < par1Str.length()) {
                var5 = "0123456789abcdefklmnor".indexOf(par1Str.toLowerCase().charAt(var3 + 1));
                if (var5 < 16) {
                    this.randomStyle = false;
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;
                    if (var5 < 0 || var5 > 15) {
                        var5 = 15;
                    }

                    if (par2) {
                        var5 += 16;
                    }

                    var6 = this.colorCode[var5];
                    if (Config.isCustomColors()) {
                        var6 = CustomColorizer.getTextColor(var5, var6);
                    }

                    this.textColor = var6;
                    this.setColor((float) (var6 >> 16) / 255.0F, (float) (var6 >> 8 & 255) / 255.0F, (float) (var6 & 255) / 255.0F, this.alpha);
                } else if (var5 == 16) {
                    this.randomStyle = true;
                } else if (var5 == 17) {
                    this.boldStyle = true;
                } else if (var5 == 18) {
                    this.strikethroughStyle = true;
                } else if (var5 == 19) {
                    this.underlineStyle = true;
                } else if (var5 == 20) {
                    this.italicStyle = true;
                } else if (var5 == 21) {
                    this.randomStyle = false;
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;
                    this.setColor(this.red, this.blue, this.green, this.alpha);
                }

                ++var3;
            } else {
                var5 = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ        !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■ ".indexOf(var4);
                if (this.randomStyle && var5 != -1) {
                    do {
                        var6 = this.fontRandom.nextInt(this.charWidth.length);
                    } while ((int) this.charWidth[var5] != (int) this.charWidth[var6]);

                    var5 = var6;
                }

                float var11 = var5 != -1 && !this.unicodeFlag ? this.offsetBold : 0.5F;
                boolean var7 = (var4 == 0 || var5 == -1 || this.unicodeFlag) && par2;
                if (var7) {
                    this.posX -= var11;
                    this.posY -= var11;
                }

                float var8 = this.renderCharAtPos(var5, var4, this.italicStyle);
                if (var7) {
                    this.posX += var11;
                    this.posY += var11;
                }

                if (this.boldStyle) {
                    this.posX += var11;
                    if (var7) {
                        this.posX -= var11;
                        this.posY -= var11;
                    }

                    this.renderCharAtPos(var5, var4, this.italicStyle);
                    this.posX -= var11;
                    if (var7) {
                        this.posX += var11;
                        this.posY += var11;
                    }

                    var8 += var11;
                }

                Tessellator var9;
                if (this.strikethroughStyle) {
                    var9 = Tessellator.instance;
                    GL11.glDisable(3553);
                    var9.startDrawingQuads();
                    var9.addVertex((double) this.posX, (double) (this.posY + (float) (this.FONT_HEIGHT / 2)), 0.0D);
                    var9.addVertex((double) (this.posX + var8), (double) (this.posY + (float) (this.FONT_HEIGHT / 2)), 0.0D);
                    var9.addVertex((double) (this.posX + var8), (double) (this.posY + (float) (this.FONT_HEIGHT / 2) - 1.0F), 0.0D);
                    var9.addVertex((double) this.posX, (double) (this.posY + (float) (this.FONT_HEIGHT / 2) - 1.0F), 0.0D);
                    var9.draw();
                    GL11.glEnable(3553);
                }

                if (this.underlineStyle) {
                    var9 = Tessellator.instance;
                    GL11.glDisable(3553);
                    var9.startDrawingQuads();
                    int var10 = this.underlineStyle ? -1 : 0;
                    var9.addVertex((double) (this.posX + (float) var10), (double) (this.posY + (float) this.FONT_HEIGHT), 0.0D);
                    var9.addVertex((double) (this.posX + var8), (double) (this.posY + (float) this.FONT_HEIGHT), 0.0D);
                    var9.addVertex((double) (this.posX + var8), (double) (this.posY + (float) this.FONT_HEIGHT - 1.0F), 0.0D);
                    var9.addVertex((double) (this.posX + (float) var10), (double) (this.posY + (float) this.FONT_HEIGHT - 1.0F), 0.0D);
                    var9.draw();
                    GL11.glEnable(3553);
                }

                this.posX += var8;
            }
        }

    }

    public int renderStringAligned(String par1Str, int par2, int par3, int par4, int par5, boolean par6) {
        if (this.bidiFlag) {
            int var7 = this.getStringWidth(this.bidiReorder(par1Str));
            par2 = par2 + par4 - var7;
        }

        return this.renderString(par1Str, par2, par3, par5, par6);
    }

    public int renderString(String par1Str, int par2, int par3, int par4, boolean par5) {
        if (par1Str == null) {
            return 0;
        } else {
            if (this.bidiFlag) {
                par1Str = this.bidiReorder(par1Str);
            }

            if ((par4 & -67108864) == 0) {
                par4 |= -16777216;
            }

            if (par5) {
                par4 = (par4 & 16579836) >> 2 | par4 & -16777216;
            }

            this.red = (float) (par4 >> 16 & 255) / 255.0F;
            this.blue = (float) (par4 >> 8 & 255) / 255.0F;
            this.green = (float) (par4 & 255) / 255.0F;
            this.alpha = (float) (par4 >> 24 & 255) / 255.0F;
            this.setColor(this.red, this.blue, this.green, this.alpha);
            this.posX = (float) par2;
            this.posY = (float) par3;
            this.renderStringAtPos(par1Str, par5);
            return (int) this.posX;
        }
    }

    public int getStringWidth(String par1Str) {
        if (par1Str == null) {
            return 0;
        } else {
            float var2 = 0.0F;
            boolean var3 = false;

            for (int var4 = 0; var4 < par1Str.length(); ++var4) {
                char var5 = par1Str.charAt(var4);
                float var6 = this.getCharWidthFloat(var5);
                if (var6 < 0.0F && var4 < par1Str.length() - 1) {
                    ++var4;
                    var5 = par1Str.charAt(var4);
                    if (var5 != 108 && var5 != 76) {
                        if (var5 == 114 || var5 == 82) {
                            var3 = false;
                        }
                    } else {
                        var3 = true;
                    }

                    var6 = 0.0F;
                }

                var2 += var6;
                if (var3 && var6 > 0.0F) {
                    var2 += this.unicodeFlag ? 1.0F : this.offsetBold;
                }
            }

            return (int) var2;
        }
    }

    public int getCharWidth(char par1) {
        return Math.round(this.getCharWidthFloat(par1));
    }

    public float getCharWidthFloat(char par1) {
        if (par1 == 167) {
            return -1.0F;
        } else if (par1 == 32) {
            return this.charWidth[32];
        } else {
            int var2 = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ        !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■ ".indexOf(par1);
            if (par1 > 0 && var2 != -1 && !this.unicodeFlag) {
                return this.charWidth[var2];
            } else if (this.glyphWidth[par1] != 0) {
                int var3 = this.glyphWidth[par1] >>> 4;
                int var4 = this.glyphWidth[par1] & 15;
                var3 &= 15;
                ++var4;
                return (float) ((var4 - var3) / 2 + 1);
            } else {
                return 0.0F;
            }
        }
    }

    public String trimStringToWidth(String par1Str, int par2) {
        return this.trimStringToWidth(par1Str, par2, false);
    }

    public String trimStringToWidth(String par1Str, int par2, boolean par3) {
        StringBuilder var4 = new StringBuilder();
        float var5 = 0.0F;
        int var6 = par3 ? par1Str.length() - 1 : 0;
        int var7 = par3 ? -1 : 1;
        boolean var8 = false;
        boolean var9 = false;

        for (int var10 = var6; var10 >= 0 && var10 < par1Str.length() && var5 < (float) par2; var10 += var7) {
            char var11 = par1Str.charAt(var10);
            float var12 = this.getCharWidthFloat(var11);
            if (var8) {
                var8 = false;
                if (var11 != 108 && var11 != 76) {
                    if (var11 == 114 || var11 == 82) {
                        var9 = false;
                    }
                } else {
                    var9 = true;
                }
            } else if (var12 < 0.0F) {
                var8 = true;
            } else {
                var5 += var12;
                if (var9) {
                    ++var5;
                }
            }

            if (var5 > (float) par2) {
                break;
            }

            if (par3) {
                var4.insert(0, var11);
            } else {
                var4.append(var11);
            }
        }

        return var4.toString();
    }

    public String trimStringNewline(String par1Str) {
        while (par1Str != null && par1Str.endsWith("\n")) {
            par1Str = par1Str.substring(0, par1Str.length() - 1);
        }

        return par1Str;
    }

    public void drawSplitString(String par1Str, int par2, int par3, int par4, int par5) {
        this.resetStyles();
        this.textColor = par5;
        par1Str = this.trimStringNewline(par1Str);
        this.renderSplitString(par1Str, par2, par3, par4, false);
    }

    public void renderSplitString(String par1Str, int par2, int par3, int par4, boolean par5) {
        List var6 = this.listFormattedStringToWidth(par1Str, par4);

        for (Iterator var7 = var6.iterator(); var7.hasNext(); par3 += this.FONT_HEIGHT) {
            String var8 = (String) var7.next();
            this.renderStringAligned(var8, par2, par3, par4, this.textColor, par5);
        }

    }

    public int splitStringWidth(String par1Str, int par2) {
        return this.FONT_HEIGHT * this.listFormattedStringToWidth(par1Str, par2).size();
    }

    public void setUnicodeFlag(boolean par1) {
        this.unicodeFlag = par1;
    }

    public boolean getUnicodeFlag() {
        return this.unicodeFlag;
    }

    public void setBidiFlag(boolean par1) {
        this.bidiFlag = par1;
    }

    public List listFormattedStringToWidth(String par1Str, int par2) {
        return Arrays.asList(this.wrapFormattedStringToWidth(par1Str, par2).split("\n"));
    }

    public String wrapFormattedStringToWidth(String par1Str, int par2) {
        int var3 = this.sizeStringToWidth(par1Str, par2);
        if (par1Str.length() <= var3) {
            return par1Str;
        } else {
            String var4 = par1Str.substring(0, var3);
            char var5 = par1Str.charAt(var3);
            boolean var6 = var5 == 32 || var5 == 10;
            String var7 = getFormatFromString(var4) + par1Str.substring(var3 + (var6 ? 1 : 0));
            return var4 + "\n" + this.wrapFormattedStringToWidth(var7, par2);
        }
    }

    public int sizeStringToWidth(String par1Str, int par2) {
        int var3 = par1Str.length();
        float var4 = 0.0F;
        int var5 = 0;
        int var6 = -1;

        for (boolean var7 = false; var5 < var3; ++var5) {
            char var8 = par1Str.charAt(var5);
            switch (var8) {
                case 10:
                    --var5;
                    break;
                case 32:
                    var6 = var5;
                case 167:
                    if (var5 < var3 - 1) {
                        ++var5;
                        char var9 = par1Str.charAt(var5);
                        if (var9 != 108 && var9 != 76) {
                            if (var9 == 114 || var9 == 82 || isFormatColor(var9)) {
                                var7 = false;
                            }
                        } else {
                            var7 = true;
                        }
                    }
                    break;
                default:
                    var4 += this.getCharWidthFloat(var8);
                    if (var7) {
                        ++var4;
                    }
            }

            if (var8 == 10) {
                ++var5;
                var6 = var5;
                break;
            }

            if (var4 > (float) par2) {
                break;
            }
        }

        return var5 != var3 && var6 != -1 && var6 < var5 ? var6 : var5;
    }

    public static boolean isFormatColor(char par0) {
        return par0 >= 48 && par0 <= 57 || par0 >= 97 && par0 <= 102 || par0 >= 65 && par0 <= 70;
    }

    public static boolean isFormatSpecial(char par0) {
        return par0 >= 107 && par0 <= 111 || par0 >= 75 && par0 <= 79 || par0 == 114 || par0 == 82;
    }

    public static String getFormatFromString(String par0Str) {
        String var1 = "";
        int var2 = -1;
        int var3 = par0Str.length();

        while ((var2 = par0Str.indexOf(167, var2 + 1)) != -1) {
            if (var2 < var3 - 1) {
                char var4 = par0Str.charAt(var2 + 1);
                if (isFormatColor(var4)) {
                    var1 = "§" + var4;
                } else if (isFormatSpecial(var4)) {
                    var1 = var1 + "§" + var4;
                }
            }
        }

        return var1;
    }

    public boolean getBidiFlag() {
        return this.bidiFlag;
    }

    public void setColor(float r, float g, float b, float a) {
        GL11.glColor4f(r, g, b, a);
    }

    public void enableAlpha() {
        GL11.glEnable(3008);
    }

    public void bindTexture(ResourceLocation location) {
        this.renderEngine.bindTexture(location);
    }

    public InputStream getResourceInputStream(ResourceLocation location) throws IOException {
        return Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream();
    }

}
