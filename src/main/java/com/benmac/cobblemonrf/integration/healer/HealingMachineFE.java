package com.benmac.cobblemonrf.integration.healer;

import com.benmac.cobblemonrf.Config;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.benmac.cobblemonrf.CobblemonRF.MODID;

/**
 * Healing Machine Forge Energy Integration
 * Attaches FE capability to Cobblemon HealingMachineBlockEntity
 */
@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HealingMachineFE {
    private static final ResourceLocation FE_KEY = new ResourceLocation(MODID, "healing_machine_fe");

    // Attach FE to every HealingMachineBlockEntity
    @SubscribeEvent
    public static void onAttachCaps(AttachCapabilitiesEvent<BlockEntity> event) {
        // Check if it's a HealingMachineBlockEntity from Cobblemon
        if (!"com.cobblemon.mod.common.block.entity.HealingMachineBlockEntity"
                .equals(event.getObject().getClass().getName()))
            return;

        FEProvider provider = new FEProvider(event.getObject());
        event.addCapability(FE_KEY, provider);
        event.addListener(provider::invalidate);
    }

    // Energy capability provider with save/load support
    private static final class FEProvider implements ICapabilitySerializable<CompoundTag> {
        private static final String NBT_ENERGY = "cobblemonrf_energy";

        private final BlockEntity blockEntity;
        private final HealingEnergyStorage storage;
        private final LazyOptional<IEnergyStorage> opt;

        FEProvider(BlockEntity blockEntity) {
            this.blockEntity = blockEntity;
            this.storage = new HealingEnergyStorage(blockEntity);
            this.opt = LazyOptional.of(() -> storage);
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.ENERGY)
                return opt.cast();
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt(NBT_ENERGY, storage.getEnergyStored());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            if (nbt.contains(NBT_ENERGY)) {
                int energy = nbt.getInt(NBT_ENERGY);
                storage.setEnergy(energy);
                blockEntity.setChanged();
            }
        }

        void invalidate() {
            opt.invalidate();
        }
    }

    // Custom energy storage with healing machine logic
    public static final class HealingEnergyStorage extends EnergyStorage {
        private final BlockEntity blockEntity;

        HealingEnergyStorage(BlockEntity blockEntity) {
            super((int) Config.maxHealerRF, (int) Config.maxReceivePerTick, 0); // No extraction
            this.blockEntity = blockEntity;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);

            if (!simulate && received > 0) {

                // Compute equivalent Cobblemon "charge"
                float newCharge = (float) this.energy / Config.rfPerHealerCharge;

                try {
                    CompoundTag savedTag = blockEntity.saveWithoutMetadata();
                    float currentCharge = savedTag.getFloat("MachineCharge");

                    // bullshit logic for updating only on energy recieve since I can't figure out
                    // how to get mixins to work.
                    // System.err.println(currentCharge + ", " + newCharge);
                    if (currentCharge < newCharge - 0.5f) {
                        this.energy = (int) (currentCharge * Config.rfPerHealerCharge);
                    } else {
                        savedTag.putFloat("MachineCharge", newCharge);
                        blockEntity.load(savedTag);
                        blockEntity.setChanged();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to update MachineCharge NBT: " + e.getMessage());
                }
            }

            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            // Healing machine doesn't output energy
            return 0;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        public void setEnergy(int value) {
            this.energy = Math.max(0, Math.min(value, this.capacity));
            blockEntity.setChanged();
        }
    }
}
