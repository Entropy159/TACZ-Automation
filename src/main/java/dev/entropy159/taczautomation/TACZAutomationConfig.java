package dev.entropy159.taczautomation;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashMap;

public class TACZAutomationConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final HashMap<String, ModConfigSpec.BooleanValue> TOGGLES = new HashMap<>();

    public static final ModConfigSpec SPEC;

    static {
        TACZAutomation.ALLOWED.forEach(type -> TOGGLES.put(type.toString(), BUILDER.define("allow." + type.toLanguageKey(), true)));
        SPEC = BUILDER.build();
    }
}
