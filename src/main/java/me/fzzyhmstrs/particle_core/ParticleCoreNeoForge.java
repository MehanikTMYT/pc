package me.fzzyhmstrs.particle_core;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Fleet build (Java, no Kotlin for Forge / Fzzy Config): client config via ModConfigSpec. */
@Mod(value = "particle_core", dist = Dist.CLIENT)
public final class ParticleCoreNeoForge {
    public ParticleCoreNeoForge(ModContainer container, IEventBus bus) {
        container.registerConfig(ModConfig.Type.CLIENT, PcConfigImpl.SPEC, "particle_core-client.toml");
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        bus.addListener(ModConfigEvent.Loading.class, e -> { if (e.getConfig().getSpec() == PcConfigImpl.SPEC) PcConfig.INSTANCE.getImpl().onLoad(); });
        bus.addListener(ModConfigEvent.Reloading.class, e -> { if (e.getConfig().getSpec() == PcConfigImpl.SPEC) PcConfig.INSTANCE.getImpl().onLoad(); });
        PcConfig.INSTANCE.init();
    }
}
