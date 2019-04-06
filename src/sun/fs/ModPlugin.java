package sun.fs;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

public class ModPlugin extends BaseModPlugin {
    static void log(String message) { if(true) Global.getLogger(ModPlugin.class).info(message); }

    @Override
    public void onGameLoad(boolean newGame) {
        if(!Global.getSector().getPlayerFleet().hasAbility("sun_fs_siphon_fuel")) {
            Global.getSector().getCharacterData().addAbility("sun_fs_siphon_fuel");
        }
    }
}
