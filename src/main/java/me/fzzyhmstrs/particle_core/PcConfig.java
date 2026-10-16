package me.fzzyhmstrs.particle_core;

import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Java port of the Kotlin {@code object PcConfig}; call sites keep using {@code PcConfig.INSTANCE}. */
public final class PcConfig {
    public static final PcConfig INSTANCE = new PcConfig();

    private final Logger logger = LoggerFactory.getLogger("particle_core");
    private final PcConfigImpl impl = new PcConfigImpl();
    double renderDistance = 0.0;
    int choiceFlags = PotionDisableType.NONE.flags;

    private PcConfig() {}

    public void init() {
        logger.info("Particle Core initialized");
    }

    public Logger getLogger() {
        return logger;
    }

    public PcConfigImpl getImpl() {
        return impl;
    }

    public boolean shouldRenderParticle(double x, double y, double z, Vec3 pos) {
        return pos.distanceToSqr(x, y, z) <= renderDistance;
    }

    public boolean shouldDisablePotionParticle(PotionDisableType type) {
        return (choiceFlags & type.flags) == type.flags;
    }

    public enum PotionDisableType {
        NONE(1), OTHER_PLAYER(2), SELF(4), MOBS(8), ALL(16);

        public final int flags;

        PotionDisableType(int flags) {
            this.flags = flags;
        }
    }
}
