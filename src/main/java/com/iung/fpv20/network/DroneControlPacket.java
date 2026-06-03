package com.iung.fpv20.network;

import com.iung.fpv20.Fpv20;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class DroneControlPacket implements FabricPacket {

    public static final PacketType<DroneControlPacket> TYPE = PacketType.create(new Identifier(Fpv20.MOD_ID, "drone_control_packet"), DroneControlPacket::new);

    public int entityId;
    public double x;
    public double y;
    public double z;
    public double vx;
    public double vy;
    public double vz;
    public float qx;
    public float qy;
    public float qz;
    public float qw;

    public DroneControlPacket(int entityId, double x, double y, double z, double vx, double vy, double vz, float qx, float qy, float qz, float qw) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.qx = qx;
        this.qy = qy;
        this.qz = qz;
        this.qw = qw;
    }

    private DroneControlPacket(PacketByteBuf buf) {
        this.entityId = buf.readInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.vx = buf.readDouble();
        this.vy = buf.readDouble();
        this.vz = buf.readDouble();
        this.qx = buf.readFloat();
        this.qy = buf.readFloat();
        this.qz = buf.readFloat();
        this.qw = buf.readFloat();
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeDouble(this.vx);
        buf.writeDouble(this.vy);
        buf.writeDouble(this.vz);
        buf.writeFloat(this.qx);
        buf.writeFloat(this.qy);
        buf.writeFloat(this.qz);
        buf.writeFloat(this.qw);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
