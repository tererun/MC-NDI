package dev.imabad.ndi;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import dev.imabad.ndi.screens.NDISettingsScreen;
import me.walkerknapp.devolay.Devolay;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class NDIMod {

    public static final String MOD_ID = "mcndi";

    private static CameraManager cameraManager;
    private static GameRenderHook gameRenderHook;
    private static boolean ndiAvailable = false;

    private static int outputWidth = 0;
    private static int outputHeight = 0;
    private static int outputFps = 30;
    private static boolean useCustomResolution = false;

    public static CameraManager getCameraManager() {
        return cameraManager;
    }
    public static GameRenderHook getGameRenderHook() { return gameRenderHook; }
    public static boolean isNdiAvailable() { return ndiAvailable; }

    public static int getOutputWidth() {
        if (useCustomResolution && outputWidth > 0) return outputWidth;
        return Minecraft.getInstance().getWindow().getScreenWidth();
    }

    public static int getOutputHeight() {
        if (useCustomResolution && outputHeight > 0) return outputHeight;
        return Minecraft.getInstance().getWindow().getScreenHeight();
    }

    public static int getOutputFps() {
        return outputFps;
    }

    public static void setOutputResolution(int width, int height) {
        outputWidth = width;
        outputHeight = height;
        useCustomResolution = true;
        // Apply to all existing NDI threads
        if (cameraManager != null) {
            cameraManager.cameras.values().forEach(thread -> thread.updateVideoFrame(width, height));
        }
        if (gameRenderHook != null) {
            gameRenderHook.setMainOutputResolution(width, height);
        }
    }

    public static void setOutputFps(int fps) {
        outputFps = fps;
        // Apply to all existing NDI threads
        if (cameraManager != null) {
            cameraManager.cameras.values().forEach(thread -> thread.setTargetFps(fps));
        }
        if (gameRenderHook != null) {
            gameRenderHook.setMainOutputFps(fps);
        }
    }

    public static boolean isUseCustomResolution() {
        return useCustomResolution;
    }

    private static KeyMapping newCameraKey, removeCameraMap, openSettingsKey;

    public static void init(){
        System.out.println("Starting Fabric NDI, loading NDI libraries.");
        try {
            Devolay.loadLibraries();
            ndiAvailable = true;
            System.out.println("NDI libraries loaded successfully.");
        } catch (Throwable e) {
            System.err.println("Failed to load NDI libraries: " + e.getMessage());
            System.err.println("NDI functionality will be disabled. This is likely because Devolay is not compiled for your OS/architecture.");
            ndiAvailable = false;
        }
        
        cameraManager = new CameraManager();
        String sourceName = "Player";
        if(Minecraft.getInstance().getUser() != null){
            sourceName = Minecraft.getInstance().getUser().getName();
        }
        if (ndiAvailable) {
            gameRenderHook = new GameRenderHook("MC - " + sourceName);
        }
        newCameraKey = new KeyMapping("keys.mcndi.new", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "NDI");
        removeCameraMap = new KeyMapping("keys.mcndi.remove", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, "NDI");
        openSettingsKey = new KeyMapping("keys.mcndi.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "NDI");
    }

    public static void handleKeybind(Minecraft instance) {
        if(openSettingsKey.consumeClick() && instance.screen == null){
            instance.setScreen(new NDISettingsScreen());
        } else if(newCameraKey.isDown() && instance.level != null && instance.player != null){
            createNewCamera();
            newCameraKey.setDown(false);
        } else if(removeCameraMap.isDown() && instance.level != null && instance.player != null){
            for(Entity ent : cameraManager.cameraEntities){
                instance.level.removeEntity(ent.getId(), Entity.RemovalReason.DISCARDED);
            }
        }
    }

    public static void createNewCamera() {
        Minecraft instance = Minecraft.getInstance();
        if (instance.level == null || instance.player == null) return;
        UUID uuid = UUID.randomUUID();
        CameraEntity armorStandEntity = new CameraEntity(instance.level, new GameProfile(uuid, uuid.toString()));
        armorStandEntity.setPos(instance.player.getX(), instance.player.getY(), instance.player.getZ());
        armorStandEntity.setPosRaw(instance.player.getX(), instance.player.getY(), instance.player.getZ());
        armorStandEntity.setYRot(instance.player.getYRot());
        armorStandEntity.setXRot(instance.player.getXRot());
        armorStandEntity.setYHeadRot(instance.player.yHeadRot);
        instance.level.addEntity(armorStandEntity);
        cameraManager.cameraEntities.add(armorStandEntity);
    }

    public static KeyMapping getNewCameraKey() {
        return newCameraKey;
    }

    public static KeyMapping getRemoveCameraMap() {
        return removeCameraMap;
    }

    public static KeyMapping getOpenSettingsKey() {
        return openSettingsKey;
    }
}
