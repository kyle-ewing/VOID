package planner;

import bwapi.UnitType;

import java.util.HashMap;

public class BuildingRequirements {

    private HashMap<UnitType, Integer> unitCount = new HashMap<>();

    //Checks if the player has the proper buildings to add to the build queue
    public boolean meetsReq(UnitType unitType) {
        if (unitType == UnitType.Terran_Command_Center) {
            return true;
        } else if (unitType == UnitType.Terran_Supply_Depot) {
            if (unitCount.containsKey(UnitType.Terran_Command_Center) && unitCount.get(UnitType.Terran_Command_Center) > 0) {
                return true;
            }
        } else if (unitType == UnitType.Terran_Barracks) {
            if (unitCount.containsKey(UnitType.Terran_Command_Center) && unitCount.get(UnitType.Terran_Command_Center) > 0) {
                return true;
            }
        } else if (unitType == UnitType.Terran_Engineering_Bay) {
            if (unitCount.containsKey(UnitType.Terran_Command_Center) && unitCount.get(UnitType.Terran_Command_Center) > 0) {
                return true;
            }
        } else if (unitType == UnitType.Terran_Refinery) {
            if (unitCount.containsKey(UnitType.Terran_Command_Center) && unitCount.get(UnitType.Terran_Command_Center) > 0) {
                return true;
            }
        } else if (unitType == UnitType.Terran_Bunker) {
            if (unitCount.containsKey(UnitType.Terran_Barracks) && unitCount.get(UnitType.Terran_Barracks) > 0) {
                return true;
            }
        } else if (unitType == UnitType.Terran_Factory) {
            if (unitCount.containsKey(UnitType.Terran_Barracks) && unitCount.get(UnitType.Terran_Barracks) > 0) {
                return true;
            }
        } else if (unitType == UnitType.Terran_Academy) {
            if (unitCount.containsKey(UnitType.Terran_Barracks) && unitCount.get(UnitType.Terran_Barracks) > 0) {
                return true;
            }
        } else if (unitType == UnitType.Terran_Missile_Turret) {
            if (unitCount.containsKey(UnitType.Terran_Engineering_Bay) && unitCount.get(UnitType.Terran_Engineering_Bay) > 0) {
                return true;
            }
        } else if (unitType == UnitType.Terran_Armory) {
            if (unitCount.containsKey(UnitType.Terran_Factory) && unitCount.get(UnitType.Terran_Factory) > 0) {
                return true;
            }
        } else if (unitType == UnitType.Terran_Starport) {
            if (unitCount.containsKey(UnitType.Terran_Factory) && unitCount.get(UnitType.Terran_Factory) > 0) {
                return true;
            }
        } else if (unitType == UnitType.Terran_Science_Facility) {
            if (unitCount.containsKey(UnitType.Terran_Starport) && unitCount.get(UnitType.Terran_Starport) > 0) {
                return true;
            }
        }
        return false;
    }
}