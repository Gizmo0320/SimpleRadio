package com.codinglitch.simpleradio.core.registry.blocks;

import com.codinglitch.simpleradio.SimpleRadioApi;
import com.codinglitch.simpleradio.central.WorldlyPosition;
import com.codinglitch.simpleradio.core.registry.SimpleRadioBlockEntities;
import com.codinglitch.simpleradio.radio.RadarArrayRelay;
import com.codinglitch.simpleradio.routers.Router;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class RadarArrayBlockEntity extends BlockEntity {
  private RadarArrayRelay relay;
  private UUID id = UUID.randomUUID();
  private UUID networkId = new UUID(0L, 0L); // default network 0; set via UI/tool

  public RadarArrayBlockEntity(final BlockPos pos, final BlockState state) {
    super(SimpleRadioBlockEntities.RADAR_ARRAY, pos, state);
  }

  public void onLoad() {
    if (level == null || level.isClientSide) return;
    if (relay == null) {
      final var freq = SimpleRadioApi.getFrequencyFor(this); // resolve stored frequency from item/state
      relay = new RadarArrayRelay(freq, WorldlyPosition.of(pos, level), id, networkId);
      // Set frequencingType consistent with Transceiver
      relay.frequencingType(SimpleRadioApi.getDefaultFrequencingType());
      relay.setLink(this.getClass());
      freq.registerReceiver(relay);
      freq.registerTransmitter(relay);
    }
  }

  public static void tick(final Level level, final BlockPos pos, final BlockState state, final RadarArrayBlockEntity be) {
    if (level.isClientSide) return;
    if (be.relay == null) be.onLoad();
    // update antenna power, etc.
    // be.relay.antennaPower = ...
  }

  @Override
  protected void saveAdditional(final CompoundTag tag) {
    super.saveAdditional(tag);
    tag.putUUID("id", id);
    tag.putUUID("networkId", networkId);
    // save frequency, modules, etc.
  }

  @Override
  public void load(final CompoundTag tag) {
    super.load(tag);
    if (tag.hasUUID("id")) id = tag.getUUID("id");
    if (tag.hasUUID("networkId")) networkId = tag.getUUID("networkId");
  }
}
