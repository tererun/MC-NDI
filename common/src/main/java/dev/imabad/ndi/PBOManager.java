package dev.imabad.ndi;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;

public class PBOManager {

    private static final int BYTES_PER_PIXEL = 4;
    private int pbos[];
    private int index = 0;
    private int nextIndex = 1;
    private final int width, height;

    public ByteBuffer buffer;
    public TextureTarget drawBuffer;

    public PBOManager(int width, int height)
    {
        this.width = width;
        this.height = height;
        buffer = BufferUtils.createByteBuffer(width * height * BYTES_PER_PIXEL);
        drawBuffer = new TextureTarget("ndi_pbo", width, height, true);
        initPbos(2);
    }

    private void initPbos(int count)
    {
        this.pbos = new int[count];
        for (int i = 0; i < pbos.length; i++)
        {
            this.pbos[i] = createPbo();
        }
    }

    private int createPbo()
    {
        int pbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbo);
        GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, width * height * BYTES_PER_PIXEL, GL15.GL_STREAM_READ);
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
        return pbo;
    }

    public void readPixelData(RenderTarget framebuffer)
    {
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        GlDevice device = (GlDevice) RenderSystem.getDevice();
        DirectStateAccess dsa = device.directStateAccess();
        
        int srcFbo = getFrameBufferId(framebuffer, dsa);
        int dstFbo = getFrameBufferId(drawBuffer, dsa);
        
        // Blit framebuffer with Y flip using direct OpenGL calls
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, srcFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dstFbo);
        GL30.glBlitFramebuffer(0, 0, framebuffer.viewWidth, framebuffer.viewHeight,
                               0, framebuffer.viewHeight, framebuffer.viewWidth, 0,
                               GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        
        GL30.glBindBuffer(GL30.GL_PIXEL_PACK_BUFFER, this.pbos[this.index]);
        int colorTextureId = getColorTextureId(drawBuffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTextureId);
        GL11.glGetTexImage(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, this.pbos[this.nextIndex]);
        buffer = GL15.glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER, GL15.GL_READ_ONLY, buffer);
        GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
        this.index = (this.index + 1) % 2;
        this.nextIndex = (this.index + 1) % 2;
    }

    public void cleanUp()
    {
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
        for (int pbo : this.pbos)
        {
            GL15.glDeleteBuffers(pbo);
        }
        drawBuffer.destroyBuffers();
    }
    
    private static int getFrameBufferId(RenderTarget target, DirectStateAccess dsa) {
        GpuTexture colorTexture = target.getColorTexture();
        GpuTexture depthTexture = target.getDepthTexture();
        if (colorTexture instanceof GlTexture glTexture) {
            return glTexture.getFbo(dsa, depthTexture);
        }
        throw new RuntimeException("Failed to get frameBufferId: colorTexture is not GlTexture");
    }
    
    private static int getColorTextureId(RenderTarget target) {
        GpuTexture colorTexture = target.getColorTexture();
        if (colorTexture instanceof GlTexture glTexture) {
            return glTexture.glId();
        }
        throw new RuntimeException("Failed to get colorTextureId: colorTexture is not GlTexture");
    }
}
