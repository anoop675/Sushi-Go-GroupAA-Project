package games.coltexpress;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Loot;
import java.util.LinkedList;

public class ColtExpressHeuristic extends TunableParameters<ColtExpressHeuristic> implements IStateHeuristic {

    double FACTOR_BULLETS_PLAYER = -0.2;
    double FACTOR_BULLETS_ENEMY = 0.1;
    double FACTOR_BULLET_CARDS = -0.3;
    double FACTOR_LOOT = 0.5;
    double FACTOR_SHOOT_CARD = 0.3;
    double FACTOR_PUNCH_CARD = 0.4;
    double FACTOR_COLLECT_CARD = 0.8;
    double FACTOR_TARGET_WITH_LOOT = 0.6;
    double FACTOR_MOVE_MARSHAL = 0.2;
    int maxLoot = 5000;

    // use bullets, minimize bulletsLeft
    // dodge bullets, maximize others' bulletsLeft
    // minimize bullet cards in hand/discard
    // maximize loot value

    public ColtExpressHeuristic() {
        addTunableParameter("maxLoot", 5000);
        addTunableParameter("BULLET_CARDS", -0.3);
        addTunableParameter("BULLETS_PLAYER", -0.2);
        addTunableParameter("BULLETS_ENEMY", 0.1);
        addTunableParameter("LOOT", 0.5);
        addTunableParameter("SHOOT_CARD", 0.3);
        addTunableParameter("PUNCH_CARD", 0.4);
        addTunableParameter("COLLECT_CARD", 0.8);
        addTunableParameter("TARGET_WITH_LOOT", 0.6);
        addTunableParameter("MOVE_MARSHAL", 0.2);
    }

    @Override
    public void _reset() {
        maxLoot = (int) getParameterValue("maxLoot");
        FACTOR_BULLET_CARDS = (double) getParameterValue("BULLET_CARDS");
        FACTOR_BULLETS_PLAYER = (double) getParameterValue("BULLETS_PLAYER");
        FACTOR_BULLETS_ENEMY = (double) getParameterValue("BULLETS_ENEMY");
        FACTOR_LOOT = (double) getParameterValue("LOOT");
        FACTOR_SHOOT_CARD = (double) getParameterValue("SHOOT_CARD");
        FACTOR_PUNCH_CARD = (double) getParameterValue("PUNCH_CARD");
        FACTOR_COLLECT_CARD = (double) getParameterValue("COLLECT_CARD");
        FACTOR_TARGET_WITH_LOOT = (double) getParameterValue("TARGET_WITH_LOOT");
        FACTOR_MOVE_MARSHAL = (double) getParameterValue("MOVE_MARSHAL");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        ColtExpressGameState cegs = (ColtExpressGameState) gs;
        ColtExpressParameters cep = (ColtExpressParameters) gs.getGameParameters();

        if (!cegs.isNotTerminal())
            return cegs.getPlayerResults()[playerId].value;

        // Number of bullets left for the player
        int nBulletsPlayer = cegs.bulletsLeft[playerId] / cep.nBulletsPerPlayer;

        // Number of bullets left for all other players
        int nBulletsOthers = 0;
        for (int i = 0; i < cegs.getNPlayers(); i++) {
            if (i != playerId) {
                nBulletsOthers += cegs.bulletsLeft[i] / cep.nBulletsPerPlayer;
            }
        }

        // Number of bullet cards in the player's deck or hand
        int nMaxBulletCards = cep.nBulletsPerPlayer * (cegs.getNPlayers() - 1);
        int nBulletCards = 0;
        for (ColtExpressCard c : cegs.playerHandCards.get(playerId).getComponents()) {
            if (c.cardType == ColtExpressCard.CardType.Bullet) {
                nBulletCards++;
            }
        }
        for (ColtExpressCard c : cegs.playerDecks.get(playerId).getComponents()) {
            if (c.cardType == ColtExpressCard.CardType.Bullet) {
                nBulletCards++;
            }
        }
        nBulletCards /= nMaxBulletCards;

        // Total value of loot collected by the player
        int lootValue = 0;
        for (Loot loot : cegs.playerLoot.get(playerId).getComponents()) {
            lootValue += loot.getValue();
        }
        lootValue /= maxLoot;

        // Additional heuristics: value shoot/punch/collect cards in hand and targets in the same compartment that have loot
        int nShootCards = 0;
        int nPunchCards = 0;
        int nCollectCards = 0;
        int nMoveMarshalCards = 0;
        for (ColtExpressCard c : cegs.playerHandCards.get(playerId).getComponents()) {
            if (c.cardType == ColtExpressCard.CardType.Shoot) nShootCards++;
            if (c.cardType == ColtExpressCard.CardType.Punch) nPunchCards++;
            if (c.cardType == ColtExpressCard.CardType.CollectMoney) nCollectCards++;
            if (c.cardType == ColtExpressCard.CardType.MoveMarshal) nMoveMarshalCards++;
        }

        // Find player's compartment (if any) and count opponents in same compartment that have loot
        int targetsWithLoot = 0;
        LinkedList<games.coltexpress.components.Compartment> train = cegs.getTrainCompartments();
        int marshalCompIndex = -1;
        int playerCompIndex = -1;
        for (int ci = 0; ci < train.size(); ci++) {
            games.coltexpress.components.Compartment comp = train.get(ci);
            if (comp.containsMarshal) marshalCompIndex = ci;
            if (comp.containsPlayer(playerId)) playerCompIndex = ci;
        }
        for (int opponent = 0; opponent < cegs.getNPlayers(); opponent++) {
            if (opponent == playerId) continue;
            // check if opponent is in same compartment
            if (playerCompIndex != -1) {
                games.coltexpress.components.Compartment comp = train.get(playerCompIndex);
                if (comp.containsPlayer(opponent)) {
                    if (cegs.getLoot(opponent).getSize() > 0) targetsWithLoot++;
                }
            }
        }

        // Move marshal usefulness: check if marshal is currently in a compartment with fewer players than some other compartment
        int playersInMarshalComp = 0;
        int maxPlayersInSomeComp = 0;
        if (marshalCompIndex != -1) {
            games.coltexpress.components.Compartment marshalComp = train.get(marshalCompIndex);
            playersInMarshalComp = marshalComp.playersInsideCompartment.size() + marshalComp.playersOnTopOfCompartment.size();
            for (games.coltexpress.components.Compartment comp : train) {
                int pc = comp.playersInsideCompartment.size() + comp.playersOnTopOfCompartment.size();
                if (pc > maxPlayersInSomeComp) maxPlayersInSomeComp = pc;
            }
        }

    double moveMarshalBenefit = 0.0;
    if (maxPlayersInSomeComp > playersInMarshalComp) moveMarshalBenefit = (double) (maxPlayersInSomeComp - playersInMarshalComp);

    return FACTOR_BULLET_CARDS * nBulletCards + FACTOR_BULLETS_ENEMY * nBulletsOthers + FACTOR_BULLETS_PLAYER * nBulletsPlayer +
        FACTOR_LOOT * lootValue + FACTOR_SHOOT_CARD * nShootCards + FACTOR_PUNCH_CARD * nPunchCards + FACTOR_COLLECT_CARD * nCollectCards +
        FACTOR_TARGET_WITH_LOOT * targetsWithLoot + FACTOR_MOVE_MARSHAL * moveMarshalBenefit;
    }

