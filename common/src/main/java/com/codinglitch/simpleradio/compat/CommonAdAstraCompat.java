package com.codinglitch.simpleradio.compat;

import com.codinglitch.simpleradio.central.WorldlyPosition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Minimal shim for Ad Astra. Keep it optional; default behavior returns identity.
 * Add lookups for celestial relationships if you want custom hop multipliers per body.
 */
public class CommonAdAstraCompat {
  public static boolean isSpace(final Level level) {
    // Detect Ad Astra dims by namespace or a dimension tag if provided by Ad Astra
    final ResourceKey<Level> key = level.dimension();
    final String ns = key.location().getNamespace();
    return "ad_astra".equals(ns);
  }
}
