package macro;

import bwapi.Player;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
import bwem.Mineral;
import information.BaseInfo;
import macro.unitgroups.WorkerStatus;
import macro.unitgroups.Workers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ResourceManager {
    public ResourceManager(BaseInfo baseInfo, Player player) {
        this.baseInfo = baseInfo;
        this.player = player;
    }

    private BaseInfo baseInfo;
    private Player player;
    private HashSet<Workers> workers = new HashSet<>();
    private HashMap<Base, HashSet<Workers>> refinerySaturation = new HashMap<>();
    private int reservedMinerals = 0;
    private int reservedGas = 0;
    private int availableMinerals = 0;
    private int availableGas = 0;
    private boolean isReserved = false;

    public void gatherMinerals() {
        //TODO: Assign workers to empty patches
        Mineral mineralPatch = baseInfo.getStartingMinerals().iterator().next();
        for(Workers scv : workers) {
            if(scv.getWorkerStatus() == WorkerStatus.IDLE) {
                scv.getUnit().gather(mineralPatch.getUnit());
                scv.setWorkerStatus(WorkerStatus.MINERALS);
            }
        }
    }

    //TODO: make generic for all bases, remove from hashmap if workers are removed
    public void gatherGas() {
        if(baseInfo.getStartingBase().getGeysers().get(0).getUnit().getType() == UnitType.Terran_Refinery && baseInfo.getStartingBase().getGeysers().get(0).getUnit().isCompleted()) {
            for(Workers scv : workers) {
                if(scv.getWorkerStatus() == WorkerStatus.MINERALS) {
                    if(refinerySaturation.containsKey(baseInfo.getStartingBase()) && refinerySaturation.get(baseInfo.getStartingBase()).size() <= 3) {
                        scv.getUnit().gather(baseInfo.getStartingBase().getGeysers().get(0).getUnit());
                        scv.setWorkerStatus(WorkerStatus.GAS);
                    }
                    refinerySaturation.computeIfAbsent(baseInfo.getStartingBase(), workerCount -> new HashSet<Workers>()).add(scv);
                }
            }
        }
    }

    public  void reserveResources(UnitType unitType) {
        reservedMinerals += unitType.mineralPrice();
        reservedGas += unitType.gasPrice();
        availableMinerals = player.minerals() - reservedMinerals;
        availableGas = player.gas() - reservedGas;
        isReserved = true;
    }

    public void unreserveResources(UnitType unitType) {
        reservedMinerals -= unitType.mineralPrice();
        reservedGas -= unitType.gasPrice();
        availableMinerals = player.minerals() - reservedMinerals;
        availableGas = player.gas() - reservedGas;
        isReserved = false;
    }

    //
    private void workerBuildClock() {
        for(Workers worker : workers) {
            if(worker.getWorkerStatus() == WorkerStatus.MOVING_TO_BUILD) {
                worker.setBuildFrameCount(worker.getBuildFrameCount() + 1);
            }
            else {
                worker.setBuildFrameCount(0);
            }

        }
    }

    public void onUnitComplete(Unit scv) {
        workers.add(new Workers(scv, WorkerStatus.IDLE));
    }

    public void onFrame() {
        setAvailableMinerals(availableMinerals);
        setAvailableGas(availableGas);
        gatherMinerals();
        gatherGas();
        workerBuildClock();
    }

    public void onUnitDestroy(Unit scv) {
        for(Workers worker : workers) {
            if(worker.getUnit() == scv) {
                workers.remove(worker);
                break;
            }
        }
    }

    public int getReservedMinerals() {
        return reservedMinerals;
    }

    public void setReservedMinerals(int reservedMinerals) {
        this.reservedMinerals = reservedMinerals;
    }

    public int getReservedGas() {
        return reservedGas;
    }

    public void setReservedGas(int reservedGas) {
        this.reservedGas = reservedGas;
    }

    public int getAvailableMinerals() {
        return availableMinerals;
    }

    private void setAvailableMinerals(int availableMinerals) {
        this.availableMinerals = player.minerals() - reservedMinerals;
    }

    public int getAvailableGas() {
        return availableGas;
    }

    public void setAvailableGas(int availableGas) {
        this.availableGas = player.gas() - reservedGas;
    }

    public boolean isReserved() {
        return isReserved;
    }

    public void setReserved(boolean reserved) {
        isReserved = reserved;
    }

    public HashSet<Workers> getWorkers() {
        return workers;
    }

}
