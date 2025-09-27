package com.codinglitch.simpleradio.core.registry.blocks;

import com.codinglitch.simpleradio.central.Receiving;
import com.codinglitch.simpleradio.central.Routing;
import com.codinglitch.simpleradio.central.Transmitting;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RadarArrayBlock extends BaseEntityBlock implements Routing, Receiving, Transmitting {
  public RadarArrayBlock(final Properties properties) {
    super(properties);
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
    return new RadarArrayBlockEntity(pos, state);
  }

  @Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
    return createTickerHelper(type, com.codinglitch.simpleradio.core.registry.SimpleRadioBlockEntities.RADAR_ARRAY, RadarArrayBlockEntity::tick);
  }
}