package groupAA;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import players.PlayerParameters;

import java.util.Arrays;
// import the actual package where the class is
import some.other.package.SushiGoAgentGroupAA;


public class AMAF_Params extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10; // assuming we have a good heuristic
    public int maxTreeDepth = 100; // effectively no limit
    public double epsilon = 1e-6;
    public IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;

    // constructor to define tunable parameters for the AMAF-enhanced MCTS Agent
    // TODO: tweak params like exploration constant, rollout length, etc.) to find the best-performing setup.
    public AMAF_Params() {
        // Math.sqrt(2) is default for the exploration constant
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        // simulates 10 steps ahead, NOTE: deeper the rollout, the more accurate but slower the value estimate.
        addTunableParameter("rolloutLength", 10, Arrays.asList(0, 3, 10, 30, 100));
        //
        addTunableParameter("maxTreeDepth", 100, Arrays.asList(1, 3, 10, 30, 100));
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
    }

    @Override
    public void _reset() {
        super._reset();
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        epsilon = (double) getParameterValue("epsilon");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
    }

    @Override
    protected AMAF_Params _copy() {
        return new AMAF_Params();
    }

    @Override
    public IStateHeuristic getStateHeuristic() {
        return heuristic;
    }

    @Override
    public SushiGoAgentGroupAA instantiate() {
        return new SushiGoAgentGroupAA((AMAF_Params) this.copy());
    }

}