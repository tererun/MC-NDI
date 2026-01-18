package dev.imabad.ndi;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;

import dev.imabad.ndi.threads.NDIControlThread;
import dev.imabad.ndi.threads.NDIThread;
import me.walkerknapp.devolay.DevolayMetadataFrame;
import me.walkerknapp.devolay.DevolaySender;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameRenderHook {

    private NDIThread mainOutput;
    private NDIControlThread mainOutputControl;
    private final DevolaySender mainSender;

    private final ConcurrentHashMap<UUID, PBOManager> entityBuffers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, RenderTarget> entityFramebuffers = new ConcurrentHashMap<>();
    private PBOManager pboManager;

    public GameRenderHook(String senderName){
        mainSender = new DevolaySender(senderName);
        DevolayMetadataFrame metadataFrame = new DevolayMetadataFrame();
        metadataFrame.setData("<ndi_capabilities ntk_ptz=\"true\"/>");
        mainSender.addConnectionMetadata(metadataFrame);
    }

    public void setMainOutputFps(int fps) {
        if (mainOutput != null) {
            mainOutput.setTargetFps(fps);
        }
    }

    public void setMainOutputResolution(int width, int height) {
        if (mainOutput != null) {
            mainOutput.updateVideoFrame(width, height);
        }
        // Mark resolution changed so buffers get recreated
        resolutionChanged = true;
    }

    private boolean resolutionChanged = false;
    private int lastOutputWidth = 0;
    private int lastOutputHeight = 0;

    public Vec3 getEyePos(Entity entity){
        return entity.position().add(0, entity.getEyeHeight(), 0);
    }

    public Vec3 getLastTickPos(Entity entity){
        return new Vec3(entity.xo, entity.yo, entity.zo);
    }

    public Vec3 getLastTickEyePos(Entity entity){
        return getLastTickPos(entity).add(0, entity.getEyeHeight(), 0);
    }

    public void updatePos(Entity entity, Vec3 newPos, Vec3 lastTickPos){
        entity.setPosRaw(newPos.x, newPos.y, newPos.z);
        entity.xOld = lastTickPos.x;
        entity.yOld = lastTickPos.y;
        entity.zOld = lastTickPos.z;
        entity.xo = lastTickPos.x;
        entity.yo = lastTickPos.y;
        entity.zo = lastTickPos.z;
    }

    public void render(RenderTarget framebuffer, Window window, Player player, boolean isPaused){
        int outputWidth = NDIMod.getOutputWidth();
        int outputHeight = NDIMod.getOutputHeight();
        
        // Check if resolution setting changed
        boolean hasResChanged = resolutionChanged || (lastOutputWidth != outputWidth) || (lastOutputHeight != outputHeight);
        if (hasResChanged) {
            lastOutputWidth = outputWidth;
            lastOutputHeight = outputHeight;
            resolutionChanged = false;
        }
        
        if(mainOutput == null){
            pboManager = new PBOManager(window.getScreenWidth(), window.getScreenHeight());
            pboManager.readPixelData(framebuffer);
            mainOutput = new NDIThread(mainSender, pboManager.buffer, window.getScreenWidth(), window.getScreenHeight(), NDIMod.getOutputFps());
            mainOutput.start();
        } else if (mainOutput.getNeedsFrame().get()) {
            if (mainOutput.width.get() != window.getScreenWidth() || mainOutput.height.get() != window.getScreenHeight()) {
                pboManager.cleanUp();
                pboManager = new PBOManager(window.getScreenWidth(), window.getScreenHeight());
                mainOutput.updateVideoFrame(window.getScreenWidth(), window.getScreenHeight());
            }
            if(mainOutput.sender.get().getConnectionCount(0) > 0){
                pboManager.readPixelData(framebuffer);
                mainOutput.setByteBuffer(pboManager.buffer);
            }
        }
        if(player != null && !isPaused){
            if(mainOutputControl == null) {
                mainOutputControl = new NDIControlThread(mainSender, player);
                mainOutputControl.start();
            }
            List<CameraEntity> needFrames = new ArrayList<>();
            for(CameraEntity e : NDIMod.getCameraManager().cameraEntities){
                PBOManager pboManager;
                if(!entityBuffers.containsKey(e.getUUID())){
                    pboManager = new PBOManager(outputWidth, outputHeight);
                    entityBuffers.put(e.getUUID(), pboManager);
                } else {
                    pboManager = entityBuffers.get(e.getUUID());
                    if(hasResChanged){
                        pboManager.cleanUp();
                        pboManager = new PBOManager(outputWidth, outputHeight);
                        entityBuffers.put(e.getUUID(), pboManager);
                    }
                }
                NDIThread ndiThread;
                if(!NDIMod.getCameraManager().cameras.containsKey(e.getUUID())){
                    DevolaySender sender = new DevolaySender("MC - " + e.getDisplayName().getString());
                    DevolayMetadataFrame metadataFrame = new DevolayMetadataFrame();
                    metadataFrame.setData("<ndi_capabilities ntk_ptz=\"true\"/>");
                    sender.addConnectionMetadata(metadataFrame);
                    ndiThread = new NDIThread(sender, pboManager.buffer, outputWidth, outputHeight, NDIMod.getOutputFps());
                    NDIMod.getCameraManager().cameras.put(e.getUUID(), ndiThread);
                    NDIControlThread ndiControlThread = new NDIControlThread(sender, e);
                    NDIMod.getCameraManager().cameraControls.put(e.getUUID(), ndiControlThread);
                    ndiThread.start();
                    ndiControlThread.start();
                } else {
                    ndiThread = NDIMod.getCameraManager().cameras.get(e.getUUID());
                    if(hasResChanged){
                        ndiThread.updateVideoFrame(outputWidth, outputHeight);
                    }
                }
                if(e.isAlive() && ndiThread.getNeedsFrame().get() && ndiThread.sender.get().getConnectionCount(0) > 0) {
                    needFrames.add(e);
                }
            }
            if(needFrames.size() > 0){
                Minecraft minecraftClient = Minecraft.getInstance();
                if(minecraftClient.gameMode == null){
                    return;
                }
                boolean oldHudHidden = minecraftClient.options.hideGui;
                Entity oldCam = minecraftClient.cameraEntity;
                CameraType perspective = minecraftClient.options.getCameraType();
                minecraftClient.options.hideGui = true;
                minecraftClient.options.setCameraType(CameraType.FIRST_PERSON);
                RenderTarget oldWindow = minecraftClient.getMainRenderTarget();
                float prevCameraY = CameraExt.from(minecraftClient.gameRenderer.getMainCamera()).getCameraY();

                for(Entity e : needFrames){
                    if(e == null || !e.isAlive()){
                        continue;
                    }
                    RenderTarget entityFramebuffer;
                    if(!entityFramebuffers.containsKey(e.getUUID())){
                        entityFramebuffer = new TextureTarget("ndi_entity", outputWidth, outputHeight, true);
                        entityFramebuffers.put(e.getUUID(), entityFramebuffer);
                    } else {
                        entityFramebuffer = entityFramebuffers.get(e.getUUID());
                        if(hasResChanged){
                            entityFramebuffer.resize(outputWidth, outputHeight);
                        }
                    }
                    MinecraftClientExt clientExt = MinecraftClientExt.from(minecraftClient);
                    clientExt.setFramebuffer(entityFramebuffer);
                    clientExt.setNdiRenderSize(outputWidth, outputHeight);

                    PBOManager entityBytes = entityBuffers.get(e.getUUID());;
                    minecraftClient.cameraEntity = e;
                    CameraExt.from(minecraftClient.gameRenderer.getMainCamera()).setCameraY(e.getEyeHeight());
                    
                    DeltaTracker deltaTracker = minecraftClient.getDeltaTracker();
                    minecraftClient.gameRenderer.renderLevel(deltaTracker);
                    
                    entityBytes.readPixelData(entityFramebuffer);
                    NDIMod.getCameraManager().cameras.get(e.getUUID()).setByteBuffer(entityBytes.buffer);
                    CameraExt.from(minecraftClient.gameRenderer.getMainCamera()).setCameraY(prevCameraY);
                    
                    clientExt.clearNdiRenderSize();
                }
                MinecraftClientExt.from(minecraftClient).setFramebuffer(oldWindow);

                minecraftClient.cameraEntity = oldCam;
//                minecraftClient.gameRenderer.getCamera().updateEyeHeight();
                minecraftClient.options.hideGui = oldHudHidden;
                minecraftClient.options.setCameraType(perspective);
            }
        }
    }

}
