package dev.imabad.ndi.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.imabad.ndi.CameraEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerRenderState, PlayerModel> {

    public PlayerEntityRendererMixin(EntityRendererProvider.Context context, PlayerModel model, float someFloat) {
        super(context, model, someFloat);
    }

    @Inject(method= "setModelProperties", at=@At("HEAD"), cancellable = true)
    public void setModelPose(PlayerRenderState playerRenderState, CallbackInfo callbackInfo){
        PlayerModel playerEntityModel = this.getModel();
        // Note: In 1.21+, we can't easily check if the render state belongs to a CameraEntity
        // This mixin may need to be reworked or removed depending on how CameraEntity renders
    }

    // Note: renderNameTag signature has changed in 1.21+, this needs to be updated
    // The method now takes RenderState instead of Entity
}
