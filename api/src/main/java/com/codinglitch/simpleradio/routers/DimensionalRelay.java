package com.codinglitch.simpleradio.routers;

import com.codinglitch.simpleradio.central.FrequencingType;
import com.codinglitch.simpleradio.central.Frequency;

import java.util.UUID;

/**
 * Marker for routers that bridge across dimensions.
 * Radar Arrays implement both Receiver + Transmitter and mirror sources cross-dimension.
 */
public interface DimensionalRelay extends Receiver, Transmitter {
  // Network ID for pairing arrays across dimensions. Could also be a String.
  UUID getNetworkId();
  DimensionalRelay networkId(UUID id);

  // Whether this relay should participate in cross-dimension mirroring.
  boolean isInterdimensionalEnabled();

  // Same systems used by other routers
  @Override DimensionalRelay frequency(Frequency frequency);
  @Override DimensionalRelay frequencingType(FrequencingType type);
}
