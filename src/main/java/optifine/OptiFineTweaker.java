package optifine;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OptiFineTweaker implements ITweaker {

    public List args;


    public void acceptOptions(List args, File gameDir, File assetsDir, String profile) {
        dbg("OptiFineTweaker: acceptOptions");
        this.args = new ArrayList(args);
        this.args.add("--gameDir");
        this.args.add(gameDir.getAbsolutePath());
        this.args.add("--assetsDir");
        this.args.add(assetsDir.getAbsolutePath());
        this.args.add("--version");
        this.args.add(profile);
    }

    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        dbg("OptiFineTweaker: injectIntoClassLoader");
        classLoader.registerTransformer("optifine.OptiFineClassTransformer");
    }

    public String getLaunchTarget() {
        dbg("OptiFineTweaker: getLaunchTarget");
        return "net.minecraft.client.main.Main";
    }

    public String[] getLaunchArguments() {
        dbg("OptiFineTweaker: getLaunchArguments");
        return (String[]) this.args.toArray(new String[this.args.size()]);
    }

    public static void dbg(String str) {
        System.out.println(str);
    }
}
