package com.codinglitch.simpleradio.radio;

import com.codinglitch.simpleradio.central.FrequencingType;
import com.codinglitch.simpleradio.central.Frequency;
import com.codinglitch.simpleradio.central.WorldlyPosition;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * Transmitter half of a Radar Array. Uses same power math as RadioTransmitter.
 */
public class RadarArrayTransmitter extends RadioTransmitter {

  public RadarArrayTransmitter(final Frequency frequency, final WorldlyPosition location, final UUID id) {
    super(frequency, location, id);
  }
  public RadarArrayTransmitter(final Frequency frequency, final Entity owner, final UUID id) {
    super(frequency, owner, id);
  }

  @Override
  public RadarArrayTransmitter frequencingType(final FrequencingType type) {
    super.frequencingType(type);
    return this;
  }

  @Override
  public RadarArrayTransmitter frequency(final Frequency frequency) {
    super.frequency(frequency);
    return this;
  }
}