package macro.buildorders;

import bwapi.TechType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import information.enemy.EnemyUnits;
import planner.PlannedItem;
import planner.PlannedItemType;
import util.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TwoRaxAcademy extends BuildOrder {
    public BuildOrderName getBuildOrderName() {
        return BuildOrderName.TWORAXACADEMY;
    }

    public ArrayList<PlannedItem> getBuildOrder() {
        ArrayList<PlannedItem> buildOrder = new ArrayList<>();
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 9, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 9, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 14, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 14, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Bunker, 16, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Refinery, 19, PlannedItemType.BUILDING, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Academy, 19, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 23, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(TechType.Stim_Packs, 26, PlannedItemType.UPGRADE, UnitType.Terran_Academy, 2));
        buildOrder.add(new PlannedItem(UpgradeType.U_238_Shells, 26, PlannedItemType.UPGRADE, UnitType.Terran_Academy, 1, 3));
        buildOrder.add(new PlannedItem(UnitType.Terran_Engineering_Bay, 28, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 30, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Barracks, 36, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Missile_Turret, 33, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Infantry_Weapons, 32, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 1, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Factory, 35, PlannedItemType.BUILDING, 1, true));
        buildOrder.add(new PlannedItem(UnitType.Terran_Supply_Depot, 38, PlannedItemType.BUILDING, 2));
        buildOrder.add(new PlannedItem(UnitType.Terran_Machine_Shop, 0, PlannedItemType.ADDON, 1));
        buildOrder.add(new PlannedItem(UpgradeType.Terran_Infantry_Armor, 38, PlannedItemType.UPGRADE, UnitType.Terran_Engineering_Bay, 1, 3));
        buildOrder.add(new PlannedItem(TechType.Tank_Siege_Mode, 0, PlannedItemType.UPGRADE, UnitType.Terran_Machine_Shop, 1));
        buildOrder.add(new PlannedItem(UnitType.Terran_Command_Center,  40, PlannedItemType.BUILDING, 1));
        return buildOrder;
    }

    public HashSet<UnitType> getLiftableBuildings() {
        return  liftableBuildings;
    }

    public BunkerLocation getBunkerLocation() {
        return BunkerLocation.MAIN;
    }

    public BuildType buildType() {
        return BuildType.BIO;
    }

    public HashMap<UnitType, Integer> getMoveOutCondition(Time time, HashSet<EnemyUnits> enemyUnits) {
        HashMap<UnitType, Integer> moveOutCondition = new HashMap<>();

        if (time.lessThanOrEqual(new Time(10,0))) {
            moveOutCondition.put(UnitType.Terran_Marine, 10);
            moveOutCondition.put(UnitType.Terran_Medic, 3);
        }
        else {
            moveOutCondition.put(UnitType.Terran_Marine, 16);
            moveOutCondition.put(UnitType.Terran_Medic, 5);
        }

        return moveOutCondition;
    }
}
