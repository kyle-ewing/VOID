package map.bwemwrappers;

public class StaticBuilding extends Neutral {
    private bwem.StaticBuilding bwemStaticBuilding;

    public StaticBuilding(bwem.StaticBuilding bwemStaticBuilding) {
        super(bwemStaticBuilding);
        this.bwemStaticBuilding = bwemStaticBuilding;
    }

    public bwem.StaticBuilding getBwemStaticBuilding() {
        return bwemStaticBuilding;
    }
}
