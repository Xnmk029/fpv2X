package com.iung.fpv20.network;

import com.iung.fpv20.Fpv20;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class DroneFlyPacket implements FabricPacket {

    public static final PacketType<DroneFlyPacket> TYPE = PacketType.create(new Identifier(Fpv20.MOD_ID, "drone_fly_packet"), DroneFlyPacket::new);

    public boolean fly;
    public int frameIndex;
    public int controlMode; // 0 = Scheme B (Player Avatar), 1 = Scheme A (Remote Entity)
    public float cameraAngle;

    private DroneFlyPacket(PacketByteBuf buf) {
        this.fly = buf.readBoolean();
        this.frameIndex = buf.readInt();
        this.controlMode = buf.readInt();
        this.cameraAngle = buf.readFloat();
    }

    public DroneFlyPacket(boolean fly, int frameIndex, int controlMode, float cameraAngle) {
        this.fly = fly;
        this.frameIndex = frameIndex;
        this.controlMode = controlMode;
        this.cameraAngle = cameraAngle;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBoolean(this.fly);
        buf.writeInt(this.frameIndex);
        buf.writeInt(this.controlMode);
        buf.writeFloat(this.cameraAngle);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
