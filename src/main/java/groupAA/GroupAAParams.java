package groupAA;

//import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import players.PlayerParameters;

import java.util.Arrays;

public class GroupAAParams extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10; // default
    public int maxTreeDepth = 100;
    public double epsilon = 1e-6; // small numeric noise used in UCT
    public double biasWeight = 0.5; // progressive-bias weight (0.1 is safe, but 0.5 if heuristic is admissible)
    public IStateHeuristic heuristic = new GroupAAHeuristic(); // default to your heuristic

    // NEW: rollout policy and exploration inside rollout (epsilon-greedy)
    public GroupAARolloutPolicy rolloutPolicy = null; // default to null => use RandomPlayer or fallback
    //public double rolloutEpsilon = 0.05; // for epsilon-rollouts (small randomisation) [ALREADY USED IN GroupAATreeNode rollOut()]

    public GroupAAParams() {
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("rolloutLength", rolloutLength, Arrays.asList(0, 3, 5, 10, 30, 100));
        addTunableParameter("maxTreeDepth", maxTreeDepth, Arrays.asList(1, 3, 8, 10, 30, 100));
        addTunableParameter("epsilon", epsilon);
        // Keep heuristic tunable (defaults to GroupAAHeuristic)
        addTunableParameter("biasWeight", biasWeight, Arrays.asList(0.0, 0.01, 0.05, 0.1, 0.2, 0.5));
        addTunableParameter("heuristic", this.heuristic);

        // New tunables for rollout policy
        // We store identifier strings or objects; here we expose policy object directly (simplest)
        //addTunableParameter("rolloutEpsilon", rolloutEpsilon, Arrays.asList(0.0, 0.01, 0.05, 0.1));
        // rolloutPolicy is not easily enumerated; we still expose as a parameter for completeness
        addTunableParameter("rolloutPolicy", null);
    }

    @Override
    public void _reset() {
        super._reset();
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        epsilon = (double) getParameterValue("epsilon");
        biasWeight = (double) getParameterValue("biasWeight");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");

        // read rollout extras (safely)
        Object rp = getParameterValue("rolloutPolicy");
        if (rp instanceof GroupAARolloutPolicy) rolloutPolicy = (GroupAARolloutPolicy) rp;
        //rolloutEpsilon = (double) getParameterValue("rolloutEpsilon");
    }

    @Override
    protected GroupAAParams _copy() {
        return new GroupAAParams();
    }

    @Override
    public IStateHeuristic getStateHeuristic() {
        return heuristic;
    }

    // convenience getter for the rollout policy
    public GroupAARolloutPolicy getRolloutPolicy() {
        return rolloutPolicy;
    }

    // convenience setter
    public void setRolloutPolicy(GroupAARolloutPolicy p) {
        setParameterValue("rolloutPolicy", p);
        this.rolloutPolicy = p;
    }

    @Override
    public SushiGoAgentGroupAA instantiate() {
        return new SushiGoAgentGroupAA((GroupAAParams) this.copy());
    }
}