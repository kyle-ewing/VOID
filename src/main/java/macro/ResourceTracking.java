package macro;

import java.util.PriorityQueue;

import bwapi.Player;
import bwapi.UnitType;
import planner.PlannedItem;
import planner.PlannedItemStatus;

public class ResourceTracking {
    private Player player;
    private PriorityQueue<PlannedItem> productionQueue;
    int reservedMinerals = 0;
    int reservedGas = 0;
    int availableMinerals = 0;
    int availableGas = 0;

    public ResourceTracking(Player player, PriorityQueue<PlannedItem> productionQueue) {
        this.player = player;
        this.productionQueue = productionQueue;
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
        setReservedResources();
        setAvailableMinerals();
        setAvailableGas();
    }

    private void setReservedResources() {
        int minerals = 0;
        int gas = 0;

        for (PlannedItem pi : productionQueue) {
            if (pi.getPlannedItemStatus() != PlannedItemStatus.SCV_ASSIGNED) {
                continue;
            }

            minerals += pi.getUnitType().mineralPrice();
            gas += pi.getUnitType().gasPrice();
        }

        reservedMinerals = minerals;
        reservedGas = gas;
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
