package com.codinglitch.simpleradio.radio;

import com.codinglitch.simpleradio.SimpleRadioLibrary;
import com.codinglitch.simpleradio.central.Frequency;
import com.codinglitch.simpleradio.central.WorldlyPosition;
import com.codinglitch.simpleradio.core.registry.blocks.RadarArrayBlockEntity;
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
  public void take(final Source source) {
    if (!this.active) return;
    if (acceptCriteria != null && !acceptCriteria.test(source)) return;

    // 1) Normal local routing (same dimension)
    super.take(source);

    // 2) Cross-dimensional mirroring (guarded by existing config)
    if (!SimpleRadioLibrary.SERVER_CONFIG.frequency.crossDimensional) return;
    final Frequency freq = this.getFrequency();
    if (freq == null) return;

    final WorldlyPosition here = this.getLocation();
    if (here == null) return;

    // Find remote Radar Array transmitters on same frequency in other dimensions
    final List<Transmitter> txs = freq.getTransmitters();
    for (final Transmitter tx : txs) {
      if (!(tx instanceof RadioTransmitter)) continue;

      final Router rtr = (Router) tx;
      if (rtr.getLink() == null || !Objects.equals(rtr.getLink(), RadarArrayBlockEntity.class)) continue;
      if (Objects.equals(rtr.getReference(), this.reference)) continue;

      final WorldlyPosition there = rtr.getLocation();
      if (there == null) continue;
      if (there.level == here.level) continue;

      // Prevent immediate feedback into sender
      if (source.getOwner() != null && source.getOwner().equals(rtr.getReference())) continue;

      // Mirror by forwarding a copy of the Source to the remote transmitter.
      // No need for volume/sound getters; accept() handles routing from the remote location.
      final Source mirrored = source.copy();
      mirrored.setOwner(this.reference);

      // Optional: apply a simple hop penalty using existing knobs
      // Dimensional interference scalar and thresholds to reduce effective power
      final double dimK = SimpleRadioLibrary.SERVER_CONFIG.frequency.dimensionalInterference;
      final int floor = SimpleRadioLibrary.SERVER_CONFIG.receiver.receptionFloor;
      final int thresh = (freq.getModulation() == Frequency.Modulation.FREQUENCY)
                   ? SimpleRadioLibrary.SERVER_CONFIG.transmitter.diminishThresholdFM
                   : SimpleRadioLibrary.SERVER_CONFIG.transmitter.diminishThresholdAM;
      final float hopCost = (float) (dimK * Math.max(floor, thresh));

      final float remaining = Math.max(0f, mirrored.getPower() - hopCost);
      mirrored.setPower(remaining);

      ((Router) tx).accept(mirrored);
    }
  }
}
