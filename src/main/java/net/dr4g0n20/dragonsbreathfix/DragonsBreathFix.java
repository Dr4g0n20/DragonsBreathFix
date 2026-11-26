package net.dr4g0n20.dragonsbreathfix;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ThreadLocalRandom;

public final class DragonsBreathFix extends JavaPlugin {

    // In the 1.12.2 play protocol, object type 3 is Area Effect Cloud.
    private static final int AREA_EFFECT_CLOUD_TYPE_ID = 3;

    private boolean hideCloudEntities;
    private boolean particlesEnabled;
    private boolean useCloudColour;
    private int particleInterval;
    private int particlesPerCloud;
    private double particleHeight;
    private BukkitTask particleTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null
                || !Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            getLogger().severe("ProtocolLib is missing or disabled. DragonsBreathFix cannot start.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (hideCloudEntities) {
            registerCloudPacketFilter();
        }

        if (particlesEnabled) {
            startParticleTask();
        }

        getLogger().info("Enabled. Server-side area effects are preserved; client cloud entities are hidden.");
    }

    private void loadSettings() {
        hideCloudEntities = getConfig().getBoolean("hide-cloud-entities", true);
        particlesEnabled = getConfig().getBoolean("particles.enabled", true);
        useCloudColour = getConfig().getBoolean("particles.use-cloud-colour", true);
        particleInterval = Math.max(1, getConfig().getInt("particles.interval-ticks", 4));
        particlesPerCloud = Math.max(0, getConfig().getInt("particles.count-per-cloud", 6));
        particleHeight = getConfig().getDouble("particles.height-offset", 0.15D);
    }

    private void registerCloudPacketFilter() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                this,
                ListenerPriority.NORMAL,
                PacketType.Play.Server.SPAWN_ENTITY
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    // The first integer is the entity ID; the second is the
                    // object type ID in the 1.12.2 SPAWN_ENTITY packet.
                    if (event.getPacket().getIntegers().read(1) == AREA_EFFECT_CLOUD_TYPE_ID) {
                        event.setCancelled(true);
                    }
                } catch (RuntimeException exception) {
                    getLogger().warning("Could not inspect an outgoing spawn-entity packet: "
                            + exception.getMessage());
                }
            }
        });
        getLogger().info("Registered outgoing AreaEffectCloud packet filter.");
    }

    private void startParticleTask() {
        particleTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (AreaEffectCloud cloud : world.getEntitiesByClass(AreaEffectCloud.class)) {
                        if (!cloud.isValid() || cloud.isDead()) {
                            continue;
                        }
                        displaySplashStyleParticles(cloud);
                    }
                }
            }
        }, particleInterval, particleInterval);
    }

    private void displaySplashStyleParticles(AreaEffectCloud cloud) {
        if (particlesPerCloud <= 0) {
            return;
        }

        Location centre = cloud.getLocation();
        World world = cloud.getWorld();
        double radius = Math.max(0.25D, cloud.getRadius());
        Color colour = useCloudColour ? cloud.getColor() : Color.WHITE;

        double red = colour.getRed() / 255.0D;
        double green = colour.getGreen() / 255.0D;
        double blue = colour.getBlue() / 255.0D;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < particlesPerCloud; i++) {
            double angle = random.nextDouble(0.0D, Math.PI * 2.0D);
            double distance = Math.sqrt(random.nextDouble()) * radius;
            double x = centre.getX() + Math.cos(angle) * distance;
            double z = centre.getZ() + Math.sin(angle) * distance;
            double y = centre.getY() + particleHeight;

            // SPELL_MOB is the coloured potion/splash particle supported by
            // the 1.12 client. A count of zero preserves the RGB offsets.
            world.spawnParticle(Particle.SPELL_MOB, x, y, z, 0,
                    red, green, blue, 1.0D);
        }
    }

    @Override
    public void onDisable() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        }
    }
}
