package macro;

import bwapi.Player;
import bwapi.UnitType;

public class ResourceTracking {
    private Player player;
    int reservedMinerals = 0;
    int reservedGas = 0;
    int availableMinerals = 0;
    int availableGas = 0;

    public ResourceTracking(Player player) {
        this.player = player;
    }

    public void reserveResources(UnitType unitType) {
        reservedMinerals += unitType.mineralPrice();
        reservedGas += unitType.gasPrice();
        availableMinerals = player.minerals() - reservedMinerals;
        availableGas = player.gas() - reservedGas;
    }

    public void unreserveResources(UnitType unitType) {
        reservedMinerals -= unitType.mineralPrice();
        reservedGas -= unitType.gasPrice();
        availableMinerals = player.minerals() - reservedMinerals;
        availableGas = player.gas() - reservedGas;
    }

    public void onFrame() {
        setAvailableMinerals();
        setAvailableGas();
    }

    public int getAvailableGas() {
        return availableGas;
    }

    public void setAvailableGas() {
        this.availableGas = player.gas() - reservedGas;
    }

    public int getAvailableMinerals() {
        return availableMinerals;
    }

    private void setAvailableMinerals() {
        this.availableMinerals = player.minerals() - reservedMinerals;
    }
}
