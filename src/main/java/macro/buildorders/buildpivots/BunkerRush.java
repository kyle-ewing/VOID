package macro.buildorders.buildpivots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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

public class BunkerRush extends BuildPivot {
    
    public BunkerRush() {
        rushActive = true;
    }

    public BuildPivotName getBuildPivotName() {
        return BuildPivotName.BUNKERRUSH;
    }

    public ArrayList<PlannedItem> getPivotBuild() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Bunker, 0, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 10, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 14, PlannedItemType.UNIT, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Marine, 14, PlannedItemType.UNIT, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 12, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 16, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 16, PlannedItemType.BUILDING, 4, true));
        buildOrder.add(new PlannedItem(UnitType.Terran_Command_Center, 20, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 21, PlannedItemType.ADDON, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Academy, 21, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 23, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 23, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(TechType.Stim_Packs, 26, PlannedItemType.UPGRADE, UnitType.Terran_Academy, 2));
        buildOrder.add(new PlannedItem(UpgradeType.U_238_Shells, 26, PlannedItemType.UPGRADE, UnitType.Terran_Academy, 1, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 30, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(TechType.Tank_Siege_Mode, 32, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Engineering_Bay, 35, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 44, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 45, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Command_Center, 50, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Infantry_Weapons, 38, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 1, 2));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Infantry_Armor, 38, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 1, 2));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Vehicle_Weapons, 50, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 1, 3));

        return buildOrder;
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(Time time, HashSet<EnemyUnits> enemyUnits) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();

        if (time.lessThanOrEqual(new Time(4,30)) && rushActive) {
            moveOutCondition.put(UnitType.Terran_Marine, 1);
        }
        else {
            moveOutCondition.put(UnitType.Terran_Marine, 15);
            moveOutCondition.put(UnitType.Terran_Siege_Tank_Tank_Mode, 3);
            moveOutCondition.put(UnitType.Terran_Marine, 4);
        }


        return moveOutCondition;
    }

    public BunkerLocation getBunkerLocation() {
        return BunkerLocation.ENEMY_NATURAL;
    }

    public BuildType buildType() {
        return BuildType.BIO;
    }

    @Override
    public boolean pivotsFrom(EnemyStrategyName enemyStrategy) {
        if (enemyStrategy == EnemyStrategyName.NEXUSFIRST) {
            return true;
        }
        return false;
    }


    @Override
    public ArrayList<UnitType> proxyBuildings() {
        ArrayList<UnitType> proxyBuildings = new ArrayList<>();
        proxyBuildings.add(UnitType.Terran_Bunker);
        return proxyBuildings;
    }

    @Override
    public HashSet<UnitType> getLiftableBuildings() {
        HashSet<UnitType> liftableBuildings = new HashSet<>();
        liftableBuildings.add(UnitType.Terran_Science_Facility);
        return liftableBuildings;
    }

}
