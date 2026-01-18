package dev.imabad.ndi.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.imabad.ndi.MinecraftClientExt;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "getWidth", at = @At("RETURN"), cancellable = true)
    private void modifyWidth(CallbackInfoReturnable<Integer> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            MinecraftClientExt ext = MinecraftClientExt.from(mc);
            if (ext.hasNdiRenderSize()) {
                cir.setReturnValue(ext.getNdiRenderWidth());
            }
        }
    }

    @Inject(method = "getHeight", at = @At("RETURN"), cancellable = true)
    private void modifyHeight(CallbackInfoReturnable<Integer> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            MinecraftClientExt ext = MinecraftClientExt.from(mc);
            if (ext.hasNdiRenderSize()) {
                cir.setReturnValue(ext.getNdiRenderHeight());
            }
        }
    }
}
