package macro.buildpivots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bwapi.Race;
import bwapi.TechType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import information.enemy.enemyopeners.EnemyStrategyName;
import macro.buildorders.BuildType;
import macro.buildorders.BunkerLocation;
import planner.PlannedItem;
import planner.PlannedItemType;
import util.Time;

public class LingRushCounter extends BuildPivot {
    
    public LingRushCounter() {
        enemyStrategies.add(EnemyStrategyName.FOURPOOL);
        enemyStrategies.add(EnemyStrategyName.NINEPOOLSPEEDLING);
    }

    public BuildPivotName getBuildPivotName() {
        return BuildPivotName.LINGRUSHCOUNTER;
    }

    public ArrayList<PlannedItem> getPivotBuild() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Comsat_Station, 0, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 9, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 19, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 15, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 20, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Academy, 19, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 23, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(TechType.Stim_Packs, 26, PlannedItemType.UPGRADE, UnitType.Terran_Academy, 1));
        buildOrder.add(new PlannedItem(UpgradeType.U_238_Shells, 26, PlannedItemType.UPGRADE, UnitType.Terran_Academy, 1, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Engineering_Bay, 31, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 30, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 32, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Missile_Turret, 33, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Infantry_Weapons, 32, PlannedItemType.UPGRADE,UnitType.Terran_Engineering_Bay, 1, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Command_Center, 34, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 38, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 0, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Infantry_Armor, 38, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 1, 4));
        buildOrder.add(new PlannedItem(TechType.Tank_Siege_Mode, 40, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 40, PlannedItemType.BUILDING, 1, true));


        return buildOrder;
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(Time time, HashSet<EnemyUnits> enemyUnits) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();

        if (time.lessThanOrEqual(new Time(10, 0))) {
            moveOutCondition.put(UnitType.Terran_Marine, 14);
            moveOutCondition.put(UnitType.Terran_Medic, 3);
        } else {
            moveOutCondition.put(UnitType.Terran_Marine, 16);
            moveOutCondition.put(UnitType.Terran_Medic, 5);
        }

        return moveOutCondition;
    }

    public BunkerLocation getBunkerLocation() {
        return BunkerLocation.MAIN;
    }

    public BuildType buildType() {
        return BuildType.BIO;
    }

    @Override
    public boolean pivotsFrom(EnemyStrategyName enemyStrategy, Race enemyRace) {
        return enemyStrategies.contains(enemyStrategy);
    }


    @Override
    public ArrayList<UnitType> proxyBuildings() {
        return new ArrayList<>();
    }

    @Override
    public HashSet<UnitType> getLiftableBuildings() {
        HashSet<UnitType> liftableBuildings = new HashSet<>();
        liftableBuildings.add(UnitType.Terran_Science_Facility);
        return liftableBuildings;
    }

    @Override
    public HashSet<UnitType> getCancelableBuildings() {
        HashSet<UnitType> cancelableBuildings = new HashSet<>();
        cancelableBuildings.add(UnitType.Terran_Factory);
        cancelableBuildings.add(UnitType.Terran_Refinery);
        return cancelableBuildings;
    }

    @Override
    public boolean retainAddedBuildings() {
        return true;
    }

    @Override
    public HashMap<UnitType, Integer> getRequiredBuildings() {
        HashMap<UnitType, Integer> requiredBuildings = new HashMap<>();
        requiredBuildings.put(UnitType.Terran_Barracks, 2);
        requiredBuildings.put(UnitType.Terran_Academy, 1);
        requiredBuildings.put(UnitType.Terran_Supply_Depot, 3);
        return requiredBuildings;
    }
}
