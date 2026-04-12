package xyz.pyrehaven.whostoppedthemusic;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;

@Mod(WhoStoppedTheMusicNeoForge.MOD_ID)
public class WhoStoppedTheMusicNeoForge {
    public static final String MOD_ID = "whostoppedthemusic";
    private static final Logger LOGGER = LogUtils.getLogger();

    public WhoStoppedTheMusicNeoForge() {
        LOGGER.info("Who Stopped The Music (NeoForge) loaded");
    }

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientHooks {
        private static Field nextSongDelayField;
        private static boolean reflectionReady = false;
        private static boolean reflectionFailed = false;

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (reflectionFailed) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) return;

            try {
                MusicManager manager = mc.getMusicManager();
                if (!reflectionReady) {
                    nextSongDelayField = MusicManager.class.getDeclaredField("nextSongDelay");
                    nextSongDelayField.setAccessible(true);
                    reflectionReady = true;
                }

                int delay = nextSongDelayField.getInt(manager);
                if (delay > 0) {
                    nextSongDelayField.setInt(manager, 0);
                }
            } catch (Throwable t) {
                reflectionFailed = true;
                LOGGER.error("Failed to hook MusicManager for always-on music", t);
            }
        }
    }
}
