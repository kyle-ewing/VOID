package information.enemy.enemyarmycomposition;

import information.enemy.EnemyUnits;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class EnemyArmyCompManager {
    private final List<EnemyArmyCompResponse> responses = new ArrayList<>();

    public EnemyArmyCompManager() {
        responses.add(new Zergling());
    }

    public List<EnemyArmyCompResponse> getTriggeredResponses(HashSet<EnemyUnits> enemyUnits) {
        List<EnemyArmyCompResponse> triggered = new ArrayList<>();
        for (EnemyArmyCompResponse response : responses) {
            if (response.isTriggered(enemyUnits)) {
                triggered.add(response);
            }
        }
        return triggered;
    }
}
