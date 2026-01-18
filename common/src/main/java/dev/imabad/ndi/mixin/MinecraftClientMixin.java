package dev.imabad.ndi.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import dev.imabad.ndi.MinecraftClientExt;
import dev.imabad.ndi.NDIMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin implements MinecraftClientExt {

    @Mutable
    @Shadow
    @Final
    private RenderTarget mainRenderTarget;
    @Shadow @Final private Window window;

    @Shadow public LocalPlayer player;

    @Shadow private boolean pause;

    @Inject(method = "runTick(Z)V", at=@At("RETURN"))
    public void runTick(boolean tick, CallbackInfo info) {
        if(NDIMod.getGameRenderHook() != null) {
            NDIMod.getGameRenderHook().render(mainRenderTarget, window, player, pause);
        }
    }

    @Override
    public void setFramebuffer(RenderTarget fb) {
        mainRenderTarget = fb;
    }

    // TODO: These methods need to be updated for 1.21.8 - camera save/load disabled for now
    // @Inject(method= "updateLevelInEngineRecordPlayback", at=@At("RETURN"))
    // public void joinWorld(ClientLevel clientWorld, CallbackInfo callbackInfo){
    //     NDIMod.getCameraManager().load(gameDirectory, hasSingleplayerServer(), singleplayerServer, levelSource, clientWorld, this.getConnection());
    // }

    // @Inject(method= "disconnect(Lnet/minecraft/client/gui/screens/Screen;)V", at=@At("HEAD"))
    // public void onDisconnect(Screen screen, CallbackInfo ci){
    //     NDIMod.getCameraManager().save(gameDirectory, player, hasSingleplayerServer(), levelSource, singleplayerServer);
    // }
}
