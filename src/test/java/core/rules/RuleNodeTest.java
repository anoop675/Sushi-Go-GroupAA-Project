package core.rules;

import core.AbstractGameState;
import core.AbstractGameStateWithTurnOrder;
import core.CoreConstants;
import core.CoreConstants.GameResult;
import core.rules.nodetypes.RuleNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Unit tests for RuleNode behavior when multiple GameOverConditions fire.
 */
public class RuleNodeTest {

    private static class MinimalGameState extends AbstractGameStateWithTurnOrder {

        protected MinimalGameState() {
            super(null, 2);
        }

        @Override
        protected core.turnorders.TurnOrder _createTurnOrder(int nPlayers) {
            return new core.turnorders.StandardTurnOrder(nPlayers);
        }

        @Override
        protected AbstractGameStateWithTurnOrder __copy(int playerId) {
            MinimalGameState copy = new MinimalGameState();
            // copy minimal state if needed
            return copy;
        }

        @Override
        protected void _reset() {
            // nothing required for this minimal test
        }

        @Override
        protected java.util.List<core.components.Component> _getAllComponents() {
            return new ArrayList<>();
        }

        @Override
        protected AbstractGameState _copy(int playerId) {
            return __copy(playerId);
        }

        @Override
        protected double _getHeuristicScore(int playerId) {
            return 0;
        }

        @Override
        public double getGameScore(int playerId) {
            return 0;
        }

        @Override
        protected boolean _equals(Object o) {
            return o instanceof MinimalGameState;
        }

        @Override
        protected games.GameType _getGameType() {
            return games.GameType.GameTemplate;
        }

        @Override
        protected Object getComponent(int componentHash) {
            return null;
        }
    }

    private static class TestRuleNode extends RuleNode {
        @Override
        protected boolean run(AbstractGameStateWithTurnOrder gs) {
            // do nothing
            return true;
        }
    }

    @Test
    public void multiGameOverConditionSelectsMostSevere() {
        MinimalGameState gs = new MinimalGameState();
        TestRuleNode node = new TestRuleNode();

        // Create two conditions: one that returns LOSE_GAME and one that returns DISQUALIFY
        GameOverCondition loseCond = new GameOverCondition() {
            @Override
            public CoreConstants.GameResult test(core.AbstractGameState gs) {
                return GameResult.LOSE_GAME;
            }
        };
        GameOverCondition disqCond = new GameOverCondition() {
            @Override
            public CoreConstants.GameResult test(core.AbstractGameState gs) {
                return GameResult.DISQUALIFY;
            }
        };

        // Attach in the order where a later, less severe condition would previously overwrite.
        node.addGameOverCondition(loseCond);
        node.addGameOverCondition(disqCond);

        // Execute the node which should evaluate game over conditions
        node.execute(gs);

        // The most severe (DISQUALIFY) should have been applied
        Assert.assertEquals(gs.getGameStatus(), GameResult.DISQUALIFY);
    }
}
