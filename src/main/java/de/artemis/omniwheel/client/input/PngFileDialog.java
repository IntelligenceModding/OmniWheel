package de.artemis.omniwheel.client.input;

import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class PngFileDialog {
    private PngFileDialog() {}

    public static CompletableFuture<Path> open() {
        var result = new CompletableFuture<Path>();
        var name = MemoryUtil.memUTF8("PNG images");
        var pattern = MemoryUtil.memUTF8("png");
        var filters = SDL_DialogFileFilter.calloc(1).name(name).pattern(pattern);
        var callback = new SDL_DialogFileCallback[1];
        callback[0] = SDL_DialogFileCallback.create((userdata, filelist, filter) -> {
            try {
                if (filelist == MemoryUtil.NULL) {
                    result.completeExceptionally(new IllegalStateException(SDLError.SDL_GetError()));
                } else {
                    long path = MemoryUtil.memGetAddress(filelist);
                    result.complete(path == MemoryUtil.NULL ? null : Path.of(MemoryUtil.memUTF8(path)));
                }
            } catch (Exception exception) {
                result.completeExceptionally(exception);
            } finally {
                Minecraft.getInstance().execute(() -> {
                    callback[0].free();
                    filters.free();
                    MemoryUtil.memFree(name);
                    MemoryUtil.memFree(pattern);
                });
            }
        });
        SDLDialog.SDL_ShowOpenFileDialog(callback[0], MemoryUtil.NULL,
                Minecraft.getInstance().getWindow().handle(), filters, "", false);
        return result;
    }
}
