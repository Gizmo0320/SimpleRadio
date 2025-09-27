package com.codinglitch.simpleradio.radio;

import com.codinglitch.simpleradio.SimpleRadioLibrary;
import com.codinglitch.simpleradio.central.FrequencingType;
import com.codinglitch.simpleradio.central.Frequency;
import com.codinglitch.simpleradio.central.WorldlyPosition;
import com.codinglitch.simpleradio.core.registry.blocks.RadarArrayBlockEntity;
import com.codinglitch.simpleradio.routers.Receiver;
import com.codinglitch.simpleradio.routers.Router;
import com.codinglitch.simpleradio.routers.Transmitter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Receiver half of a Radar Array. After local routing, mirrors to paired Radar Array transmitters in other dimensions.
 */
public class RadarArrayReceiver extends RadioReceiver {

  public int antennaPower = 0;

  public RadarArrayReceiver(final Frequency frequency, final WorldlyPosition location, final UUID id) {
    super(frequency, location, id);
  }

  @Override
  public RadarArrayReceiver frequencingType(final FrequencingType type) {
    super.frequencingType(type);
    return this;
  }

  @Override
  public RadarArrayReceiver frequency(final Frequency frequency) {
    super.frequency(frequency);
    return this;
  }

  @Override
  public void take(final Source source) {
    if (!this.active) return;
    if (acceptCriteria != null && !acceptCriteria.test(source)) return;

    // 1) Normal local routing (within the same dimension)
    super.take(source);

    // 2) Cross-dimensional mirroring (existing config toggle)
    if (!SimpleRadioLibrary.SERVER_CONFIG.frequency.crossDimensional) return;
    final Frequency freq = this.getFrequency();
    if (freq == null) return;

    final WorldlyPosition here = this.getLocation();
    if (here == null) return;

    // Mirror to other Radar Array transmitters on the same frequency
    final List<Transmitter> txs = freq.getTransmitters();
    for (final Transmitter tx : txs) {
      if (!(tx instanceof final RadioTransmitter rt)) continue;

      // Only mirror to arrays (link marker) and skip self and same dimension
      final Router rtr = (Router) tx;
      if (rtr.getLink() == null || !Objects.equals(rtr.getLink(), RadarArrayBlockEntity.class)) continue;
      if (Objects.equals(rtr.getReference(), this.reference)) continue;

      final WorldlyPosition there = rtr.getLocation();
      if (there == null) continue;
      if (there.level == here.level) continue;

      // Avoid immediate feedback into sender
      if (source.getOwner() != null && source.getOwner().equals(rtr.getReference())) continue;

      // Re-emit from remote transmitter in its own dimension with the same data/volume if present.
      final byte[] data = source.getData();
      if (data != null) {
        // Sender is this array's id to maintain loop-prevention on the remote side
        ((Router) tx).send(data, source.getVolume());
      } else if (source.getSound() != null) {
        // If Source contains a sound event instead of Opus data, mirror that
        ((Router) tx).send(there, this.reference, source.getSoundHolder(), source.getVolume(), source.getPitch(), source.getSeed());
      }
    }
  }
}
