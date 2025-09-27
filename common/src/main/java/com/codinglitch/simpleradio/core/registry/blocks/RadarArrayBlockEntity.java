package com.codinglitch.simpleradio.core.registry.blocks;

import com.codinglitch.simpleradio.core.registry.SimpleRadioBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class RadarArrayBlockEntity extends BlockEntity {
  private UUID id = UUID.randomUUID();
  private UUID networkId = new UUID(0L, 0L); // configurable via UI/tool later

  public RadarArrayBlockEntity(final BlockPos pos, final BlockState state) {
    super(SimpleRadioBlockEntities.RADAR_ARRAY, pos, state);
  }

  public static void tick(final Level level, final BlockPos pos, final BlockState state, final RadarArrayBlockEntity be) {
    // Intentionally empty for now — wiring into router graph comes next.
    // Matches pattern used by other block entities that supply a static tick entrypoint.
  }

  @Override
  protected void saveAdditional(final CompoundTag tag) {
    super.saveAdditional(tag);
    tag.putUUID("id", id);
    tag.putUUID("networkId", networkId);
  }

  @Override
  public void load(final CompoundTag tag) {
    super.load(tag);
    if (tag.hasUUID("id")) id = tag.getUUID("id");
    if (tag.hasUUID("networkId")) networkId = tag.getUUID("networkId");
  }
}