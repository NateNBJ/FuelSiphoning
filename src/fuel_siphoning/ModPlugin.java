package fuel_siphoning;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

public class ModPlugin extends BaseModPlugin {
    @Override
    public void afterGameSave() {
        Global.getSector().getCharacterData().addAbility("sun_fs_siphon_fuel");
    }

    @Override
    public void beforeGameSave() {
        Global.getSector().getCharacterData().removeAbility("sun_fs_siphon_fuel");
    }

    @Override
    public void onGameLoad(boolean newGame) {
        if(!Global.getSector().getPlayerFleet().hasAbility("sun_fs_siphon_fuel")) {
            Global.getSector().getCharacterData().addAbility("sun_fs_siphon_fuel");
        }
    }
}
