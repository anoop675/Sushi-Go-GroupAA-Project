package evaluation.heuristics;

import core.CoreConstants;
import core.Game;
import core.interfaces.IGameHeuristic;

public record TargetWinner(String winnerName) implements IGameHeuristic {

    @Override
    public double evaluateGame(Game game) {
        // +1 for winning (regardless of draw status)
        // 0 otherwise
        for (int p = 0; p < game.getPlayers().size(); p++) {
            if (game.getPlayers().get(p).toString().equals(winnerName))
                if (game.getGameState().getPlayerResults()[p] == CoreConstants.GameResult.WIN_GAME)
                    return 1.0;
        }
        return 0.0;
    }
}
