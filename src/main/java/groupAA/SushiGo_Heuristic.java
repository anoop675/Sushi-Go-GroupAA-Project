package groupAA;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class SushiGo_Heuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerID) {
        // Placeholder heuristic: neutral value
        return 0.0;
    }
}