package com.benmac.cobblemonrf.mixin;

import com.benmac.cobblemonrf.Config;
import com.benmac.cobblemonrf.integration.healer.HealingMachineFE;
import com.cobblemon.mod.common.block.entity.HealingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Syncs Forge Energy with Cobblemon's healing machine charge usage.
 */
@Mixin(value = HealingMachineBlockEntity.class, remap = false)
@Debug(export = true)
public abstract class HealingMachineBlockEntityMixin extends BlockEntity {

    protected HealingMachineBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow
    public abstract float getHealingCharge();

    @Inject(method = "activate", at = @At(value = "FIELD", target = "Lcom/cobblemon/mod/common/block/entity/HealingMachineBlockEntity;healingCharge:F", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void cobblemonrf$syncEnergyAfterHealing(ServerPlayer player, CallbackInfo ci) {
        float machineCharge = getHealingCharge();
        this.getCapability(ForgeCapabilities.ENERGY).ifPresent(storage -> {
            if (storage instanceof HealingMachineFE.HealingEnergyStorage healingStorage) {
                int newEnergy = Math.min((int) (machineCharge * Config.rfPerHealerCharge), healingStorage.getMaxEnergyStored());
                healingStorage.setEnergy(newEnergy);
            }
        });
    }
}
