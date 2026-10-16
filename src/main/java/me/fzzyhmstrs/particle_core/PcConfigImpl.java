package me.fzzyhmstrs.particle_core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Java port of the Fzzy-Config {@code PcConfigImpl}: same options and semantics, stored in
 * {@code config/particle_core-client.toml} through ModConfigSpec. Getter names are kept for the mixins.
 */
public final class PcConfigImpl {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    static final ModConfigSpec.BooleanValue DISABLE_PARTICLES = B.comment("Completely turns off all particles on the client.")
        .define("disableParticles", false);
    static final ModConfigSpec.ConfigValue<List<? extends String>> BY_TYPE = B
        .comment("Spawn chance per particle type, \"<particle id>=<0.0..1.0>\", e.g. \"minecraft:smoke=0.3\".")
        .defineListAllowEmpty("byTypeReductions", List.of(), () -> "minecraft:smoke=1.0", PcConfigImpl::validEntry);
    static final ModConfigSpec.IntValue MAX_PER_SHEET = B.comment("Maximum particles per render sheet (restart required).")
        .gameRestart().defineInRange("maxParticlesPerSheet", 16384, 0, Integer.MAX_VALUE);
    static final ModConfigSpec.DoubleValue RENDER_DISTANCE_MULTIPLIER = B.comment("Particle render distance as a fraction of the render distance.")
        .defineInRange("particleRenderDistanceMultiplier", 1.0, 0.1, 1.0);
    static final ModConfigSpec.BooleanValue ASYNC = B.comment("Tick particles asynchronously (turns itself off on concurrency problems).")
        .define("asynchronousTicking", false);
    static final ModConfigSpec.DoubleValue REDUCE_ALL = B.comment("Chance to treat ALL particle setting as DECREASED.")
        .defineInRange("reduceAllChance", 0.0, 0.0, 1.0);
    static final ModConfigSpec.DoubleValue REDUCE_DECREASED = B.comment("Chance to treat DECREASED particle setting as MINIMAL.")
        .defineInRange("reduceDecreasedChance", 0.0, 0.0, 1.0);
    static final ModConfigSpec.ConfigValue<List<? extends String>> POTION = B
        .comment("Potion particles to hide: NONE, OTHER_PLAYER, SELF, MOBS or ALL (ALL wins, NONE only alone).")
        .defineListAllowEmpty("turnOffPotionParticles", List.of("NONE"), () -> "NONE", PcConfigImpl::validPotion);
    static final ModConfigSpec SPEC = B.build();

    private volatile Map<Identifier, Double> byType = Map.of();
    private final Setting<Boolean> disableParticles = new Setting<>(DISABLE_PARTICLES);
    private final Setting<Integer> maxParticlesPerSheet = new Setting<>(MAX_PER_SHEET);
    private final Setting<Boolean> asynchronousTicking = new Setting<>(ASYNC);

    /** Minimal stand-in for Fzzy's ValidatedField API used by the mixins: {@code get()} and {@code validateAndSet()}. */
    public static final class Setting<T> {
        private final ModConfigSpec.ConfigValue<T> value;
        private volatile T override;

        Setting(ModConfigSpec.ConfigValue<T> value) {
            this.value = value;
        }

        public T get() {
            T o = override;
            if (o != null) return o;
            return SPEC.isLoaded() ? value.get() : value.getDefault();
        }

        /** Runtime change (e.g. async ticking turning itself off); persisted when the config is loaded. */
        public void validateAndSet(T newValue) {
            override = newValue;
            if (SPEC.isLoaded()) {
                value.set(newValue);
                value.save();
                override = null;
            }
        }
    }

    public Setting<Boolean> getDisableParticles() {
        return disableParticles;
    }

    public Setting<Integer> getMaxParticlesPerSheet() {
        return maxParticlesPerSheet;
    }

    public Setting<Boolean> getAsynchronousTicking() {
        return asynchronousTicking;
    }

    void onLoad() {
        Map<Identifier, Double> map = new HashMap<>();
        for (String entry : BY_TYPE.get()) {
            int eq = entry.lastIndexOf('=');
            Identifier id = Identifier.tryParse(entry.substring(0, eq).trim());
            if (id != null) map.put(id, Mth.clamp(Double.parseDouble(entry.substring(eq + 1).trim()), 0.0, 1.0));
        }
        byType = Map.copyOf(map);
        List<? extends String> choices = POTION.get();
        int flags = 0;
        if (choices.contains("ALL")) {
            flags = PcConfig.PotionDisableType.ALL.flags;
        } else {
            for (String c : choices) if (!c.equals("NONE")) flags |= PcConfig.PotionDisableType.valueOf(c).flags;
            if (flags == 0) flags = PcConfig.PotionDisableType.NONE.flags;
        }
        PcConfig.INSTANCE.choiceFlags = flags;
    }

    public void setupParticleViewDistance() {
        double multiplier = SPEC.isLoaded() ? RENDER_DISTANCE_MULTIPLIER.get() : 1.0;
        PcConfig.INSTANCE.renderDistance = Mth.square(Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 * multiplier);
    }

    public boolean shouldSpawnParticle(ParticleType<?> type) {
        Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
        if (id == null) return true;
        Double chance = byType.get(id);
        return chance == null || PcUtils.random.nextDouble() < chance;
    }

    public ParticleStatus getReducedParticleSpawnType(ParticleStatus mode) {
        ParticleStatus out = mode;
        if (out == ParticleStatus.ALL && PcUtils.random.nextFloat() < (SPEC.isLoaded() ? REDUCE_ALL.get() : 0.0)) out = ParticleStatus.DECREASED;
        if (out == ParticleStatus.DECREASED && PcUtils.random.nextFloat() < (SPEC.isLoaded() ? REDUCE_DECREASED.get() : 0.0)) out = ParticleStatus.MINIMAL;
        return out;
    }

    private static boolean validEntry(Object o) {
        if (!(o instanceof String s)) return false;
        int eq = s.lastIndexOf('=');
        if (eq <= 0 || Identifier.tryParse(s.substring(0, eq).trim()) == null) return false;
        try {
            double d = Double.parseDouble(s.substring(eq + 1).trim());
            return d >= 0.0 && d <= 1.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean validPotion(Object o) {
        if (!(o instanceof String s)) return false;
        for (PcConfig.PotionDisableType t : PcConfig.PotionDisableType.values()) if (t.name().equals(s)) return true;
        return false;
    }
}
