package information.enemy;

import bwapi.Game;
import bwapi.UnitType;
import bwapi.UpgradeType;

public class EnemyUpgrades {
    private Game game;

    private boolean u238Shells = false;
    private boolean ionThrusters = false;
    private boolean titanReactor = false;
    private boolean ocularImplants = false;
    private boolean moebiusReactor = false;
    private boolean apolloReactor = false;
    private boolean colossusReactor = false;
    private boolean caduceusReactor = false;
    private boolean charonBoosters = false;

    private boolean ventralSacs = false;
    private boolean antennae = false;
    private boolean pneumatizedCarapace = false;
    private boolean metabolicBoost = false;
    private boolean adrenalGlands = false;
    private boolean muscularAugments = false;
    private boolean groovedSpines = false;
    private boolean gameteMeiosis = false;
    private boolean metasynapticNode = false;
    private boolean chitinousPlating = false;
    private boolean anabolicSynthesis = false;

    private boolean singularityCharge = false;
    private boolean legEnhancements = false;
    private boolean scarabDamage = false;
    private boolean reaverCapacity = false;
    private boolean graviticDrive = false;
    private boolean sensorArray = false;
    private boolean graviticBoosters = false;
    private boolean khaydarinAmulet = false;
    private boolean apialSensors = false;
    private boolean graviticThrusters = false;
    private boolean carrierCapacity = false;
    private boolean khaydarinCore = false;
    private boolean argusJewel = false;
    private boolean argusTalisman = false;

    private int terranInfantryWeapons = 0;
    private int terranInfantryArmor = 0;
    private int terranVehicleWeapons = 0;
    private int terranVehiclePlating = 0;
    private int terranShipWeapons = 0;
    private int terranShipPlating = 0;

    private int zergMeleeAttacks = 0;
    private int zergMissileAttacks = 0;
    private int zergCarapace = 0;
    private int zergFlyerAttacks = 0;
    private int zergFlyerCarapace = 0;

    private int protossGroundWeapons = 0;
    private int protossGroundArmor = 0;
    private int protossAirWeapons = 0;
    private int protossAirArmor = 0;
    private int protossPlasmaShields = 0;

    public EnemyUpgrades(Game game) {
        this.game = game;
    }

