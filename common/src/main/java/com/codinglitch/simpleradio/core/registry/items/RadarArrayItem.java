package com.codinglitch.simpleradio.core.registry.items;

import com.codinglitch.simpleradio.central.Alterable;
import com.codinglitch.simpleradio.central.Frequencing;
import com.codinglitch.simpleradio.central.Module;
import com.codinglitch.simpleradio.core.registry.SimpleRadioBlocks;
import com.codinglitch.simpleradio.core.registry.modules.SimpleRadioModules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Radar Array item that participates in the Radiosmither frequency workflow.
 * Implements the same interfaces as TransmitterItem/ReceiverItem so it can be
 * given a frequency and modulation (MHz) in the Radiosmither.
 */
public class RadarArrayItem extends BlockItem implements Frequencing, Alterable {
  public RadarArrayItem(final Properties settings) {
    super(SimpleRadioBlocks.RADAR_ARRAY, settings);
  }

  @Override
  public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> components, final TooltipFlag tooltip) {
    appendTooltip(stack, components);
    super.appendHoverText(stack, level, components, tooltip);
  }

  @Override
  public void inventoryTick(final ItemStack stack, final Level level, final Entity entity, final int slot, final boolean selected) {
    super.inventoryTick(stack, level, entity, slot, selected);
    tick(stack, level);
  }

  @Override
  public boolean canAcceptUpgrade(final Module upgrade) {
    // Keep it simple and consistent: allow RANGE module like other radio blocks
    return upgrade == SimpleRadioModules.RANGE;
  }
}
