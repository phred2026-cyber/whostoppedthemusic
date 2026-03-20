package xyz.pyrehaven.whostoppedthemusic.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicTracker.class)
public class MusicManagerMixin {

    @Shadow
    private int timeUntilNextSong;

    @Shadow
    private SoundInstance current;

    @Shadow
    private MinecraftClient client;

    /**
     * Zero out the inter-track delay so music resumes immediately
     * instead of waiting vanilla's random gap (up to ~2.75 min).
     * Only fires when no music is currently playing.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void whostm_zeroDelay(CallbackInfo ci) {
        if (this.current == null || !this.client.getSoundManager().isPlaying(this.current)) {
            if (this.timeUntilNextSong > 0) {
                this.timeUntilNextSong = 0;
            }
        }
    }

    /**
     * Prevent stop() from cutting songs short.
     * We cancel the no-arg stop() only if music is actively playing.
     * Typed stop(MusicSound) (for music type changes) is NOT cancelled —
     * that lets vanilla switch tracks correctly (e.g. entering a biome).
     */
    @Inject(method = "stop()V", at = @At("HEAD"), cancellable = true)
    private void whostm_preventStop(CallbackInfo ci) {
        if (this.current != null && this.client.getSoundManager().isPlaying(this.current)) {
            ci.cancel();
        }
    }
}