    @Override
    protected ColtExpressHeuristic _copy() {
        ColtExpressHeuristic retValue = new ColtExpressHeuristic();
        retValue.maxLoot = maxLoot;
        retValue.FACTOR_BULLETS_ENEMY = FACTOR_BULLETS_ENEMY;
        retValue.FACTOR_LOOT = FACTOR_LOOT;
        retValue.FACTOR_BULLETS_PLAYER = FACTOR_BULLETS_PLAYER;
        retValue.FACTOR_BULLET_CARDS = FACTOR_BULLET_CARDS;
        retValue.FACTOR_SHOOT_CARD = FACTOR_SHOOT_CARD;
        retValue.FACTOR_PUNCH_CARD = FACTOR_PUNCH_CARD;
        retValue.FACTOR_COLLECT_CARD = FACTOR_COLLECT_CARD;
        retValue.FACTOR_TARGET_WITH_LOOT = FACTOR_TARGET_WITH_LOOT;
        retValue.FACTOR_MOVE_MARSHAL = FACTOR_MOVE_MARSHAL;
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof ColtExpressHeuristic) {
            ColtExpressHeuristic other = (ColtExpressHeuristic) o;
        return other.FACTOR_BULLET_CARDS == FACTOR_BULLET_CARDS &&
            other.FACTOR_BULLETS_ENEMY == FACTOR_BULLETS_ENEMY &&
            other.FACTOR_BULLETS_PLAYER == FACTOR_BULLETS_PLAYER &&
            other.FACTOR_LOOT == FACTOR_LOOT &&
            other.FACTOR_SHOOT_CARD == FACTOR_SHOOT_CARD &&
            other.FACTOR_PUNCH_CARD == FACTOR_PUNCH_CARD &&
            other.FACTOR_COLLECT_CARD == FACTOR_COLLECT_CARD &&
            other.FACTOR_TARGET_WITH_LOOT == FACTOR_TARGET_WITH_LOOT &&
            other.FACTOR_MOVE_MARSHAL == FACTOR_MOVE_MARSHAL &&
            other.maxLoot == maxLoot;
        }
        return false;
    }

    public ColtExpressHeuristic instantiate() {
        return this._copy();
    }

}