    public void onFrame() {
        if (game.enemy() == null) {
            return;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.U_238_Shells) > 0) {
            u238Shells = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Ion_Thrusters) > 0) {
            ionThrusters = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Titan_Reactor) > 0) {
            titanReactor = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Ocular_Implants) > 0) {
            ocularImplants = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Moebius_Reactor) > 0) {
            moebiusReactor = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Apollo_Reactor) > 0) {
            apolloReactor = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Colossus_Reactor) > 0) {
            colossusReactor = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Caduceus_Reactor) > 0) {
            caduceusReactor = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Charon_Boosters) > 0) {
            charonBoosters = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Ventral_Sacs) > 0) {
            ventralSacs = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Antennae) > 0) {
            antennae = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Pneumatized_Carapace) > 0) {
            pneumatizedCarapace = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Metabolic_Boost) > 0) {
            metabolicBoost = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Adrenal_Glands) > 0) {
            adrenalGlands = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Muscular_Augments) > 0) {
            muscularAugments = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Grooved_Spines) > 0) {
            groovedSpines = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Gamete_Meiosis) > 0) {
            gameteMeiosis = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Metasynaptic_Node) > 0) {
            metasynapticNode = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Chitinous_Plating) > 0) {
            chitinousPlating = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Anabolic_Synthesis) > 0) {
            anabolicSynthesis = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Singularity_Charge) > 0) {
            singularityCharge = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Leg_Enhancements) > 0) {
            legEnhancements = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Scarab_Damage) > 0) {
            scarabDamage = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Reaver_Capacity) > 0) {
            reaverCapacity = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Gravitic_Drive) > 0) {
            graviticDrive = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Sensor_Array) > 0) {
            sensorArray = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Gravitic_Boosters) > 0) {
            graviticBoosters = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Khaydarin_Amulet) > 0) {
            khaydarinAmulet = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Apial_Sensors) > 0) {
            apialSensors = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Gravitic_Thrusters) > 0) {
            graviticThrusters = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Carrier_Capacity) > 0) {
            carrierCapacity = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Khaydarin_Core) > 0) {
            khaydarinCore = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Argus_Jewel) > 0) {
            argusJewel = true;
        }

        if (game.enemy().getUpgradeLevel(UpgradeType.Argus_Talisman) > 0) {
            argusTalisman = true;
        }

        terranInfantryWeapons = Math.max(terranInfantryWeapons, game.enemy().getUpgradeLevel(UpgradeType.Terran_Infantry_Weapons));
        terranInfantryArmor = Math.max(terranInfantryArmor, game.enemy().getUpgradeLevel(UpgradeType.Terran_Infantry_Armor));
        terranVehicleWeapons = Math.max(terranVehicleWeapons, game.enemy().getUpgradeLevel(UpgradeType.Terran_Vehicle_Weapons));
        terranVehiclePlating = Math.max(terranVehiclePlating, game.enemy().getUpgradeLevel(UpgradeType.Terran_Vehicle_Plating));
        terranShipWeapons = Math.max(terranShipWeapons, game.enemy().getUpgradeLevel(UpgradeType.Terran_Ship_Weapons));
        terranShipPlating = Math.max(terranShipPlating, game.enemy().getUpgradeLevel(UpgradeType.Terran_Ship_Plating));

        zergMeleeAttacks = Math.max(zergMeleeAttacks, game.enemy().getUpgradeLevel(UpgradeType.Zerg_Melee_Attacks));
        zergMissileAttacks = Math.max(zergMissileAttacks, game.enemy().getUpgradeLevel(UpgradeType.Zerg_Missile_Attacks));
        zergCarapace = Math.max(zergCarapace, game.enemy().getUpgradeLevel(UpgradeType.Zerg_Carapace));
        zergFlyerAttacks = Math.max(zergFlyerAttacks, game.enemy().getUpgradeLevel(UpgradeType.Zerg_Flyer_Attacks));
        zergFlyerCarapace = Math.max(zergFlyerCarapace, game.enemy().getUpgradeLevel(UpgradeType.Zerg_Flyer_Carapace));

        protossGroundWeapons = Math.max(protossGroundWeapons, game.enemy().getUpgradeLevel(UpgradeType.Protoss_Ground_Weapons));
        protossGroundArmor = Math.max(protossGroundArmor, game.enemy().getUpgradeLevel(UpgradeType.Protoss_Ground_Armor));
        protossAirWeapons = Math.max(protossAirWeapons, game.enemy().getUpgradeLevel(UpgradeType.Protoss_Air_Weapons));
        protossAirArmor = Math.max(protossAirArmor, game.enemy().getUpgradeLevel(UpgradeType.Protoss_Air_Armor));
        protossPlasmaShields = Math.max(protossPlasmaShields, game.enemy().getUpgradeLevel(UpgradeType.Protoss_Plasma_Shields));
    }

    public boolean hasSpeedUpgrade(UnitType unitType) {
        if (unitType == UnitType.Terran_Vulture) {
            return ionThrusters;
        }

        if (unitType == UnitType.Zerg_Zergling) {
            return metabolicBoost;
        }

        if (unitType == UnitType.Zerg_Overlord) {
            return pneumatizedCarapace;
        }

        if (unitType == UnitType.Zerg_Hydralisk) {
            return muscularAugments;
        }

        if (unitType == UnitType.Zerg_Ultralisk) {
            return anabolicSynthesis;
        }

        if (unitType == UnitType.Protoss_Zealot) {
            return legEnhancements;
        }

        if (unitType == UnitType.Protoss_Shuttle) {
            return graviticDrive;
        }

        if (unitType == UnitType.Protoss_Observer) {
            return graviticBoosters;
        }

        if (unitType == UnitType.Protoss_Scout) {
            return graviticThrusters;
        }

        return false;
    }

    public boolean hasRangeUpgrade(UnitType unitType) {
        if (unitType == UnitType.Terran_Marine) {
            return u238Shells;
        }

        if (unitType == UnitType.Terran_Goliath) {
            return charonBoosters;
        }

        if (unitType == UnitType.Zerg_Hydralisk) {
            return groovedSpines;
        }

        if (unitType == UnitType.Protoss_Dragoon) {
            return singularityCharge;
        }

        return false;
    }
}
