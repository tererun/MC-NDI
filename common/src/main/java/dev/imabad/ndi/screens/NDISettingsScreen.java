package dev.imabad.ndi.screens;

import dev.imabad.ndi.CameraEntity;
import dev.imabad.ndi.NDIMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NDISettingsScreen extends Screen {

    private EditBox widthField;
    private EditBox heightField;
    private EditBox fpsField;
    private int scrollOffset = 0;
    private UUID selectedCameraId = null;
    private CameraEntity glowingCamera = null;
    
    private static final int LIST_ENTRY_HEIGHT = 24;
    private static final int MAX_VISIBLE_ENTRIES = 6;

    public NDISettingsScreen() {
        super(Component.literal("NDI Settings"));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int startY = 40;

        // Resolution settings
        this.widthField = new EditBox(this.font, centerX - 120, startY, 60, 20, Component.literal("Width"));
        this.widthField.setValue(String.valueOf(NDIMod.getOutputWidth()));
        this.widthField.setMaxLength(5);
        this.addWidget(this.widthField);

        this.heightField = new EditBox(this.font, centerX - 50, startY, 60, 20, Component.literal("Height"));
        this.heightField.setValue(String.valueOf(NDIMod.getOutputHeight()));
        this.heightField.setMaxLength(5);
        this.addWidget(this.heightField);

        this.fpsField = new EditBox(this.font, centerX + 20, startY, 40, 20, Component.literal("FPS"));
        this.fpsField.setValue(String.valueOf(NDIMod.getOutputFps()));
        this.fpsField.setMaxLength(3);
        this.addWidget(this.fpsField);

        // Apply button
        this.addRenderableWidget(Button.builder(Component.literal("Apply"), this::onApplySettings)
                .bounds(centerX + 70, startY, 50, 20)
                .build());

        // New Camera button
        this.addRenderableWidget(Button.builder(Component.literal("+ New Camera"), this::onNewCamera)
                .bounds(centerX - 120, startY + 30, 100, 20)
                .build());

        // Remove All button
        this.addRenderableWidget(Button.builder(Component.literal("Remove All"), this::onRemoveAll)
                .bounds(centerX + 20, startY + 30, 100, 20)
                .build());
    }

    private void onApplySettings(Button button) {
        try {
            int w = Integer.parseInt(this.widthField.getValue());
            int h = Integer.parseInt(this.heightField.getValue());
            int fps = Integer.parseInt(this.fpsField.getValue());
            if (w > 0 && h > 0 && fps > 0 && fps <= 120) {
                NDIMod.setOutputResolution(w, h);
                NDIMod.setOutputFps(fps);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void onNewCamera(Button button) {
        if (this.minecraft != null && this.minecraft.level != null && this.minecraft.player != null) {
            NDIMod.createNewCamera();
        }
    }

    private void onRemoveAll(Button button) {
        if (this.minecraft != null && this.minecraft.level != null) {
            clearGlow();
            List<CameraEntity> cameras = new ArrayList<>(NDIMod.getCameraManager().cameraEntities);
            for (CameraEntity camera : cameras) {
                this.minecraft.level.removeEntity(camera.getId(), Entity.RemovalReason.DISCARDED);
            }
            selectedCameraId = null;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle widget clicks first (EditBox, Buttons)
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Check camera list clicks
        int listX = this.width / 2 - 120;
        int listY = 100;
        int listWidth = 240;

        List<CameraEntity> cameras = new ArrayList<>(NDIMod.getCameraManager().cameraEntities);
        int visibleCount = Math.min(cameras.size() - scrollOffset, MAX_VISIBLE_ENTRIES);

        for (int i = 0; i < visibleCount; i++) {
            int entryY = listY + i * LIST_ENTRY_HEIGHT;
            CameraEntity camera = cameras.get(i + scrollOffset);

            // Delete button (check first, rightmost)
            int delBtnX = listX + listWidth - 35;
            if (mouseX >= delBtnX && mouseX <= delBtnX + 35 && mouseY >= entryY + 2 && mouseY <= entryY + 18) {
                deleteCamera(camera);
                return true;
            }

            // Teleport button
            int tpBtnX = listX + listWidth - 75;
            if (mouseX >= tpBtnX && mouseX <= tpBtnX + 35 && mouseY >= entryY + 2 && mouseY <= entryY + 18) {
                teleportToCamera(camera);
                return true;
            }

            // Camera name area (selection)
            if (mouseX >= listX && mouseX < tpBtnX && mouseY >= entryY && mouseY <= entryY + LIST_ENTRY_HEIGHT - 2) {
                if (selectedCameraId != null && selectedCameraId.equals(camera.getUUID())) {
                    clearGlow();
                    selectedCameraId = null;
                } else {
                    clearGlow();
                    selectedCameraId = camera.getUUID();
                    setGlow(camera);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<CameraEntity> cameras = new ArrayList<>(NDIMod.getCameraManager().cameraEntities);
        int maxScroll = Math.max(0, cameras.size() - MAX_VISIBLE_ENTRIES);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) scrollY));
        return true;
    }

    private void setGlow(CameraEntity camera) {
        camera.setGlowingTag(true);
        glowingCamera = camera;
    }

    private void clearGlow() {
        if (glowingCamera != null) {
            glowingCamera.setGlowingTag(false);
            glowingCamera = null;
        }
    }

    private void teleportToCamera(CameraEntity camera) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.setPos(camera.getX(), camera.getY(), camera.getZ());
            this.minecraft.player.setYRot(camera.getYRot());
            this.minecraft.player.setXRot(camera.getXRot());
        }
    }

    private void deleteCamera(CameraEntity camera) {
        if (this.minecraft != null && this.minecraft.level != null) {
            if (selectedCameraId != null && selectedCameraId.equals(camera.getUUID())) {
                clearGlow();
                selectedCameraId = null;
            }
            this.minecraft.level.removeEntity(camera.getId(), Entity.RemovalReason.DISCARDED);
        }
    }

    @Override
    public void removed() {
        super.removed();
        clearGlow();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        // Title
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);

        // Labels
        guiGraphics.drawString(this.font, "Width:", centerX - 120, 30, 0xAAAAAA);
        guiGraphics.drawString(this.font, "Height:", centerX - 50, 30, 0xAAAAAA);
        guiGraphics.drawString(this.font, "FPS:", centerX + 20, 30, 0xAAAAAA);

        // Render text fields
        this.widthField.render(guiGraphics, mouseX, mouseY, delta);
        this.heightField.render(guiGraphics, mouseX, mouseY, delta);
        this.fpsField.render(guiGraphics, mouseX, mouseY, delta);

        // Camera list header
        guiGraphics.drawString(this.font, "Cameras:", centerX - 120, 85, 0xFFFFFF);

        // Camera list
        int listX = centerX - 120;
        int listY = 100;
        int listWidth = 240;
        int listHeight = MAX_VISIBLE_ENTRIES * LIST_ENTRY_HEIGHT;

        // List background
        guiGraphics.fill(listX - 2, listY - 2, listX + listWidth + 2, listY + listHeight + 2, 0x80000000);

        List<CameraEntity> cameras = new ArrayList<>(NDIMod.getCameraManager().cameraEntities);
        int visibleCount = Math.min(cameras.size() - scrollOffset, MAX_VISIBLE_ENTRIES);

        for (int i = 0; i < visibleCount; i++) {
            CameraEntity camera = cameras.get(i + scrollOffset);
            int entryY = listY + i * LIST_ENTRY_HEIGHT;
            boolean isSelected = selectedCameraId != null && selectedCameraId.equals(camera.getUUID());

            // Entry background
            int bgColor = isSelected ? 0x80404080 : 0x40202020;
            guiGraphics.fill(listX, entryY, listX + listWidth - 80, entryY + LIST_ENTRY_HEIGHT - 2, bgColor);

            // Camera name
            String name = camera.getDisplayName().getString();
            if (name.length() > 20) {
                name = name.substring(0, 17) + "...";
            }
            guiGraphics.drawString(this.font, name, listX + 4, entryY + 6, isSelected ? 0xFFFF55 : 0xFFFFFF);

            // Teleport button
            int tpBtnX = listX + listWidth - 75;
            boolean tpHover = mouseX >= tpBtnX && mouseX <= tpBtnX + 35 && mouseY >= entryY + 2 && mouseY <= entryY + 18;
            guiGraphics.fill(tpBtnX, entryY + 2, tpBtnX + 35, entryY + 18, tpHover ? 0xFF4488FF : 0xFF2266AA);
            guiGraphics.drawCenteredString(this.font, "TP", tpBtnX + 17, entryY + 5, 0xFFFFFF);

            // Delete button
            int delBtnX = listX + listWidth - 35;
            boolean delHover = mouseX >= delBtnX && mouseX <= delBtnX + 35 && mouseY >= entryY + 2 && mouseY <= entryY + 18;
            guiGraphics.fill(delBtnX, entryY + 2, delBtnX + 35, entryY + 18, delHover ? 0xFFFF4444 : 0xFFAA2222);
            guiGraphics.drawCenteredString(this.font, "X", delBtnX + 17, entryY + 5, 0xFFFFFF);
        }

        if (cameras.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "No cameras", centerX, listY + 30, 0x888888);
        }

        // Scroll indicator
        if (cameras.size() > MAX_VISIBLE_ENTRIES) {
            int scrollBarHeight = (int) ((float) MAX_VISIBLE_ENTRIES / cameras.size() * listHeight);
            int scrollBarY = listY + (int) ((float) scrollOffset / (cameras.size() - MAX_VISIBLE_ENTRIES) * (listHeight - scrollBarHeight));
            guiGraphics.fill(listX + listWidth + 4, scrollBarY, listX + listWidth + 8, scrollBarY + scrollBarHeight, 0xFFAAAAAA);
        }

        // Help text
        guiGraphics.drawCenteredString(this.font, "Click camera to select/glow | TP=Teleport | X=Delete", centerX, this.height - 20, 0x888888);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
