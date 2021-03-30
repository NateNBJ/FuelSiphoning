package fuel_siphoning.campaign.abilities;

import java.awt.Color;
import java.util.EnumSet;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetMemberViewAPI;
import fuel_siphoning.ModPlugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseToggleAbility;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class SiphonFuelAbility extends BaseToggleAbility {
	public static final Color CONTRAIL_COLOR = new Color(255, 97, 27, 80);

    public float getFuelPerSupply(boolean isHighDensity) {
        return (isHighDensity ? ModPlugin.HIGH_DENSITY_CONVERSION_RATIO : ModPlugin.LOW_DENSITY_CONVERSION_RATIO)
                * Global.getSector().getEconomy().getCommoditySpec(Commodities.SUPPLIES).getBasePrice()
                / Global.getSector().getEconomy().getCommoditySpec(Commodities.FUEL).getBasePrice();
    }
    
    @Override
    protected String getActivationText() {
        if (Commodities.SUPPLIES != null && getFleet() != null
                && (getFleet().getCargo().getSupplies() <= 0
                || getFleet().getCargo().getFuel() >= getFleet().getCargo().getMaxFuel())) {
            return null;
        } else return "Siphoning Fuel";
    }

    @Override
    protected void activateImpl() { }

    @Override
    public boolean showActiveIndicator() { return isActive(); }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        Color gray = Misc.getGrayColor();
        Color highlight = Misc.getHighlightColor();

        String status = " (off)";
        if (turnedOn) {
                status = " (on)";
        }

        LabelAPI title = tooltip.addTitle(spec.getName() + status);
        title.highlightLast(status);
        title.setHighlightColor(gray);

        float pad = 10f;
        tooltip.addPara("Synthesize fuel using the trace antimatter and heavy isotopes of hydrogen found in nebulae.", pad);

        if (!hasFuelSource()) {
            tooltip.addPara("Your fleet must be within a nebula in order to siphon fuel.", Misc.getNegativeHighlightColor(), pad);
        } else {
            boolean inNebulaSystem = getFleet().getContainingLocation().isNebula();
            Color clr = inNebulaSystem ? Misc.getPositiveHighlightColor() : Misc.getHighlightColor();
            String density = (inNebulaSystem ? "high" : "low") + " density nebula";
            String fuelPerSupply = Misc.getRoundedValueMaxOneAfterDecimal(getFuelPerSupply(inNebulaSystem));
            String canOrIs = isActive() ? "is siphoning" : "can siphon";

            tooltip.addPara("Your fleet is within a %s and " + canOrIs + " %s fuel per unit of supplies.",
                        pad, Misc.getTextColor(), clr, density, fuelPerSupply);
        }

        tooltip.addPara("Increases the range at which the fleet can be detected by %s and consumes supplies in exchange for fuel.",
                        pad, highlight, (int)ModPlugin.SENSOR_PROFILE_INCREASE_PERCENT + "%");


        addIncompatibleToTooltip(tooltip, expanded);
    }

    @Override
    public boolean hasTooltip() { return true; }

    @Override
    protected void applyEffect(float amount, float level) {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null) return;
        
        if(!isActive()) return;

        fleet.getStats().getDetectedRangeMod().modifyPercent(getModId(), ModPlugin.SENSOR_PROFILE_INCREASE_PERCENT, "Siphoning fuel");

        boolean inNebulaSystem = getFleet().getContainingLocation().isNebula();
        float days = Global.getSector().getClock().convertToDays(amount);
        float cost = days * fleet.getCargo().getMaxFuel() * (level / getFuelPerSupply(inNebulaSystem))
                * (fleet.getCurrBurnLevel() / 10);
        float fuel = fleet.getCargo().getCommodityQuantity(Commodities.FUEL);

        if (!hasFuelSource()) {
            deactivate();
        } else if(fleet.getCargo().getCommodityQuantity(Commodities.SUPPLIES) <= 0) {
            CommoditySpecAPI spec = getCommodity();
            fleet.addFloatingText("Out of " + spec.getName().toLowerCase(), Misc.setAlpha(entity.getIndicatorColor(), 255), 0.5f);
            deactivate();
        } else if(fuel + cost * getFuelPerSupply(inNebulaSystem) >= fleet.getCargo().getMaxFuel()) {
            fleet.getCargo().addFuel(fleet.getCargo().getMaxFuel() - fuel);
            fleet.addFloatingText("Full of fuel", Misc.setAlpha(entity.getIndicatorColor(), 255), 0.5f);
            deactivate();
        } else {
            fleet.getCargo().removeCommodity(Commodities.SUPPLIES, cost);
            fleet.getCargo().addCommodity(Commodities.FUEL, cost * getFuelPerSupply(inNebulaSystem));

			for (FleetMemberViewAPI view : getFleet().getViews()) {
				view.getContrailColor().shift("sun_fs_wake", CONTRAIL_COLOR, getActivationDays(), 2, 1f);
				view.getContrailWidthMult().shift("sun_fs_wake", 6, getActivationDays(), 2, 1f);
			}
        }
    }

    public CommoditySpecAPI getCommodity() {
        return Global.getSettings().getCommoditySpec(Commodities.SUPPLIES);
    }

    @Override
    public boolean isUsable() {
        return isActive() || hasFuelSource();
    }

    protected boolean hasFuelSource() {
    	return getFleet().getStats().getDetectedRangeMod().getMultBonuses().containsKey("nebula_stat_mod_1");
    }

    @Override
    protected void deactivateImpl() { cleanupImpl(); }

    @Override
    protected void cleanupImpl() {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null) return;

        fleet.getStats().getDetectedRangeMod().unmodify(getModId());
    }
}