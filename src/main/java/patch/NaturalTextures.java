package patch;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

public class NaturalTextures {

   public static NaturalProperties[] propertiesByIndex = new NaturalProperties[0];


   public static void update() {
      propertiesByIndex = new NaturalProperties[0];
      if(Config.isNaturalTextures()) {
         String fileName = "optifine/natural.properties";

         try {
            ResourceLocation e = new ResourceLocation(fileName);
            if(!Config.hasResource(e)) {
               Config.dbg("patch.NaturalTextures: configuration \"" + fileName + "\" not found");
               propertiesByIndex = makeDefaultProperties();
               return;
            }

            InputStream in = Config.getResourceStream(e);
            ArrayList list = new ArrayList(256);
            String configStr = Config.readInputStream(in);
            in.close();
            String[] configLines = Config.tokenize(configStr, "\n\r");
            Config.dbg("Natural Textures: Parsing configuration \"" + fileName + "\"");

            for(int i = 0; i < configLines.length; ++i) {
               String line = configLines[i].trim();
               if(!line.startsWith("#")) {
                  String[] strs = Config.tokenize(line, "=");
                  if(strs.length != 2) {
                     Config.warn("Natural Textures: Invalid \"" + fileName + "\" line: " + line);
                  } else {
                     String key = strs[0].trim();
                     String type = strs[1].trim();
                     TextureAtlasSprite ts = TextureMap.textureMapBlocks.getIconSafe(key);
                     if(ts == null) {
                        Config.warn("Natural Textures: Texture not found: \"" + fileName + "\" line: " + line);
                     } else {
                        int tileNum = ts.getIndexInMap();
                        if(tileNum < 0) {
                           Config.warn("Natural Textures: Invalid \"" + fileName + "\" line: " + line);
                        } else {
                           NaturalProperties props = new NaturalProperties(type);
                           if(props.isValid()) {
                              while(list.size() <= tileNum) {
                                 list.add((Object)null);
                              }

                              list.set(tileNum, props);
                              Config.dbg("patch.NaturalTextures: " + key + " = " + type);
                           }
                        }
                     }
                  }
               }
            }

            propertiesByIndex = (NaturalProperties[])((NaturalProperties[])list.toArray(new NaturalProperties[list.size()]));
         } catch (FileNotFoundException var15) {
            Config.warn("patch.NaturalTextures: configuration \"" + fileName + "\" not found");
            propertiesByIndex = makeDefaultProperties();
            return;
         } catch (Exception var16) {
            var16.printStackTrace();
         }

      }
   }

   public static NaturalProperties getNaturalProperties(IIcon icon) {
      if(!(icon instanceof TextureAtlasSprite)) {
         return null;
      } else {
         TextureAtlasSprite ts = (TextureAtlasSprite)icon;
         int tileNum = ts.getIndexInMap();
         if(tileNum >= 0 && tileNum < propertiesByIndex.length) {
            NaturalProperties props = propertiesByIndex[tileNum];
            return props;
         } else {
            return null;
         }
      }
   }

   public static NaturalProperties[] makeDefaultProperties() {
      Config.dbg("patch.NaturalTextures: Checking default configuration.");
      ArrayList propsList = new ArrayList();
      setIconProperties(propsList, "grass_top", "4F");
      setIconProperties(propsList, "stone", "2F");
      setIconProperties(propsList, "dirt", "4F");
      setIconProperties(propsList, "grass_side", "F");
      setIconProperties(propsList, "grass_side_overlay", "F");
      setIconProperties(propsList, "stone_slab_top", "F");
      setIconProperties(propsList, "bedrock", "2F");
      setIconProperties(propsList, "sand", "4F");
      setIconProperties(propsList, "gravel", "2");
      setIconProperties(propsList, "log_oak", "2F");
      setIconProperties(propsList, "log_oak_top", "4F");
      setIconProperties(propsList, "gold_ore", "2F");
      setIconProperties(propsList, "iron_ore", "2F");
      setIconProperties(propsList, "coal_ore", "2F");
      setIconProperties(propsList, "diamond_ore", "2F");
      setIconProperties(propsList, "redstone_ore", "2F");
      setIconProperties(propsList, "lapis_ore", "2F");
      setIconProperties(propsList, "obsidian", "4F");
      setIconProperties(propsList, "leaves_oak", "2F");
      setIconProperties(propsList, "leaves_oak_opaque", "2F");
      setIconProperties(propsList, "leaves_jungle", "2");
      setIconProperties(propsList, "leaves_jungle_opaque", "2");
      setIconProperties(propsList, "snow", "4F");
      setIconProperties(propsList, "grass_side_snowed", "F");
      setIconProperties(propsList, "cactus_side", "2F");
      setIconProperties(propsList, "clay", "4F");
      setIconProperties(propsList, "mycelium_side", "F");
      setIconProperties(propsList, "mycelium_top", "4F");
      setIconProperties(propsList, "farmland_wet", "2F");
      setIconProperties(propsList, "farmland_dry", "2F");
      setIconProperties(propsList, "netherrack", "4F");
      setIconProperties(propsList, "soul_sand", "4F");
      setIconProperties(propsList, "glowstone", "4");
      setIconProperties(propsList, "log_spruce", "2F");
      setIconProperties(propsList, "log_birch", "F");
      setIconProperties(propsList, "leaves_spruce", "2F");
      setIconProperties(propsList, "leaves_spruce_opaque", "2F");
      setIconProperties(propsList, "log_jungle", "2F");
      setIconProperties(propsList, "end_stone", "4");
      setIconProperties(propsList, "sandstone_top", "4");
      setIconProperties(propsList, "sandstone_bottom", "4F");
      setIconProperties(propsList, "redstone_lamp_on", "4F");
      NaturalProperties[] terrainProps = (NaturalProperties[])((NaturalProperties[])propsList.toArray(new NaturalProperties[propsList.size()]));
      return terrainProps;
   }

   public static void setIconProperties(List propsList, String iconName, String propStr) {
      TextureMap terrainMap = TextureMap.textureMapBlocks;
      TextureAtlasSprite icon = terrainMap.getIconSafe(iconName);
      if(icon == null) {
         Config.warn("*** patch.NaturalProperties: Icon not found: " + iconName + " ***");
      } else if(!(icon instanceof TextureAtlasSprite)) {
         Config.warn("*** patch.NaturalProperties: Icon is not IconStitched: " + iconName + ": " + icon.getClass().getName() + " ***");
      } else {
         TextureAtlasSprite ts = (TextureAtlasSprite)icon;
         int index = ts.getIndexInMap();
         if(index < 0) {
            Config.warn("*** patch.NaturalProperties: Invalid index for icon: " + iconName + ": " + index + " ***");
         } else if(Config.isFromDefaultResourcePack(new ResourceLocation("textures/blocks/" + iconName + ".png"))) {
            while(index >= propsList.size()) {
               propsList.add((Object)null);
            }

            NaturalProperties props = new NaturalProperties(propStr);
            propsList.set(index, props);
            Config.dbg("patch.NaturalTextures: " + iconName + " = " + propStr);
         }
      }
   }

}
