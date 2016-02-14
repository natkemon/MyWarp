/*
 * Copyright (C) 2011 - 2016, MyWarp team and contributors
 *
 * This file is part of MyWarp.
 *
 * MyWarp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWarp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWarp. If not, see <http://www.gnu.org/licenses/>.
 */

package me.taylorkelly.mywarp.teleport.strategy;

import com.google.common.base.Optional;

import me.taylorkelly.mywarp.LocalWorld;
import me.taylorkelly.mywarp.util.Vector3;

/**
 * Corrects positions saved with MyWarp versions prior to version 3 if necessary.
 * <p/>
 * Before version 3 the warp height was equivalent with the Y coordinate of the block. If the warp was located on top of
 * a block that was smaller than a full block (e.g. a half step, a stair, a pressured plate), the height needs to be
 * adjusted or the entity ends up <i>within</i> this block.
 */
public class LegacyPositionCorrectionStrategy implements PositionValidationStrategy {

  @Override
  public Optional<Vector3> getValidPosition(Vector3 originalPosition, LocalWorld world) {
    if (world.getBlock(originalPosition).isNotFullHeight()) {
      originalPosition = originalPosition.add(0, 1, 0);
    }
    return Optional.of(originalPosition);
  }
}
