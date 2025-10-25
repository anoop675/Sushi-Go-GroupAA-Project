package groupAA;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.PlayerParameters;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class GroupAAGreedyRolloutPolicy implements GroupAARolloutPolicy {

    private static final Logger LOGGER = Logger.getLogger(GroupAAGreedyRolloutPolicy.class.getName());

    private final SushiGoAgentGroupAA player;
    private final AMAF_Params params;

    public GroupAAGreedyRolloutPolicy(SushiGoAgentGroupAA player) {
        this.player = player;
        this.params = player.getParameters();
    }

    //does a cheap single-step (single simulation) greedy lookahead
    @Override
    public AbstractAction chooseAction(AbstractGameState state, List<AbstractAction> actions, int playerId, Random rnd) {
        if (actions == null || actions.isEmpty()){
            LOGGER.info("No action for single-rollout step lookahead");
            return null;
        }

        AbstractAction best = null;
        double bestScore = -Double.MAX_VALUE;
        for (AbstractAction a : actions) { //for every candidate action
            try {
                AbstractGameState copy = state.copy();
                player.getForwardModel().next(copy, a.copy()); //it simulates one step
                double score = params.getStateHeuristic().evaluateState(copy, playerId); //evaluates the child state with the heuristic
                score = utilities.Utils.noise(score, params.epsilon, rnd.nextDouble()); //adding exploration (epsilon) noise to the score
                if (score > bestScore) {
                    bestScore = score;
                    best = a;
                    LOGGER.info("Selected action leading to better heuristic value");
                }
            } catch (Throwable ignored) {
                if (best == null)
                    best = a;
            }
        }
        return best != null ? best : actions.get(rnd.nextInt(actions.size())); //returns the action with the best heuristic score
    }
}