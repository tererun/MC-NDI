package dev.imabad.ndi;

import com.mojang.authlib.GameProfile;
import dev.imabad.ndi.threads.NDIControlThread;
import dev.imabad.ndi.threads.NDIThread;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CameraManager {


    public ConcurrentSet<CameraEntity> cameraEntities;
    public ConcurrentHashMap<UUID, NDIThread> cameras;
    public ConcurrentHashMap<UUID, NDIControlThread> cameraControls;

    public CameraManager() {
        cameras = new ConcurrentHashMap<>();
        cameraControls = new ConcurrentHashMap<>();
        cameraEntities = new ConcurrentSet<>();
    }

    public void save(File runDirectory, LocalPlayer player, boolean isIntegratedServer, LevelStorageSource levelStorage, IntegratedServer server){
        if(isIntegratedServer && server == null){
            return;
        } else if(player == null || player.connection == null){
            return;
        }
        CompoundTag allEntities = new CompoundTag();
        cameraEntities.forEach(cameraEntity -> {
            CompoundTag compoundTag = cameraEntity.getTag();
            allEntities.put(cameraEntity.getStringUUID(), compoundTag);
        });
        File serversFolder = new File(runDirectory, "cameras");
        File saveFile;
        if(!isIntegratedServer){
            if(!serversFolder.exists()){
                serversFolder.mkdir();
            }
            String ip = player.connection.getConnection().getRemoteAddress().toString() ;
            ip = ip.substring(ip.indexOf("/") + 1);
            ip = ip.substring(0, ip.indexOf(":"));
            saveFile = new File(serversFolder, ip + ".dat");
        } else {
            saveFile = new File(server.getWorldPath(LevelResource.ROOT).toFile(), "cameras.dat");
        }
        try {
            NbtIo.writeCompressed(allEntities, saveFile.toPath());
        } catch (Exception exception) {
            System.out.println("Error occurred whilst saving cameras " + exception);
        }
    }

    public void load(File runDirectory, boolean isIntegratedServerRunning, IntegratedServer server, LevelStorageSource levelStorage, ClientLevel world, ClientPacketListener clientPlayNetworkHandler){
        File serversFolder = new File(runDirectory, "cameras");
        File saveFile = new File(serversFolder, "temp.dat");
        if(!isIntegratedServerRunning && clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection() != null){
            if(serversFolder.exists()){
                String ip = clientPlayNetworkHandler.getConnection().getRemoteAddress().toString() ;
                ip = ip.substring(ip.indexOf("/") + 1);
                ip = ip.substring(0, ip.indexOf(":"));
                saveFile = new File(serversFolder, ip + ".dat");
            }
        } else if (server != null){
            saveFile = new File(server.getWorldPath(LevelResource.ROOT).toFile(), "cameras.dat");
        }
        CompoundTag compoundTag = null;

        try {
            if (saveFile.exists() && saveFile.isFile()) {
                compoundTag = NbtIo.readCompressed(saveFile.toPath(), NbtAccounter.unlimitedHeap());
            }
        } catch (Exception exception) {
            System.out.println("Error occurred whilst loading cameras " + exception);
        }
        if(compoundTag != null) {
            CompoundTag finalCompoundTag = compoundTag;
            compoundTag.keySet().forEach(s -> {
                CompoundTag compoundTag1 = (CompoundTag) finalCompoundTag.get(s);
                String uuid = compoundTag1.getString("uuid").orElse("");
                String name = compoundTag1.getString("name").orElse("");
                CameraEntity cameraEntity = new CameraEntity(world, new GameProfile(UUID.fromString(uuid), name));
                cameraEntity.cameraFromTag(compoundTag1);
                world.addEntity(cameraEntity);
                cameraEntities.add(cameraEntity);
            });
        }
    }
}
