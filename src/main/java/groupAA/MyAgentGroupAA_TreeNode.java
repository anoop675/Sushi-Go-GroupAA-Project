public class MyAgentGroupAA_TreeNode extends BasicTreeNode {
    protected static final double K_RAVE = 3000.0;

    public MyAgentGroupAA_TreeNode(BasicMCTSPlayer parent, MyAgentGroupAA_TreeNode parentNode, AbstractGameState state, Random rnd) {
        super(parent, parentNode, state, rnd);
    }

    @Override
    protected MyAgentGroupAA_TreeNode createChild(AbstractAction action, AbstractGameState nextState) {
        return new MyAgentGroupAA_TreeNode(player, this, nextState, rnd);
    }

    // OVERIDE SELECTION + BACKPROPAGATE FOR RAVE-MCTS
    //

    // EQUATION FROM : https://www.cs.utexas.edu/~pstone/Courses/394Rspring11/resources/mcrave.pdf
    // Q_combined = (1 - Beta) Q(s,a)+ Beta * Q_RAVE(s,a)
    // UCB_RAVE = Q_combined + C sqrt( logn(N(s,a), N(s,a) )
    @Override
    protected AbstractAction ucb() {
        protected Map<AbstractAction, Double> raveTotValue = new HashMap<>();
        protected Map<AbstractAction, Integer> raveVisits = new HashMap<>();

        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;

        var params = player.getParameters();

        for (AbstractAction action : children.keySet()) {
            BasicTreeNode child = children.get(action);
            if (child == null)
                continue;

            // CALCULATE THE EQUATIONS
            // Q(s,a)
            double qValue = child.getTotValue() / (child.getNVisits() + params.epsilon);

            // Q_RAVE(s,a)
            double qRave = raveTotValue.getOrDefault(action, 0.0)
                    / (raveVisits.getOrDefault(action, 0) + params.epsilon);

            // β = K / (N + N_RAVE + K)
            double beta = K_RAVE /
                    (child.getNVisits() + raveVisits.getOrDefault(action, 0) + K_RAVE);

            // Q + Q_RAVE
            double qCombined = (1 - beta) * qValue + beta * qRave;
            //

            // START
            // REUSE OF BasicTreeNode
            //
            double explorationTerm = params.K * Math.sqrt(
                    Math.log(this.getNVisits() + 1) / (child.getNVisits() + params.epsilon)
            );

            boolean iAmMoving = state.getCurrentPlayer() == player.getPlayerID();
            double uctValue = (iAmMoving ? qCombined : -qCombined) + explorationTerm;
            uctValue = noise(uctValue, params.epsilon, player.getRnd().nextDouble());

            if (uctValue > bestValue) {
                bestAction = action;
                bestValue = uctValue;
            }

            //
            // REUSE OF BasicTreeNode
            // END
        }

        if (bestAction == null)
            throw new AssertionError("We have a null value in UCB : shouldn't really happen!");

        root.fmCallsCount++;
        return bestAction;
    }

    // TODO
    // BACKPROPAGATION need to update the RAVE value
    @Override
    protected void backUp(double result) {
        super.backUp(result); // normal MCTS backup
    }
}