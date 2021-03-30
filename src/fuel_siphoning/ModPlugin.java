package fuel_siphoning;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import org.json.JSONObject;

public class ModPlugin extends BaseModPlugin {
    public static final String ID = "sun_fuel_siphoning";
    public static final String ABILITY_ID = "sun_fs_siphon_fuel";
    public static final String SETTINGS_PATH = "FUEL_SIPHONING_OPTIONS.ini";

    public static float
        FUEL_CONSUMPTION_MULT = 1,
        FUEL_PRICE_MULT = 25,
        SENSOR_PROFILE_INCREASE_PERCENT = 3,
        HIGH_DENSITY_CONVERSION_RATIO = 1,
        LOW_DENSITY_CONVERSION_RATIO = 0.75f;

    private static boolean settingsAlreadyRead = false;

    @Override
    public void afterGameSave() {
        Global.getSector().getCharacterData().addAbility(ABILITY_ID);
    }

    @Override
    public void beforeGameSave() {
        Global.getSector().getCharacterData().removeAbility(ABILITY_ID);
    }

    @Override
    public void onGameLoad(boolean newGame) {
        try {
            if(!Global.getSector().getPlayerFleet().hasAbility(ABILITY_ID)) {
                Global.getSector().getCharacterData().addAbility(ABILITY_ID);
            }


            if(!settingsAlreadyRead) {
                JSONObject cfg = Global.getSettings().getMergedJSONForMod(SETTINGS_PATH, ID);

                FUEL_CONSUMPTION_MULT = (float) Math.max(0, cfg.getDouble("fuelConsumptionMult"));
                FUEL_PRICE_MULT = (float) cfg.getDouble("fuelPriceMult");
                SENSOR_PROFILE_INCREASE_PERCENT = (float) cfg.getDouble("sensorProfileIncreasePercent");
                HIGH_DENSITY_CONVERSION_RATIO = (float) cfg.getDouble("highDensityConversionRatio");
                LOW_DENSITY_CONVERSION_RATIO = (float) cfg.getDouble("lowDensityConversionRatio");

                settingsAlreadyRead = true;
            }

            if(FUEL_CONSUMPTION_MULT != 1) {
                Global.getSector().getPlayerFleet().getStats().getFuelUseHyperMult().modifyMult("sun_fs_fuel_mult", FUEL_CONSUMPTION_MULT);
            }

            if(FUEL_PRICE_MULT != 1) {
                CommoditySpecAPI fuelSpec = Global.getSector().getEconomy().getCommoditySpec("fuel");
                fuelSpec.setBasePrice(Math.max(1, fuelSpec.getBasePrice() * FUEL_PRICE_MULT));
            }
        } catch (Exception e) {
            String stackTrace = "";

            for(int i = 0; i < e.getStackTrace().length; i++) {
                StackTraceElement ste = e.getStackTrace()[i];
                stackTrace += "    " + ste.toString() + System.lineSeparator();
            }

            Global.getLogger(ModPlugin.class).error(e.getMessage() + System.lineSeparator() + stackTrace);
        }
    }
}
