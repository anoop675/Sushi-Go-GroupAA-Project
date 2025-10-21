package games.puertorico;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import games.puertorico.PuertoRicoConstants.Crop;
import games.puertorico.actions.*;
import games.puertorico.components.ProductionBuilding;

import java.util.List;

public class PuertoRicoActionHeuristic001 implements IActionHeuristic {

    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState gameState, List<AbstractAction> contextActions) {
        // This just needs to return a value that is higher for better actions
        int player = gameState.getCurrentPlayer();
        PuertoRicoGameState state = (PuertoRicoGameState) gameState;

        if (action instanceof Build buildAction) {

            // more expensive buildings are better
            return buildAction.type.cost + buildAction.type.vp;
        } else if (action instanceof DrawPlantation plantationAction) {
            // we prefer plantations we have unused capacity for
            Crop PlantationType = plantationAction.crop;
            int numPlantations = (int) state.getPlayerBoard(player).getPlantations().stream()
                    .filter(p -> p.crop == PlantationType).count();
            int capacity = state.getPlayerBoard(player).getBuildings().stream()
                    .filter(b -> b instanceof ProductionBuilding)
                    .mapToInt(b -> b.buildingType.capacity).sum();
            if (capacity > numPlantations) {
                return 40.0;
            } else {
                return 10.0;
            }
        } else if (action instanceof BuildQuarry) {
            // this is good early in the game
            return 50 - 5 * state.getRoundCounter();
        } else if (action instanceof DiscardGoodsExcept discardAction) {
            int discardedGoods = 0;
            for (Crop crop : Crop.getTradeableCrops()) {
                if (crop != discardAction.crop && !state.getPlayerBoard(player).cropsInWarehouses.contains(crop)) {
                    discardedGoods += state.getPlayerBoard(player).getStoresOf(crop);
                }
            }
            return -discardedGoods * 5.0;
        } else if (action instanceof GainCrop gainCropAction) {
            return gainCropAction.amount * gainCropAction.crop.price;
        } else if (action instanceof OccupyBuilding occupyBuildingAction) {
            return occupyBuildingAction.building.cost;
        } else if (action instanceof OccupyPlantation occupyPlantationAction) {
            return occupyPlantationAction.crop.price;
        } else if (action instanceof SelectRole selectRoleAction) {
            double cashBonus = state.getMoneyOnRole(selectRoleAction.role) * Math.max(0.0, 5.0 - state.getDoubloons(player));
            switch (selectRoleAction.role) {
                case BUILDER:
                    return cashBonus + state.getDoubloons(player) * 3.0;
                case CAPTAIN:
                    return cashBonus + state.getPlayerBoard(player).getStores().values().stream().mapToInt(i -> i).sum();
                case CRAFTSMAN:
                    return cashBonus + 5.0 - state.getPlayerBoard(player).getStores().values().stream().mapToInt(i -> i).sum();
                case MAYOR:
                    return cashBonus + state.getPlayerBoard(player).getTownVacancies() + state.getPlayerBoard(player).getPlantationVacancies();
                case PROSPECTOR:
                    return cashBonus * 2.0;
                case SETTLER:
                    return cashBonus - state.getPlayerBoard(player).getPlantationVacancies();
                case TRADER:
                    return cashBonus + state.getPlayerBoard(player).getStores().entrySet().stream()
                            .filter(e -> !state.getMarket().contains(e.getKey()))
                            .filter(e -> e.getValue() > 0)
                            .mapToInt(e -> e.getKey().price).max().orElse(-5) * 5.0;
                default:
                    return 0;
            }
        } else if (action instanceof Sell sellAction) {
            return sellAction.salesPrice * 5.0;
        } else if (action instanceof ShipCargo shipCargoAction) {
            return shipCargoAction.amountToShip * 3.0;
        } else if (action instanceof WarehouseStorage warehouseStorageAction) {
            return warehouseStorageAction.storedCrop.price * state.getPlayerBoard(player).getStoresOf(warehouseStorageAction.storedCrop);
        } else {
            return 0;
        }
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof PuertoRicoActionHeuristic001;
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
