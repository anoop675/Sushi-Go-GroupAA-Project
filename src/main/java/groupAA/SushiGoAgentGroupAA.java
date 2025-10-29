package groupAA;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import java.util.Random;
import java.util.logging.Logger;

public class SushiGoAgentGroupAA extends AbstractPlayer {

    private static final Logger LOGGER = Logger.getLogger(SushiGoAgentGroupAA.class.getName());

    public SushiGoAgentGroupAA(GroupAAParams params) {
        super(params, "GroupAA MCTS Agent");
        initRandom();
        params.heuristic = new GroupAAHeuristic();
        params.setParameterValue("heuristic", this.getParameters().heuristic);

        System.out.println("SushiGoAgentGroupAA initialized and ready!");
    }

    public SushiGoAgentGroupAA() {
        this(new GroupAAParams());
    }

    public SushiGoAgentGroupAA(long randomSeed) {
        super(new GroupAAParams(), "GroupAA MCTS Agent");
        parameters.setRandomSeed(randomSeed);
        initRandom();
        setDefaultParams();
    }

    private void initRandom() {
        long seed = (parameters != null) ? parameters.getRandomSeed() : System.currentTimeMillis();
        if (seed == 0L) seed = System.currentTimeMillis(); //use system time for fallback
        this.rnd = new Random(seed);
        LOGGER.info("Random seed set to: " + seed);
    }

    //Overwrites the default AMAF parameters
    private void setDefaultParams() {
        GroupAAParams params = getParameters();
        params.K = Math.sqrt(2);
        params.rolloutLength = 10;
        params.maxTreeDepth = 8;
        params.epsilon = 1e-6;
        params.heuristic = new GroupAAHeuristic();
        params.setParameterValue("heuristic", this.getParameters().heuristic);

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
    public GroupAAParams getParameters() {
        return (GroupAAParams) parameters;
    }

    @Override
    public String toString() {
        return "GroupAA MCTS Agent";
    }

    @Override
    public SushiGoAgentGroupAA copy() {
        LOGGER.info("Creating copy of SushiGoAgentGroupAA agent with parameters");
        // copy parameters first
        GroupAAParams parametersCopy = (GroupAAParams) parameters.copy();

        // create a new agent using the params-copy constructor
        SushiGoAgentGroupAA agentCopy = new SushiGoAgentGroupAA(parametersCopy);

        // copy RNG state: seed a new Random with the saved seed (if available)
        long seed = parametersCopy.getRandomSeed();
        agentCopy.rnd = new Random(seed);

        return agentCopy;
    }
}