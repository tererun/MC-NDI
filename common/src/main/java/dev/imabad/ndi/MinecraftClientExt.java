package dev.imabad.ndi;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

public interface MinecraftClientExt {

    void setFramebuffer(RenderTarget fb);
    void setNdiRenderSize(int width, int height);
    void clearNdiRenderSize();
    int getNdiRenderWidth();
    int getNdiRenderHeight();
    boolean hasNdiRenderSize();

    static MinecraftClientExt from(Minecraft self){
        return (MinecraftClientExt)self;
    }

}
