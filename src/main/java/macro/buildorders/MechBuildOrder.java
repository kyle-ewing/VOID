package macro.buildorders;

public abstract class MechBuildOrder extends BuildOrder {

    public boolean prioritizeTankFirst() {
        return true;
    }
}
