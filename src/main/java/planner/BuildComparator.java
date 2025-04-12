package planner;

import planner.PlannedItem;

import java.util.Comparator;

public class BuildComparator implements Comparator<PlannedItem> {

    public int compare(PlannedItem item1, PlannedItem item2) {
        if(item1.getPriority() < item2.getPriority()) {
            return -1;
        } else if(item1.getPriority() > item2.getPriority()) {
            return 1;
        } else {
            return 0;
        }
    };
}
