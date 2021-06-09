package ml.northwestwind.survivalspectator.entity;


import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;

public class FakeNetworkManager extends NetworkManager {
    public FakeNetworkManager(PacketDirection p)
    {
        super(p);
    }

    @Override
    public void setReadOnly()
    {
    }

    @Override
    public void handleDisconnection()
    {
    }
}
