package com.codinglitch.simpleradio.core.registry.blocks;

import com.codinglitch.simpleradio.SimpleRadioApi;
import com.codinglitch.simpleradio.central.Receiving;
import com.codinglitch.simpleradio.central.Transmitting;
import com.codinglitch.simpleradio.central.WorldlyPosition;
import com.codinglitch.simpleradio.core.registry.SimpleRadioBlockEntities;
import com.codinglitch.simpleradio.core.registry.SimpleRadioFrequencing;
import com.codinglitch.simpleradio.radio.RadarArrayReceiver;
import com.codinglitch.simpleradio.radio.RadarArrayTransmitter;
import com.codinglitch.simpleradio.routers.Receiver;
import com.codinglitch.simpleradio.routers.Transmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RadarArrayBlockEntity extends CatalyzingBlockEntity implements Receiving, Transmitting {

  public boolean isActive = false;
  public boolean isDirty = true;
  public int antennaPower = 0;

  public RadarArrayBlockEntity(final BlockPos pos, final BlockState state) {
    super(SimpleRadioBlockEntities.RADAR_ARRAY, pos, state);
  }

  @Override
  public UUID getReference() {
    if (this.id != null) return this.id;

    // Deterministic fallback: dimension + position -> UUID
    final String dim = this.level != null && this.level.dimension() != null
                 ? this.level.dimension().location().toString()
                 : "unknown";
    final String key = dim + "@" + this.getBlockPos().toShortString();
    return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public BlockPos getAdaptorLocation() {
    if (getBlockState().hasProperty(ReceiverBlock.FACING)) {
      return getBlockPos().relative(getBlockState().getValue(ReceiverBlock.FACING).getOpposite());
    }
    return getBlockPos();
  }

  @Override
  public void setRemoved() {
    inactivate();
    super.setRemoved();
  }

  @Override
  public void loadTag(final CompoundTag tag) {
    super.loadTag(tag);
  }

  @Override
  public void saveTag(final CompoundTag tag) {
    super.saveTag(tag);
    tag.putInt("antennaPower", antennaPower);
  }

  @Override
  public void load(final CompoundTag tag) {
    super.load(tag);
    loadTag(tag);

    if (tag.contains("antennaPower")) {
      this.antennaPower = tag.getInt("antennaPower");
    }
  }

  @Override
  public void saveAdditional(final CompoundTag tag) {
    saveTag(tag);
    super.saveAdditional(tag);
  }

  @Override
  public void markDirty() {
    this.isDirty = true;
  }

  public static void tick(final Level level, final BlockPos pos, final BlockState state, final RadarArrayBlockEntity be) {
    if (be.frequency != null && be.id != null && !be.isActive) {
      be.activate();
    }
    CatalyzingBlockEntity.tick(level, pos, state, be);

    if (!be.catalyzed) {
      if (be.receiver != null) be.receiver.setActive(false);
      if (be.transmitter != null) be.transmitter.setActive(false);
      return;
    }

    if (be.receiver != null) be.receiver.setActive(true);
    if (be.transmitter != null) be.transmitter.setActive(true);

    if (be.isDirty && level.getGameTime() % 200 == 0 && !level.isClientSide) {
      be.antennaPower = be.calculateAntennaPower(be.getAdaptorLocation(), level);

      if (be.receiver instanceof final RadarArrayReceiver rar) {
        rar.antennaPower = be.antennaPower;
      }
      if (be.transmitter instanceof final RadarArrayTransmitter rat) {
        rat.antennaPower = be.antennaPower;
      }

      be.isDirty = false;
      be.setChanged();
    }

    if (!level.isClientSide) {
      if (be.receiver != null && !be.receiver.validate()) {
        be.inactivate();
      }
      if (be.transmitter != null && !be.transmitter.validate()) {
        be.inactivate();
      }
    }
  }

  private void activate() {
    if (this.level == null || this.level.isClientSide) return;
    if (this.frequency == null || this.id == null) return;
    if (this.isActive) return;

    final WorldlyPosition here = WorldlyPosition.of(getBlockPos(), level);

    final RadarArrayReceiver rar = new RadarArrayReceiver(frequency, here, this.id);
    rar.frequencingType(SimpleRadioFrequencing.RECEIVER);
    rar.setLink(RadarArrayBlockEntity.class);
    rar.antennaPower = this.antennaPower;

    final RadarArrayTransmitter rat = new RadarArrayTransmitter(frequency, here, this.id);
    rat.frequencingType(SimpleRadioFrequencing.TRANSMITTER);
    rat.setLink(RadarArrayBlockEntity.class);
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

  private void inactivate() {
    if (this.level != null && !this.level.isClientSide && this.frequency != null) {
      if (this.receiver != null) {
        this.frequency.removeReceiver(this.receiver);
      }
      if (this.transmitter != null) {
        this.frequency.removeTransmitter(this.transmitter);
      }
    }

    this.receiver = null;
    this.transmitter = null;
    this.isActive = false;
  }
}