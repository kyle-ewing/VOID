package macro;

import bwapi.Game;
import bwapi.Player;
import bwapi.Race;
import bwapi.UnitType;
import information.GameState;
import planner.PlannedItem;

public class ExpansionCriteria {
    private Game game;
    private Player player;
    private GameState gameState;
    private int expansionScore = 0;
    private boolean workerCriteria = false;
    private boolean enemyBaseCriteria = false;
    private boolean mineralCriteria = false;
    private boolean productionBuildingCriteria = false;
    private boolean armyComparisonCriteria = false;


    public ExpansionCriteria(Game game, Player player, GameState gameState) {
        this.game = game;
        this.player = player;
        this.gameState = gameState;
    }

    private void evaluateExpansion() {
        int baseCount = gameState.getUnitTypeCount().get(UnitType.Terran_Command_Center);

        if(gameState.getCanExpand()) {
            return;
        }

        //Don't start evaluating until after first expansion
        if(baseCount == 1) {
            return;
        }

        if(gameState.getProductionQueue().stream().map(PlannedItem::getUnitType).anyMatch(ut -> ut == UnitType.Terran_Command_Center)) {
            return;
        }

        // Repeatedly checked criteria
        if(!enemyBaseCriteria) {
            Race enemyRace = game.enemy().getRace();

            switch(enemyRace) {
                case Zerg:
                    if(gameState.getKnownEnemyUnits().stream().filter(b -> b.getEnemyType() == UnitType.Zerg_Hatchery).count() + 1
                            >= gameState.getUnitTypeCount().get(UnitType.Terran_Command_Center)) {
                        enemyBaseCriteria = true;
                        expansionScore += 2;
                    }
                    break;
                case Protoss:
                    if(gameState.getKnownEnemyUnits().stream().filter(b -> b.getEnemyType() == UnitType.Protoss_Nexus).count()
                            >= gameState.getUnitTypeCount().get(UnitType.Terran_Command_Center)) {
                        enemyBaseCriteria = true;
                        expansionScore += 2;
                    }
                    break;
                case Terran:
                    if(gameState.getKnownEnemyUnits().stream().filter(b -> b.getEnemyType() == UnitType.Terran_Command_Center).count()
                            >= gameState.getUnitTypeCount().get(UnitType.Terran_Command_Center)) {
                        enemyBaseCriteria = true;
                        expansionScore += 2;
                    }
                    break;
            }
        }

        if(!mineralCriteria) {
            if(gameState.getResourceTracking().availableMinerals > 600 && gameState.getUnitTypeCount().get(UnitType.Terran_Command_Center) >= 2) {
                mineralCriteria = true;
                expansionScore += 2;
            }
        }

        if(!armyComparisonCriteria) {
            // do this later
        }

        //Single triggered criteria
        if(!workerCriteria) {
            int workerCount = gameState.getWorkers().size();

            if(workerCount >= baseCount * 24 - 4 && baseCount >= 2) {
                workerCriteria = true;
                expansionScore += 2;
            }
        }

        if(!productionBuildingCriteria) {
            switch (gameState.getStartingOpener().buildType()) {
                case BIO:
                    if(gameState.getUnitTypeCount().get(UnitType.Terran_Barracks) >= 5 && baseCount == 2) {
                        productionBuildingCriteria = true;
                        expansionScore += 2;
                    }
                    break;
                case MECH:
                    if(gameState.getUnitTypeCount().get(UnitType.Terran_Factory) >= 5 && baseCount == 2) {
                        productionBuildingCriteria = true;
                        expansionScore += 2;
                    }
                    break;
                case SKYTERRAN:
                    // meme for later
            }
        }

        if(expansionScore >= 4) {
            gameState.setCanExpand(true);
            expansionScore = 0;
            enemyBaseCriteria = false;
            mineralCriteria = false;
            armyComparisonCriteria = false;
        }
    }

    public void onFrame() {
        evaluateExpansion();
    }

}
