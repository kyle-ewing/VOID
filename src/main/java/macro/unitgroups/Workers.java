package macro.unitgroups;

import bwapi.Unit;

public class Workers {
    private Unit unit;
    private WorkerStatus workerStatus;
    private int buildFrameCount;

    public Workers(Unit unit, WorkerStatus workerStatus) {
        this.unit = unit;
        this.workerStatus = workerStatus;
        this.buildFrameCount = 0;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public WorkerStatus getWorkerStatus() {
        return workerStatus;
    }

    public void setWorkerStatus(WorkerStatus workerStatus) {
        this.workerStatus = workerStatus;
    }

    public int getBuildFrameCount() {
        return buildFrameCount;
    }

    public void setBuildFrameCount(int buildFrameCount) {
        this.buildFrameCount = buildFrameCount;
    }
}
