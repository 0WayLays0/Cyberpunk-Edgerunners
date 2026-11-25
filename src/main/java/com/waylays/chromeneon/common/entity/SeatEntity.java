package com.waylays.chromeneon.common.entity;

import com.waylays.chromeneon.common.block.RipperdocChairBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The invisible seat of a ripperdoc chair: the player rides it while in the chair. It belongs to its chair block,
 * disappears as soon as nobody sits on it or the chair is gone, and is never saved.
 */
public class SeatEntity extends Entity {
    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No synced data: a seat is only a position
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (getPassengers().isEmpty()
                || !(level().getBlockState(blockPosition()).getBlock() instanceof RipperdocChairBlock))) {
            discard();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Never saved (see shouldBeSaved)
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Never saved (see shouldBeSaved)
    }
}
