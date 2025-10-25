package groupAA;

import core.AbstractGameState;
import core.actions.AbstractAction;
import java.util.List;
import java.util.Random;

public interface GroupAARolloutPolicy {
    AbstractAction chooseAction(
            AbstractGameState state,
            List<AbstractAction> actions,
            int playerId, Random rnd
    );
}