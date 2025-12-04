package com.benmac.cobblemonrf;

import com.cobblemon.mod.common.Cobblemon;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = CobblemonRF.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
        private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        private static final ForgeConfigSpec.IntValue RF_PER_HEALER_CHARGE = BUILDER
                        .comment("RF per healer charge")
                        .defineInRange("rfPerHealerCharge", 500, 0, Integer.MAX_VALUE);

        private static final ForgeConfigSpec.IntValue MAX_RECEIVE_PER_TICK = BUILDER
                        .comment("Max receive per tick")
                        .defineInRange("maxReceivePerTick", 50, 0, Integer.MAX_VALUE);

        static final ForgeConfigSpec SPEC = BUILDER.build();

        public static int rfPerHealerCharge;
        public static int maxReceivePerTick;
        public static int maxHealerRF;

        @SubscribeEvent
        static void onLoad(final ModConfigEvent event) {
                rfPerHealerCharge = RF_PER_HEALER_CHARGE.get();
                maxReceivePerTick = MAX_RECEIVE_PER_TICK.get();

                // Overwrite Cobblemon's chargeGainedPerTick to 0.0 and read maxHealerCharge
                // using reflection
                try {
                        Object config = Cobblemon.config;

                        Field chargeField = config.getClass().getDeclaredField("chargeGainedPerTick");
                        chargeField.setAccessible(true);
                        chargeField.setFloat(config, 0.0f);

                        Field maxField = config.getClass().getDeclaredField("maxHealerCharge");
                        maxField.setAccessible(true);
                        float maxHealerCharge = (Float) maxField.get(config);
                        maxHealerRF = (int) (rfPerHealerCharge * maxHealerCharge);
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
}
