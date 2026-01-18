package dev.imabad.ndi.mixin;

import dev.imabad.ndi.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private float renderDistance;

    // Note: Projection matrix modification for custom resolution NDI output
    // is now handled via RenderSystem.viewport in GameRenderHook
}
