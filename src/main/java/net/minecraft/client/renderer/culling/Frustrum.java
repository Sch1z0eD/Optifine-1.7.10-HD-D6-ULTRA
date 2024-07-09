package net.minecraft.client.renderer.culling;

import net.minecraft.util.AxisAlignedBB;

public class Frustrum implements ICamera {

    public ClippingHelper clippingHelper = ClippingHelperImpl.getInstance();
    public double xPosition;
    public double yPosition;
    public double zPosition;
    public static final String __OBFID = "CL_00000976";


    public void setPosition(double par1, double par3, double par5) {
        this.xPosition = par1;
        this.yPosition = par3;
        this.zPosition = par5;
    }

    public boolean isBoxInFrustum(double par1, double par3, double par5, double par7, double par9, double par11) {
        return this.clippingHelper.isBoxInFrustum(par1 - this.xPosition, par3 - this.yPosition, par5 - this.zPosition, par7 - this.xPosition, par9 - this.yPosition, par11 - this.zPosition);
    }

    public boolean isBoundingBoxInFrustum(AxisAlignedBB par1AxisAlignedBB) {
        return this.isBoxInFrustum(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ, par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
    }

    public boolean isBoxInFrustumFully(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.clippingHelper.isBoxInFrustumFully(minX - this.xPosition, minY - this.yPosition, minZ - this.zPosition, maxX - this.xPosition, maxY - this.yPosition, maxZ - this.zPosition);
    }

    public boolean isBoundingBoxInFrustumFully(AxisAlignedBB aab) {
        return this.isBoxInFrustumFully(aab.minX, aab.minY, aab.minZ, aab.maxX, aab.maxY, aab.maxZ);
    }
}
