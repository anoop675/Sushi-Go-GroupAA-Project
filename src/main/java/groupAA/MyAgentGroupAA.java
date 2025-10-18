package groupAA;

public class MyAgentGroupAA extends AbstractPlayer {

    public MyAgentGroupAA() {
        this(System.currentTimeMillis());
    }

    // TODO
    // CHANGE THE PARAMETERS TO APPROPRIATE MCTS-RAVE ARGUEMENTS
    //
    public MyAgentGroupAA(long seed) {
        super(new MyAgentGroupAA(), "GroupAA RAVE-MCTS");
        // for clarity we create a new set of parameters here, but we could just use the default parameters
        parameters.setRandomSeed(seed);
        rnd = new Random(seed);

        // TODO
        // CHANGES THIS
        // These parameters can be changed, and will impact the Basic MCTS algorithm
        BasicMCTSParams params = getParameters();
        params.K = Math.sqrt(2);
        params.rolloutLength = 10;
        params.maxTreeDepth = 5;
        params.epsilon = 1e-6;

    }

    public BasicMCTSPlayer(BasicMCTSParams params) {
        super(params, "GroupAA RAVE-MCTS");
        rnd = new Random(params.getRandomSeed());
    }

    // TODO
    // Create new class TreeNode (can extends BasicTreeNode)
    //
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Search for best action from the root
        MyAgentGroupAA root = new BasicTreeNode(this, null, gameState, rnd);

        // mctsSearch does all of the hard work
        root.mctsSearch();

        // Return best action
        return root.bestAction();
    }

    @Override
    public MyAgentGroupAA copy() {
        return new MyAgentGroupAA((BasicMCTSParams) parameters.copy());
    }

    // TODO
    // OVERIDE FUNCTIONS LIKE IN BasicMTCSPlayer
}
