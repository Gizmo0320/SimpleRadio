package com.codinglitch.simpleradio.radio;

import com.codinglitch.simpleradio.CommonSimpleRadio;
import com.codinglitch.simpleradio.CompatCore;
import com.codinglitch.simpleradio.SimpleRadioLibrary;
import com.codinglitch.simpleradio.central.FrequencingType;
import com.codinglitch.simpleradio.central.Frequency;
import com.codinglitch.simpleradio.central.WorldlyPosition;
import com.codinglitch.simpleradio.routers.DimensionalRelay;
import com.codinglitch.simpleradio.routers.Router;
import com.codinglitch.simpleradio.routers.Transmitter;
import com.codinglitch.simpleradio.routers.Receiver;

import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A Router that receives in one dimension then re-transmits in others
 * via matching Radar Arrays on the same frequency and Network ID.
 */
public class RadarArrayRelay extends RadioRouter implements DimensionalRelay {
  protected Frequency frequency;
  protected FrequencingType frequencingType;
  protected UUID id;
  protected UUID networkId;
  protected int antennaPower;
  protected boolean active = true;

  public RadarArrayRelay(final Frequency frequency, final WorldlyPosition location, final UUID id, final UUID networkId) {
    this.frequency = frequency;
    this.location = location;
    this.reference = id;
    this.id = id;
    this.networkId = networkId;
  }

  public static boolean matchesNetwork(final RadarArrayRelay a, final RadarArrayRelay b) {
    return a != null && b != null && Objects.equals(a.networkId, b.networkId);
  }

  @Override
  public boolean shouldRouteTo(final RadioSource source, final RadioRouter destination) {
    // Normal leg budget (same dim, same as transmitter logic)
    if (destination instanceof final RadioReceiver receiver) {
      final FrequencingType type = source.frequencingType == -1? this.frequencingType : source.getFrequencingType();
      final double transmissionPower = source.frequencingType == -1? this.getPower(this.frequency.getModulation()) : source.transmissionPower;

      final double distance = this.getLocation().distance(receiver.getLocation());
      final double cost = distance * type.transmissionDiminishment;

      return (transmissionPower + receiver.getPower()) >= cost;
    }

    // Allow array->array routing to create interdimensional mirror (handled in take)
    if (destination instanceof final RadarArrayRelay relay) {
      // Don’t chain to self; don’t loop; must share network
      if (source.getOwner() != null && source.getOwner().equals(relay.reference)) return false;
      return matchesNetwork(this, relay);
    }

    return super.shouldRouteTo(source, destination);
  }

  @Override
  public RadioSource prepareSource(final RadioSource source, final RadioRouter destination) {
    if (source.frequencingType == -1) {
      final float transmissionPower = getPower(frequency.getModulation());
      source.frequencingType = this.frequencingType.id;
      source.transmissionCap = transmissionPower;
      source.addPower(transmissionPower);
    }
    return super.prepareSource(source, destination);
  }

  @Override
  public void take(final Source source) {
    if (!this.active) return;
    if (acceptCriteria != null && !acceptCriteria.test(source)) return;

    // 1) Route locally (same dimension) just like a transmitter.
    this.route(source, router -> {
      // Avoid feedback into this same router
      return source.getOwner() == null || !source.getOwner().equals(router.reference);
    });

    // 2) Mirror to other dimensions: For each RadarArrayRelay on this frequency with same network ID and different level,
    //    create a mirrored source and emit from that relay. Apply hop cost from config.
    if (!SimpleRadioServerConfig.INSTANCE.radarArray.enabled) return;

    final List<Router> relays = this.frequency.getRouters((Predicate<Router>)r ->
                                                            r instanceof final RadarArrayRelay other
                                                            && other != this
                                                            && other.isInterdimensionalEnabled()
                                                            && matchesNetwork(this, other)
                                                         );

    for (final Router r : relays) {
      final RadarArrayRelay remote = (RadarArrayRelay) r;

      // Only cross-dimension mirror
      if (remote.getLocation().level == this.getLocation().level) continue;

      final float hopCost = SimpleRadioServerConfig.INSTANCE.radarArray.getHopCost(this.frequency.getModulation(), this.getLocation(), remote.getLocation());

      final RadioSource mirrored = source.copy(); // Implement a safe copy in RadioSource if not present
      mirrored.setOwner(this.reference);    // prevent immediate feedback loops
      mirrored.transmissionCap -= hopCost;
      if (mirrored.transmissionCap <= 0) continue;

      // Start new path from remote array’s position
      mirrored.setLocation(remote.getLocation());

      // Local emit from remote relay to receivers in that dimension
      remote.route(mirrored, router -> {
        return mirrored.getOwner() == null || !mirrored.getOwner().equals(router.reference);
      });
    }
  }

  // Transmitter API
  @Override public int getAntennaPower() { return this.antennaPower; }
  @Override public float getPower(final Frequency.Modulation modulation) {
    // Match current Transmitter logic: base power from config, plus antenna aptitude scaling
    return SimpleRadioServerConfig.INSTANCE.radarArray.getBasePower(modulation, this.getAntennaPower());
  }
  @Override public FrequencingType getFrequencingType() { return this.frequencingType; }
  @Override public Transmitter frequency(final Frequency frequency) { this.frequency = frequency; return this; }
  @Override public Transmitter frequencingType(final FrequencingType type) { this.frequencingType = type; return this; }

  // Receiver API (if you gate reception power/aptitude similarly to Receiver)
  public int getReceptionPower() {
    return SimpleRadioServerConfig.INSTANCE.radarArray.receptionPower;
  }

  // DimensionalRelay
  @Override public UUID getNetworkId() { return this.networkId; }
  @Override public DimensionalRelay networkId(final UUID id) { this.networkId = id; return this; }

  public boolean isInterdimensionalEnabled() {
    return SimpleRadioServerConfig.INSTANCE.radarArray.enabled;
  }
}
