package com.benmac.cobblemonrf.integration.healer;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// RF consumption logic was previously handled in HealingMachineBlockEntityMixin (removed)
@Mod.EventBusSubscriber(modid = "cobblemonrf")
public class HealerRfLogic {
    // Mixin detects MachineCharge drops and consumes RF accordingly
}