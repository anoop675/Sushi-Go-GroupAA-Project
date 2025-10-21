package evaluation.heuristics;

import core.Game;
import core.interfaces.IGameHeuristic;

public record TargetGameLength(LengthType type, int target) implements IGameHeuristic {

    public TargetGameLength(String type, int target) {
        this.target = target;
        this.type = LengthType.valueOf(type);
    }

    @Override
    public double evaluateGame(Game game) {
        int result = 0;
        switch (type) {
            case ROUNDS:
                result = game.getGameState().getRoundCounter();
                break;
            case TURNS:
                result = game.getGameState().getTurnCounter();
                break;
            case TICKS:
                result = game.getTick();
                break;
        }
        return -Math.abs(target - result);
        // we use a simple absolute loss
    }

    public enum LengthType {
        ROUNDS, TURNS, TICKS
    }
}
