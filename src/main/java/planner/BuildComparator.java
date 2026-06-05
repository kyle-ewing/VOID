package planner;

import planner.PlannedItem;
import planner.PlannedItemType;

import java.util.Comparator;

public class BuildComparator implements Comparator<PlannedItem> {

    public int compare(PlannedItem item1, PlannedItem item2) {
        if (item1.getPriority() < item2.getPriority()) {
            return -1;
        }
        else if (item1.getPriority() > item2.getPriority()) {
            return 1;
        }
        else if (item1.getSupply() < item2.getSupply()) {
            return -1;
        }
        else if (item1.getSupply() > item2.getSupply()) {
            return 1;
        }
        else if (item1.getPlannedItemType() == PlannedItemType.BUILDING && item2.getPlannedItemType() != PlannedItemType.BUILDING) {
            return -1;
        }
        else if (item1.getPlannedItemType() != PlannedItemType.BUILDING && item2.getPlannedItemType() == PlannedItemType.BUILDING) {
            return 1;
        }
        else {
            return 0;
        }
    };
}
