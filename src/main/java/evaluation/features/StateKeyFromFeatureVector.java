package evaluation.features;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateKey;

import java.util.Arrays;

public record StateKeyFromFeatureVector(IStateFeatureVector featureVector) implements IStateKey {

    @Override
    public String getKey(AbstractGameState state, int playerId) {
        double[] retValue = featureVector.doubleVector(state, playerId);
        return String.format("%d-%s", playerId, Arrays.toString(retValue));
    }
}
