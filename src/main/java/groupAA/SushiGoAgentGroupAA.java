package groupAA;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.Random;

public class SushiGoAgentGroupAA extends AbstractPlayer {

    public SushiGoAgentGroupAA(AMAF_Params params) {
        super(params, "GroupAA MCTS Agent");
        long seed = params != null ? params.getRandomSeed() : System.currentTimeMillis(); // adapt field name if needed
        this.rnd = new Random(seed);
    }

    public SushiGoAgentGroupAA() {
        this(System.currentTimeMillis());
    }

    public SushiGoAgentGroupAA(long randomSeed) {
        super(new AMAF_Params(), "GroupAA MCTS Agent");
        // store the seed for reproducibility
        parameters.setRandomSeed(randomSeed);

        this.rnd = new Random(randomSeed);

        // Overwrite AMAF parameter values (getParameters() returns AMAF_Params)
        AMAF_Params params = getParameters();
        params.K = Math.sqrt(2);
        params.rolloutLength = 10;
        params.maxTreeDepth = 5;
        params.epsilon = 1e-6;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, java.util.List<AbstractAction> actions) {
        GroupAATreeNode node = new GroupAATreeNode(this, null, gameState, this.rnd);
        node.mctsSearch();
        return node.bestAction();
    }

    @Override
    public AMAF_Params getParameters() {
        return (AMAF_Params) parameters;
    }

    @Override
    public String toString() {
        return "GroupAA MCTS Agent";
    }

    @Override
    public SushiGoAgentGroupAA copy() {
        // copy parameters first
        AMAF_Params parametersCopy = (AMAF_Params) parameters.copy();

        // create a new agent using the params-copy constructor
        SushiGoAgentGroupAA agentCopy = new SushiGoAgentGroupAA(parametersCopy);

        // copy RNG state: seed a new Random with the saved seed (if available)
        long seed = parametersCopy.getRandomSeed();
        agentCopy.rnd = new Random(seed);

        return agentCopy;
    }
}
