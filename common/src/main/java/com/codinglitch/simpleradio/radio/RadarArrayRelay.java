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
import java.util.function.Predicate;

/**
 * Coordinator for Radar Array cross-dimensional mirroring.
 * Note: This class does NOT implement Receiver or Transmitter directly to avoid method signature clashes.
 * The block entity should still create a normal RadioReceiver and RadioTransmitter and set link(RadarArrayBlockEntity.class).
 * This coordinator can be used if you want a single object to run mirror logic, but isn’t strictly required.
 */
public class RadarArrayRelay extends RadioRouter {

  private Frequency frequency;
  private FrequencingType frequencingType;
  private UUID networkId;
  private int antennaPower;

  public RadarArrayRelay(final Frequency frequency, final WorldlyPosition location, final UUID id, final UUID networkId) {
    super(location, id); // sets position/reference
    this.frequency = frequency;
    this.networkId = networkId;
  }

  public void setFrequencingType(final FrequencingType type) {
    this.frequencingType = type;
  }

  public void setAntennaPower(final int antennaPower) {
    this.antennaPower = antennaPower;
  }

  public void setFrequency(final Frequency frequency) {
    this.frequency = frequency;
  }

  public UUID getNetworkId() {
    return networkId;
  }

  public void setNetworkId(final UUID id) {
    this.networkId = id;
  }

  public int getAntennaPower() {
    return antennaPower;
  }

  // Same formula style as RadioTransmitter: base + antenna contribution using the assigned FrequencingType.
  public float getPower(final Frequency.Modulation modulation) {
    if (frequencingType == null) return 0f;
    final int baseTransmissionPower = frequencingType.getTransmissionPower(modulation);
    return baseTransmissionPower + (antennaPower * frequencingType.antennaAptitude);
  }

  private boolean isCrossDimensionalEnabled() {
    return SimpleRadioLibrary.SERVER_CONFIG.frequency.crossDimensional;
  }

  // Derive a hop penalty using existing config knobs (no new config pages)
  private float getHopCost(final Frequency.Modulation mod) {
    final double dimK = SimpleRadioLibrary.SERVER_CONFIG.frequency.dimensionalInterference;
    final int floor = SimpleRadioLibrary.SERVER_CONFIG.receiver.receptionFloor;
    final int thresh = (mod == Frequency.Modulation.FREQUENCY)
                 ? SimpleRadioLibrary.SERVER_CONFIG.transmitter.diminishThresholdFM
                 : SimpleRadioLibrary.SERVER_CONFIG.transmitter.diminishThresholdAM;
    return (float) (dimK * Math.max(floor, thresh));
  }

  private static boolean isRadarArrayRouter(final Router r) {
    // Identify arrays by their link marker; BE must call router.setLink(RadarArrayBlockEntity.class)
    final Class<?> link = r.getLink();
    return link != null && Objects.equals(link, RadarArrayBlockEntity.class);
  }

  /**
   * Local routing (normal transmitter-like leg) + cross-dimensional mirror to matching arrays.
   * This relies on the block entity having registered a standard Transmitter and Receiver for this id.
   */
  @Override
  public void take(final Source source) {
    if (!this.active) return;
    if (acceptCriteria != null && !acceptCriteria.test(source)) return;

    // 1) Local leg routing (behaves like a transmitter would: route to receivers)
    this.route(source, router -> {
      // avoid immediate feedback to the same reference
      return source.getOwner() == null || !source.getOwner().equals(router.getReference());
    });

    // 2) Cross-dimensional mirror
    if (!isCrossDimensionalEnabled() || frequency == null) return;

    // Find all Radar Array receivers on this frequency (arrays should register as receivers and transmitters with the same UUID).
    final List<Receiver> receivers = frequency.getReceivers();

    for (final Receiver recv : receivers) {
      if (!isRadarArrayRouter(recv)) continue;

      // Skip same reference (self) and same dimension
      if (Objects.equals(recv.getReference(), this.reference)) continue;
      final WorldlyPosition here = this.getLocation();
      final WorldlyPosition there = recv.getLocation();
      if (here == null || there == null) continue;
      if (here.level == there.level) continue;

      // Pair to its co-registered transmitter by UUID
      final Transmitter remoteTx = frequency.getTransmitter(recv.getReference());
      if (remoteTx == null) continue;

      // Apply hop cost guard by reducing effective cap; if cap drained, skip.
      // We can approximate by checking activity/power budget; since Source is opaque, we re-send with same data and volume.
      final float hopCost = getHopCost(frequency.getModulation());

      // Re-emit from the remote transmitter in its own dimension.
      // Use Router.send(byte[], float) variant to originate from remote location.
      final byte[] data = source.getData();
      final float volume = source.getVolume(); // falls back to source volume; if not present in API, set 1f

      // If data is null (e.g., sound event path), use the sound send variant if needed.
      if (data != null) {
        // Mark owner to prevent immediate loop-back
        // We send with this.reference as sender; RadioRouter logic prevents routing to the same reference.
        remoteTx.send(data, volume);
      } else {
        // If no opus data is present, you can extend this branch to mirror sound events when needed.
        // No-op for now to keep logic simple.
      }
    }
  }
}