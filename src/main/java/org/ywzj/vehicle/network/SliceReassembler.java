package org.ywzj.vehicle.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.network.message.ServerSlicedPacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SliceReassembler {
    private static final Map<UUID, Collector> collectors = new ConcurrentHashMap<>();
    public static final int SLICE_SIZE = 256 * 1024; // 默认一个切片最大256KB

    public static void receiveSlice(UUID id, int index, int total, byte[] data) {
        Collector c = collectors.computeIfAbsent(id, k -> new Collector(total));
        if (c.addPart(index, data)) {
            FriendlyByteBuf buf = c.assembleToBuffer();
            collectors.remove(id);
            CommonAssetsManager.fromNetwork(buf);
        }
    }

    public static ServerSlicedPacket[] sliceData(FriendlyByteBuf buf) {
        int size = SLICE_SIZE;
        int length = buf.readableBytes();
        if (length == 0) {
            UUID id = UUID.randomUUID();
            return new ServerSlicedPacket[] { new ServerSlicedPacket(id, 0, 1, new byte[0]) };
        }

        int parts = (length + size - 1) / size;
        UUID id = UUID.randomUUID();
        ServerSlicedPacket[] packets = new ServerSlicedPacket[parts];

        int baseIndex = buf.readerIndex();
        for (int i = 0; i < parts; i++) {
            int offset = i * size;
            int len = Math.min(size, length - offset);
            byte[] slice = new byte[len];
            buf.getBytes(baseIndex + offset, slice);
            packets[i] = new ServerSlicedPacket(id, i, parts, slice);
        }
        return packets;
    }

    public static FriendlyByteBuf toBuffer(byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBytes(data);
        return buf;
    }

    private static class Collector {
        final int total;
        final byte[][] parts;
        int received = 0;

        Collector(int total) {
            this.total = total;
            this.parts = new byte[total][];
        }

        // return true when all parts received
        synchronized boolean addPart(int idx, byte[] part) {
            if (idx < 0 || idx >= total) return false;
            if (parts[idx] == null) {
                parts[idx] = part;
                received++;
            }
            return received == total;
        }

        synchronized FriendlyByteBuf assembleToBuffer() {
            for (int i = 0; i < total; i++) {
                if (parts[i] == null) {
                    return new FriendlyByteBuf(Unpooled.buffer(0));
                }
            }
            int totalLen = 0;
            for (int i = 0; i < total; i++) totalLen += parts[i].length;

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(totalLen));
            for (int i = 0; i < total; i++) {
                buf.writeBytes(parts[i]);
            }
            return buf;
        }
    }
}
