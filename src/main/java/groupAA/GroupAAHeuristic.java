package groupAA;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import org.json.simple.JSONObject;

public class GroupAAHeuristic implements IStateHeuristic {

    // No-arg constructor (used by default)
    public GroupAAHeuristic() {
        System.out.println("GroupAAHeuristic initialized (no-arg constructor)");
    }

    // JSON constructor (used by JSONUtils)
    public GroupAAHeuristic(JSONObject json) {
        System.out.println("GroupAAHeuristic initialized from JSON: " + json.toJSONString());
        // Optional: parse fields from JSON if needed in the future
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        // Simple placeholder heuristic
        return 0;
    }

    @Override
    public double minValue() {
        return -1;
    }

    @Override
    public double maxValue() {
        return 1;
    }
}
