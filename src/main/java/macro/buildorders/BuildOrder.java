package macro.buildorders;

import planner.PlannedItem;

import java.util.ArrayList;

public interface BuildOrder {
    BuildOrderName getBuildOrderName();
    ArrayList<PlannedItem> getBuildOrder();
}
