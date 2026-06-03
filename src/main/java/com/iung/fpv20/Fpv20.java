package com.iung.fpv20;

import com.iung.fpv20.blocks.ReceiverBlockEntity;
import com.iung.fpv20.config.Fpv20ConfigCommon;
import com.iung.fpv20.consts.ModBlocks;
import com.iung.fpv20.consts.ModItemGroups;
import com.iung.fpv20.consts.ScreenHandlers;
import com.iung.fpv20.entity.DroneEntity;
import com.iung.fpv20.globals.AllChannels;
import com.iung.fpv20.mixin_utils.IsFlying;
import com.iung.fpv20.network.ChannelUpdatePacket;
import com.iung.fpv20.network.DroneControlPacket;
import com.iung.fpv20.network.DroneFlyPacket;
import com.iung.fpv20.network.SetReceiverPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fpv20 implements ModInitializer {
    public static final String MOD_ID = "fpv20";

    public static Fpv20ConfigCommon config = Fpv20ConfigCommon.createAndLoad();

    public static final EntityType<DroneEntity> DRONE_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MOD_ID, "drone"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, DroneEntity::new)
                    .dimensions(EntityDimensions.fixed(0.5f, 0.2f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(1)
                    .build()
    );

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    @Override
    public void onInitialize() {

        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        if (config.client_only) {
            return;
        }
//        LOGGER.info("Hello Fabric world!");
        ModBlocks.registerModBlocks();
        ModItemGroups.registerItemGroups();
        ScreenHandlers.reg();
        ServerPlayNetworking.registerGlobalReceiver(ChannelUpdatePacket.TYPE, (packet, player, responseSender) -> {
            AllChannels.ALL_CHANNELS.put(packet.name, new AllChannels.ChannelInfo(packet.value));
        });

        ServerPlayNetworking.registerGlobalReceiver(SetReceiverPacket.TYPE, (packet, player, responseSender) -> {
            BlockEntity be = player.getWorld().getBlockEntity(packet.pos);
            if (!(be instanceof ReceiverBlockEntity r)) {
                return;
            }
            r.channel = packet.channel_name;
            r.neg = packet.neg;
            player.getWorld().markDirty(packet.pos);

        });


        ServerTickEvents.START_SERVER_TICK.register((e) -> {
//            LOGGER.info("{}", AllChannels.ALL_CHANNELS);

        });

        ServerLifecycleEvents.SERVER_STOPPING.register(e -> config.save());

        ServerPlayNetworking.registerGlobalReceiver(DroneFlyPacket.TYPE, (packet, player, responseSender) -> {
            player.getServer().execute(() -> {
                if (packet.fly) {
                    boolean f = player.getAbilities().invulnerable;
                    ((IsFlying) player).set_obj(f);
                    player.getAbilities().invulnerable = true;

                    if (packet.controlMode == 1) {
                        // Scheme A: Spawn a separate DroneEntity
                        // 1. Clean up old drones piloted by this player
                        for (net.minecraft.entity.Entity oldDrone : player.getServerWorld().getEntitiesByType(com.iung.fpv20.Fpv20.DRONE_ENTITY_TYPE, e -> player.getUuid().equals(((DroneEntity) e).getPilotUuid()))) {
                            oldDrone.discard();
                        }
                        // 2. Spawn new DroneEntity at player's head/position
                        DroneEntity drone = new DroneEntity(com.iung.fpv20.Fpv20.DRONE_ENTITY_TYPE, player.getWorld());
                        drone.setPosition(player.getX(), player.getEyeY(), player.getZ());
                        drone.setPilotUuid(player.getUuid());
                        drone.setFrameIndex(packet.frameIndex);
                        drone.setCameraAngle(packet.cameraAngle);
                        player.getWorld().spawnEntity(drone);
                        Fpv20.LOGGER.info("Spawned separate DroneEntity for pilot " + player.getName().getString() + " (ID: " + drone.getId() + ")");
                    }
                } else {
                    Object cached = ((IsFlying) player).get_obj();
                    player.getAbilities().invulnerable = (cached instanceof Boolean) ? (Boolean) cached : player.isCreative();
                    
                    // Scheme A cleanup: Discard any drone entities piloted by this player
                    for (net.minecraft.entity.Entity oldDrone : player.getServerWorld().getEntitiesByType(com.iung.fpv20.Fpv20.DRONE_ENTITY_TYPE, e -> player.getUuid().equals(((DroneEntity) e).getPilotUuid()))) {
                        oldDrone.discard();
                    }
                }
                ((IsFlying) player).set_frame_index(packet.frameIndex);
                ((IsFlying) player).set_is_flying(packet.fly);
                Fpv20.LOGGER.info("updated fly state to: " + packet.fly + " with frame index: " + packet.frameIndex + " mode: " + packet.controlMode);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(DroneControlPacket.TYPE, (packet, player, responseSender) -> {
            player.getServer().execute(() -> {
                net.minecraft.entity.Entity entity = player.getWorld().getEntityById(packet.entityId);
                if (entity instanceof DroneEntity drone) {
                    if (player.getUuid().equals(drone.getPilotUuid())) {
                        org.joml.Quaternionf q = new org.joml.Quaternionf(packet.qx, packet.qy, packet.qz, packet.qw);
                        org.joml.Vector3f x_y_z = q.getEulerAnglesZXY(new org.joml.Vector3f());
                        float yaw = (float) Math.toDegrees(x_y_z.y) + 180.0f;
                        float pitch = (float) Math.toDegrees(x_y_z.x);
                        drone.refreshPositionAndAngles(packet.x, packet.y, packet.z, yaw, pitch);
                        drone.setQuaternion(packet.qx, packet.qy, packet.qz, packet.qw);
                        drone.setVelocity(packet.vx, packet.vy, packet.vz);
                    }
                }
            });
        });

    }
}