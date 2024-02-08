package fuel_siphoning;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONObject;

import java.awt.*;
import java.util.MissingResourceException;

public class ModPlugin extends BaseModPlugin {
    public static final String ID = "sun_fuel_siphoning";
    public static final String PREFIX = "sun_fs_";
    public static final String ABILITY_ID = "sun_fs_siphon_fuel";
    public static final String SETTINGS_PATH = "FUEL_SIPHONING_OPTIONS.ini";

    static final String LUNALIB_ID = "lunalib";
    static JSONObject settingsCfg = null;
    static <T> T get(String id, Class<T> type) throws Exception {
        if(Global.getSettings().getModManager().isModEnabled(LUNALIB_ID)) {
            id = PREFIX + id;

            if(type == Integer.class) return type.cast(LunaSettings.getInt(ModPlugin.ID, id));
            if(type == Float.class) return type.cast(LunaSettings.getFloat(ModPlugin.ID, id));
            if(type == Boolean.class) return type.cast(LunaSettings.getBoolean(ModPlugin.ID, id));
            if(type == Double.class) return type.cast(LunaSettings.getDouble(ModPlugin.ID, id));
            if(type == String.class) return type.cast(LunaSettings.getString(ModPlugin.ID, id));
        } else {
            if(settingsCfg == null) settingsCfg = Global.getSettings().getMergedJSONForMod(SETTINGS_PATH, ID);
            
            if(type == Integer.class) return type.cast(settingsCfg.getInt(id));
            if(type == Float.class) return type.cast((float) settingsCfg.getDouble(id));
            if(type == Boolean.class) return type.cast(settingsCfg.getBoolean(id));
            if(type == Double.class) return type.cast(settingsCfg.getDouble(id));
            if(type == String.class) return type.cast(settingsCfg.getString(id));
        }

        throw new MissingResourceException("No setting found with id: " + id, type.getName(), id);
    }
    static int getInt(String id) throws Exception { return get(id, Integer.class); }
    static double getDouble(String id) throws Exception { return get(id, Double.class); }
    static float getFloat(String id) throws Exception { return get(id, Float.class); }
    static boolean getBoolean(String id) throws Exception { return get(id, Boolean.class); }
    static String getString(String id) throws Exception { return get(id, String.class); }
    static boolean readSettings() {
        try {
            FUEL_CONSUMPTION_MULT = Math.max(0, getFloat("fuelConsumptionMult"));
            FUEL_PRICE_MULT = getFloat("fuelPriceMult");
            SENSOR_PROFILE_INCREASE_PERCENT = getFloat("sensorProfileIncreasePercent");
            HIGH_DENSITY_CONVERSION_RATIO = getFloat("highDensityConversionRatio");
            LOW_DENSITY_CONVERSION_RATIO = getFloat("lowDensityConversionRatio");
            CONVERSION_RATE_MULT = getFloat("conversionRateMult");

            MutableStat fuelConsumption = Global.getSector().getPlayerFleet().getStats().getFuelUseHyperMult();

            fuelConsumption.unmodify("sun_ns_fuel_mult"); // In case it was previously modified by Nomadic Survival
            fuelConsumption.modifyMult("sun_fs_fuel_mult", FUEL_CONSUMPTION_MULT);
            CommoditySpecAPI fuelSpec = Global.getSector().getEconomy().getCommoditySpec("fuel");
            fuelSpec.setBasePrice(Math.max(1, ORIGINAL_FUEL_PRICE * FUEL_PRICE_MULT));
        } catch (Exception e) {
            settingsCfg = null;

            return reportCrash(e);
        }

        settingsCfg = null;

        return true;
    }

    public static float
            ORIGINAL_FUEL_PRICE = 25,
            FUEL_CONSUMPTION_MULT = 1,
            FUEL_PRICE_MULT = 1,
            SENSOR_PROFILE_INCREASE_PERCENT = 300,
            HIGH_DENSITY_CONVERSION_RATIO = 1,
            LOW_DENSITY_CONVERSION_RATIO = 0.75f,
            CONVERSION_RATE_MULT = 1.0f;

    @Override
    public void afterGameSave() {
        Global.getSector().getCharacterData().addAbility(ABILITY_ID);
    }

    @Override
    public void beforeGameSave() {
        Global.getSector().getCharacterData().removeAbility(ABILITY_ID);
    }

    @Override
    public void onApplicationLoad() {
        ORIGINAL_FUEL_PRICE = Global.getSettings().getCommoditySpec(Commodities.FUEL).getBasePrice();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        if(!Global.getSector().getPlayerFleet().hasAbility(ABILITY_ID)) {
            Global.getSector().getCharacterData().addAbility(ABILITY_ID);
        }

        if(Global.getSettings().getModManager().isModEnabled(LUNALIB_ID)) {
            LunaSettingsChangedListener.addToManagerIfNeeded();
        }

        readSettings();
    }

    public static boolean reportCrash(Exception exception) {
        try {
            String stackTrace = "", message = "Fuel Siphoning encountered an error!\nPlease let the mod author know.";

            for(int i = 0; i < exception.getStackTrace().length; i++) {
                StackTraceElement ste = exception.getStackTrace()[i];
                stackTrace += "    " + ste.toString() + System.lineSeparator();
            }

            Global.getLogger(ModPlugin.class).error(exception.getMessage() + System.lineSeparator() + stackTrace);

            if (Global.getCombatEngine() != null && Global.getCurrentState() == GameState.COMBAT) {
                Global.getCombatEngine().getCombatUI().addMessage(1, Color.ORANGE, exception.getMessage());
                Global.getCombatEngine().getCombatUI().addMessage(2, Color.RED, message);
            } else if (Global.getSector() != null) {
                CampaignUIAPI ui = Global.getSector().getCampaignUI();

                ui.addMessage(message, Color.RED);
                ui.addMessage(exception.getMessage(), Color.ORANGE);
                ui.showConfirmDialog(message + "\n\n" + exception.getMessage(), "Ok", null, null, null);

                if(ui.getCurrentInteractionDialog() != null) ui.getCurrentInteractionDialog().dismiss();
            } else return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
