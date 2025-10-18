package groupAA;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;

import java.util.List;

public class MyAgentGroupAA extends BasicMCTSPlayer {

    // Placeholder for future GroupAA-specific configuration

    public MyAgentGroupAA() { super(System.currentTimeMillis()); }

    public MyAgentGroupAA(long seed) { super(seed); }

    // Custom parameter defaults could be set here in future if needed

    public MyAgentGroupAA(BasicMCTSParams params) { super(params); }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // For now, use the default BasicMCTS implementation
        return super._getAction(gameState, actions);
    }

    @Override
    public MyAgentGroupAA copy() { return new MyAgentGroupAA((BasicMCTSParams) parameters.copy()); }
}
