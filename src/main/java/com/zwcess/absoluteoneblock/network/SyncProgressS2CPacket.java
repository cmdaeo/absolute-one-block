package com.zwcess.absoluteoneblock.network;

import com.zwcess.absoluteoneblock.client.ClientOneBlockData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class SyncProgressS2CPacket {
    private final int blocksBroken;
    private final int blocksNeeded;
    private final String currentPhaseName;
    private final String nextPhaseName;

    public SyncProgressS2CPacket(int blocksBroken, int blocksNeeded, String currentPhaseName, String nextPhaseName) {
        this.blocksBroken = blocksBroken;
        this.blocksNeeded = blocksNeeded;
        this.currentPhaseName = currentPhaseName;
        this.nextPhaseName = nextPhaseName;
    }

    public SyncProgressS2CPacket(FriendlyByteBuf buf) {
        this.blocksBroken = buf.readInt();
        this.blocksNeeded = buf.readInt();
        this.currentPhaseName = buf.readUtf();
        this.nextPhaseName = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(blocksBroken);
        buf.writeInt(blocksNeeded);
        buf.writeUtf(currentPhaseName);
        buf.writeUtf(nextPhaseName);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ClientOneBlockData.set(this.blocksBroken, this.blocksNeeded, this.currentPhaseName, this.nextPhaseName);
        });
        return true;
    }
}
