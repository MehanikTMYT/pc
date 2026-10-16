package me.fzzyhmstrs.particle_core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java port of the Kotlin {@code object PcDisable}: reads {@code config/particle_core_disabled_optimizations_v2.json}
 * (same file and format) for the mixin plugin, which runs before mods are constructed.
 */
public final class PcDisable {
    public static final PcDisable INSTANCE = new PcDisable();
    private static final Logger LOGGER = LoggerFactory.getLogger("particle_core");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();

    private final DisabledOptimizations disabledOptimizations;

    private PcDisable() {
        DisabledOptimizations d;
        try {
            d = GSON.fromJson(read(), DisabledOptimizations.class);
            if (d == null || d.disableOptimizations == null) d = new DisabledOptimizations();
        } catch (Exception e) {
            LOGGER.error("Unexpected error encountered retrieving disabled mixins for Particle Core", e);
            d = new DisabledOptimizations();
        }
        disabledOptimizations = d;
    }

    public DisabledOptimizations getDisabledOptimizations() {
        return disabledOptimizations;
    }

    private static String read() {
        try {
            Path dir = FMLPaths.CONFIGDIR.get();
            Path file = dir.resolve("particle_core_disabled_optimizations_v2.json");
            if (Files.exists(file)) return Files.readString(file, StandardCharsets.UTF_8);
            Path old = dir.resolve("particle_core_disabled_optimizations.json");
            DisabledOptimizations config = new DisabledOptimizations();
            if (Files.exists(old)) {
                DisabledOptimizations previous = GSON.fromJson(Files.readString(old, StandardCharsets.UTF_8), DisabledOptimizations.class);
                if (previous != null && previous.disableOptimizations != null) config.disableOptimizations = previous.disableOptimizations;
                Files.delete(old);
            }
            String text = GSON.toJson(config);
            Files.writeString(file, text, StandardCharsets.UTF_8);
            return text;
        } catch (Exception e) {
            LOGGER.error("Unexpected error encountered preparing disabled mixin config for Particle Core", e);
            return "{\"disableOptimizations\": []}";
        }
    }

    public static final class DisabledOptimizations {
        Map<String, String> _Disable_Optimizations_Options = options();
        List<String> disableOptimizations = List.of("NONE");

        private static Map<String, String> options() {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("ROTATION", "[Impact: Medium] Disables mixins related to vertex rotation caching (ParticleManagerRotationMixin, BillboardParticleMixin)");
            m.put("CULLING", "[Impact: HIGH] Disables mixins related to particle culling (FrustumAccessor, ParticleAccessor, ParticleRendererFrustumMixin, WorldRendererFrustumMixin, ParticleFrustumBlacklistMixin)");
            m.put("TYPE", "[Impact: Low to Medium] Disables mixins related to particle disabling and reduction (ParticleManagerTypeMixin, ParticleManagerCreatorMixin, FireworksSparkParticleMixin)");
            m.put("DECREASE", "[Impact: Low] Disables mixins related particle settings reduction (ALL, DECREASED, MINIMAL) (ClientWorldDecreaseMixin)");
            m.put("LIGHTMAP", "[Impact: Medium] Disables mixins related to light map caching (ParticleManagerCachedLightMixin, ParticleBrightnessCacheMixin, ParticleCachePosMixin, ParticleRendererCachedPosMixin, ParticleRendererBrightnessTickMixin)");
            m.put("POTION", "[Impact: Low] Disables mixins related to potion particle disabling (LivingEntityMixin)");
            m.put("MOVE", "[Impact: Medium] Disables mixins related to particle movement optimization (ParticleMoveAdjustMixin, ParticleCachePosMixin, ParticleRendererCachedPosMixin)");
            m.put("VERTEX", "[Impact: HIGH] Disables mixins related to particle vertex drawing optimizations (BillboardParticleVertexMixin, BufferBuilderVertexMixin)");
            m.put("COUNT", "[Impact: Variable] Disables mixins related to max particle count setting (ParticleRendererCountMixin)");
            m.put("ASYNC", "[Impact: Medium] Disables asynchronous ticking of particles (ParticleManagerAsyncMixin, ParticleRendererAccessor)");
            m.put("RENDER_DISTANCE", "[Impact: Variable] Disables mixins that control max particle render distance (ParticleManagerRenderDistanceMixin, ParticleAccessor, ParticleRendererRenderDistanceMixin)");
            return m;
        }

        private boolean off(String key, String className, String... suffixes) {
            if (!disableOptimizations.contains(key)) return false;
            for (String s : suffixes) {
                if (className.endsWith(s)) {
                    LOGGER.info("Disabling [{}] due to '{}' key in particle core config!", className, key);
                    return true;
                }
            }
            return false;
        }

        public boolean shouldDisableMixin(String className) {
            return off("TYPE", className, "ParticleEngineTypeMixin", "ParticleEngineCreatorMixin", "FireworksSparkParticleMixin")
                || off("DECREASE", className, "ClientLevelDecreaseMixin")
                || off("LIGHTMAP", className, "ParticleEngineCachedLightMixin", "ParticleBrightnessCacheMixin")
                || off("POTION", className, "LivingEntityMixin")
                || off("MOVE", className, "ParticleMoveAdjustMixin")
                || (disableOptimizations.contains("MOVE") && off("LIGHTMAP", className, "ParticleCachePosMixin", "ParticleGroupCachedPosMixin"))
                || off("COUNT", className, "ParticleGroupCountMixin")
                || off("ASYNC", className, "ParticleEngineAsyncMixin", "ParticleGroupAccessor")
                || off("RENDER_DISTANCE", className, "ParticleGroupRenderDistanceMixin", "ParticleAccessor", "ParticleEngineRenderDistanceMixin");
        }
    }
}
