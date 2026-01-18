package dev.imabad.ndi.mixin;

import dev.imabad.ndi.CameraEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {

    @Shadow @Final protected Minecraft minecraft;

    @Inject(method= "shouldShowEntityOutlines()Z", at =@At("HEAD"), cancellable=true)
    private void canDrawEntityOutlines(CallbackInfoReturnable<Boolean> cr){
        if(minecraft.getCameraEntity() instanceof CameraEntity){
            cr.setReturnValue(false);
            cr.cancel();
        }
    }

    // Fix: When camera is CameraEntity, always allow LocalPlayer to be rendered
    // Inject at the end to add LocalPlayer to the list if missing
    @Inject(method = "collectVisibleEntities", at = @At("RETURN"))
    private void addLocalPlayerWhenCameraEntity(Camera camera, net.minecraft.client.renderer.culling.Frustum frustum, List<Entity> list, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.getCameraEntity() instanceof CameraEntity && minecraft.player != null) {
            // Add player if not already in list
            if (!list.contains(minecraft.player)) {
                list.add(minecraft.player);
            }
        }
    }

}
