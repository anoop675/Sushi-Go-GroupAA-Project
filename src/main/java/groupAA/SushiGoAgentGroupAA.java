package groupAA;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import java.util.Random;
import java.util.logging.Logger;

public class SushiGoAgentGroupAA extends AbstractPlayer {

    private static final Logger LOGGER = Logger.getLogger(SushiGoAgentGroupAA.class.getName());
    // Store our algorithm-specific params separately from AbstractPlayer.parameters to avoid type clashes
    private AMAF_Params amafParams;

    public SushiGoAgentGroupAA(AMAF_Params params) {
        super(params, "GroupAA MCTS Agent");
        this.amafParams = params != null ? params : new AMAF_Params();
        long seed = this.amafParams != null ? this.amafParams.getRandomSeed() : System.currentTimeMillis(); // adapt field name if needed
        this.rnd = new Random(seed);
        LOGGER.info("SushiGoAgentGroupAA initialized and ready!");
    }

    public SushiGoAgentGroupAA() {
        this(System.currentTimeMillis());
    }

    public SushiGoAgentGroupAA(long randomSeed) {
        super(new AMAF_Params(), "GroupAA MCTS Agent");
        // store the seed for reproducibility
        // keep AbstractPlayer.parameters in sync but do not rely on its concrete type elsewhere
        parameters.setRandomSeed(randomSeed);
        // also maintain our local params
        this.amafParams = new AMAF_Params();
        this.amafParams.setRandomSeed(randomSeed);

        this.rnd = new Random(randomSeed);

        // Overwrite AMAF parameter values (getParameters() returns AMAF_Params)
        AMAF_Params params = getParameters();
        params.K = Math.sqrt(2);
        params.rolloutLength = 10;
        params.maxTreeDepth = 5;
        params.epsilon = 1e-6;
        LOGGER.info("AMAF Parameters initialized!");
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, java.util.List<AbstractAction> actions) {
        GroupAATreeNode node = new GroupAATreeNode(this, null, gameState, this.rnd);
        LOGGER.info("SushiGoAgentGroupAA performing search and finding the best action for this gameState: " + gameState.toString());
        node.mctsSearch();
        return node.bestAction();
    }

    @Override
    public AMAF_Params getParameters() {
        return amafParams;
    }

    @Override
    public String toString() {
        return "GroupAA MCTS Agent";
    }

    @Override
    public SushiGoAgentGroupAA copy() {
        LOGGER.info("Creating copy of SushiGoAgentGroupAA agent with parameters");
        // copy our local params explicitly
        AMAF_Params parametersCopy = (AMAF_Params) this.amafParams.copy();

        // create a new agent using the params-copy constructor
        SushiGoAgentGroupAA agentCopy = new SushiGoAgentGroupAA(parametersCopy);

        // copy RNG state: seed a new Random with the saved seed (if available)
        long seed = parametersCopy.getRandomSeed();
        agentCopy.rnd = new Random(seed);

        return agentCopy;
    }
}
