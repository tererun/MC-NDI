package dev.imabad.ndi;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
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

    public static CameraManager getCameraManager() {
        return cameraManager;
    }
    public static GameRenderHook getGameRenderHook() { return gameRenderHook; }
    public static boolean isNdiAvailable() { return ndiAvailable; }
    private static KeyMapping newCameraKey, removeCameraMap;

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
    }

    public static void handleKeybind(Minecraft instance) {
        if(newCameraKey.isDown() && instance.level != null && instance.player != null){
            UUID uuid = UUID.randomUUID();
            CameraEntity armorStandEntity = new CameraEntity(instance.level, new GameProfile(uuid, uuid.toString()));
            armorStandEntity.setPos(instance.player.getX(), instance.player.getY(), instance.player.getZ());
            armorStandEntity.setPosRaw(instance.player.getX(), instance.player.getY(), instance.player.getZ());
            armorStandEntity.setYRot(instance.player.getYRot());
            armorStandEntity.setXRot(instance.player.getXRot());
            armorStandEntity.setYHeadRot(instance.player.yHeadRot);
            instance.level.addEntity(armorStandEntity);
            newCameraKey.setDown(false);
            cameraManager.cameraEntities.add(armorStandEntity);
        } else if(removeCameraMap.isDown() && instance.level != null && instance.player != null){
            for(Entity ent : cameraManager.cameraEntities){
                instance.level.removeEntity(ent.getId(), Entity.RemovalReason.DISCARDED);
            }
        }
    }

    public static KeyMapping getNewCameraKey() {
        return newCameraKey;
    }

    public static KeyMapping getRemoveCameraMap() {
        return removeCameraMap;
    }
}
