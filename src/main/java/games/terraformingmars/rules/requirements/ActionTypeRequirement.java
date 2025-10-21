package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.TMCard;

import java.awt.*;

public record ActionTypeRequirement(TMTypes.ActionType actionType,
                                    TMTypes.StandardProject project) implements Requirement<TMAction> {

    public ActionTypeRequirement copy() {
        return this;
    }

    @Override
    public boolean testCondition(TMCard o) {
        return o.actionType == actionType
                && (project == null && o.standardProject == null
                || o.standardProject == project);
    }

    @Override
    public boolean max() {
        return false;
    }

    @Override
    public boolean appliesWhenAnyPlayer() {
        return false;
    }

    @Override
    public String getDisplayText(TMGameState gs) {
        return null;
    }

    @Override
    public String getReasonForFailure(TMGameState gs) {
        return null;
    }

    @Override
    public Image[] getDisplayImages() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActionTypeRequirement that)) return false;
        return actionType == that.actionType && project == that.project;
    }

    @Override
    public String toString() {
        return "Action Type";
    }
}
