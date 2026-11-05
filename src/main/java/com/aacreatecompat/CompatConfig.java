package com.aacreatecompat;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CompatConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue COOLDOWN;
    public static final ModConfigSpec.IntValue BATCH_SIZE;
    public static final ModConfigSpec.BooleanValue DEBUG;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("create_process");
        COOLDOWN = b.comment("The cooldown after laser emission, 0 means no cooldown, and all items below will be immediately processed.")
                .defineInRange("laser_cooldown", 20, 0, 72000);
        BATCH_SIZE = b.comment("The maximum number of items converted from a single belt/depot stack per laser emission. Remaining items stay held and are processed after the cooldown.")
                .defineInRange("batch_size", 64, 1, 4096);
        DEBUG = b.comment("Write diagnostic lines (rate-limited) to the log about belt/depot lookups, holds, conversions and rejections.")
                .define("debug_log", false);
        b.pop();
        SPEC = b.build();
    }
}
