package com.codinglitch.simpleradio.core.registry.blocks;

import com.codinglitch.simpleradio.central.AuditoryBlockEntity;
import com.codinglitch.simpleradio.central.Receiving;
import com.codinglitch.simpleradio.central.Routing;
import com.codinglitch.simpleradio.central.Transmitting;
import com.codinglitch.simpleradio.central.WorldlyPosition;
import com.codinglitch.simpleradio.core.registry.SimpleRadioFrequencing;
import com.codinglitch.simpleradio.radio.RadarArrayReceiver;
import com.codinglitch.simpleradio.radio.RadarArrayTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Radar Array Block; mirrors Transmitter/Receiver patterns to preserve frequency NBT to item.
 */
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

  // Preserve frequency/modulation and id when broken (same as Transmitter/Receiver blocks)
  @Override
  public List<ItemStack> getDrops(final BlockState state, final LootParams.Builder builder) {
    final ItemStack stack = new ItemStack(this);
    final BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
    if (be instanceof final AuditoryBlockEntity abe) {
      abe.saveToItem(stack);
    }
    return List.of(stack);
  }

  @Override
  public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, @Nullable final LivingEntity placer, final ItemStack stack) {
    final BlockEntity be = level.getBlockEntity(pos);
    if (be instanceof final AuditoryBlockEntity abe) {
      abe.loadFromItem(stack);
    }
    super.setPlacedBy(level, pos, state, placer, stack);
  }

  // Support catalyst swapping like other catalyzing blocks
  @Override
  public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult result) {
    final BlockEntity be = level.getBlockEntity(pos);
    if (be instanceof final CatalyzingBlockEntity cbe) {
      final InteractionResult res = cbe.trySwapCatalyst(state, level, pos, player, hand, result);
      if (res != null) return res;
    }
    return super.use(state, level, pos, player, hand, result);
  }

  private void activate() {
    if (this.level == null || this.level.isClientSide) return;
    if (this.frequency == null || this.id == null) return;
    if (this.isActive) return;

    final WorldlyPosition here = WorldlyPosition.of(getBlockPos(), level);

    final RadarArrayReceiver rar = new RadarArrayReceiver(frequency, here, this.id);
    rar.frequencingType(SimpleRadioFrequencing.RECEIVER);
    rar.setLink(RadarArrayBlock.class); // FIX: use Block class

    final RadarArrayTransmitter rat = new RadarArrayTransmitter(frequency, here, this.id);
    rat.frequencingType(SimpleRadioFrequencing.TRANSMITTER);
    rat.setLink(RadarArrayBlock.class); // FIX: use Block class

    // keep antenna power assignment if you have it
    rar.antennaPower = this.antennaPower;
    rat.antennaPower = this.antennaPower;

    this.receiver = rar;
    this.transmitter = rat;

    frequency.registerReceiver(this.receiver);
    frequency.registerTransmitter(this.transmitter);

    rar.updateLocation(here);
    rat.updateLocation(here);

    this.isActive = true;
    this.isDirty = true;
    this.setChanged();
  }
}