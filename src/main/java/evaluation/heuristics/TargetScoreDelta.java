package evaluation.heuristics;

import core.AbstractGameState;
import core.Game;
import core.interfaces.IGameHeuristic;

import java.util.DoubleSummaryStatistics;
import java.util.stream.IntStream;

public record TargetScoreDelta(int target) implements IGameHeuristic {


    @Override
    public double evaluateGame(Game game) {
        AbstractGameState state = game.getGameState();
        DoubleSummaryStatistics stats = IntStream.range(0, state.getNPlayers(playerId))
                .mapToDouble(state::getGameScore)
                .summaryStatistics();
        return -Math.abs(stats.getMax() - stats.getMin() - target);
    }
}
