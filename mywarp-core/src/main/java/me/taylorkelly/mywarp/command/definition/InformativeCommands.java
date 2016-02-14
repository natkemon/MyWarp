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

package me.taylorkelly.mywarp.command.definition;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Ordering;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.sk89q.intake.parametric.annotation.Range;
import com.sk89q.intake.parametric.annotation.Switch;
import com.sk89q.intake.util.auth.AuthorizationException;

import me.taylorkelly.mywarp.Actor;
import me.taylorkelly.mywarp.Game;
import me.taylorkelly.mywarp.LocalEntity;
import me.taylorkelly.mywarp.LocalPlayer;
import me.taylorkelly.mywarp.Settings;
import me.taylorkelly.mywarp.command.CommandHandler;
import me.taylorkelly.mywarp.command.paginator.StringPaginator;
import me.taylorkelly.mywarp.command.parametric.binding.PlayerBinding.IllegalCommandSenderException;
import me.taylorkelly.mywarp.command.parametric.binding.WarpBinding.Name;
import me.taylorkelly.mywarp.command.parametric.binding.WarpBinding.Name.Condition;
import me.taylorkelly.mywarp.command.parametric.economy.Billable;
import me.taylorkelly.mywarp.command.printer.AssetsPrinter;
import me.taylorkelly.mywarp.command.printer.InfoPrinter;
import me.taylorkelly.mywarp.economy.FeeProvider.FeeType;
import me.taylorkelly.mywarp.limit.LimitService;
import me.taylorkelly.mywarp.util.Message;
import me.taylorkelly.mywarp.util.Vector3;
import me.taylorkelly.mywarp.util.i18n.DynamicMessages;
import me.taylorkelly.mywarp.warp.Warp;
import me.taylorkelly.mywarp.warp.WarpManager;
import me.taylorkelly.mywarp.warp.authorization.AuthorizationService;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bundles commands that provide information about existing Warps.
 */
public class InformativeCommands {

  private static final DynamicMessages msg = new DynamicMessages(CommandHandler.RESOURCE_BUNDLE_NAME);

  private final WarpManager warpManager;
  private final LimitService limitService;
  private final AuthorizationService authorizationService;
  private final Game game;
  private final Settings settings;

  /**
   * Creates an instance.
   *
   * @param warpManager          the WarpManager used by commands
   * @param limitService         the LimitService used by commands
   * @param authorizationService the AuthorizationService used by commands
   * @param settings             the Settings used by commands
   */
  public InformativeCommands(WarpManager warpManager, LimitService limitService,
                             AuthorizationService authorizationService, Game game, Settings settings) {
    this.limitService = limitService;
    this.game = game;
    this.settings = settings;
    this.warpManager = warpManager;
    this.authorizationService = authorizationService;
  }

  /**
   * Displays a player's assets.
   *
   * @param actor   the Actor
   * @param creator the LocalPlayer
   * @throws IllegalCommandSenderException if no {@code creator} is given and the given Actor is not a player
   */
  @Command(aliases = {"assets", "limits"}, desc = "assets.description", help = "assets.help")
  @Require("mywarp.cmd.assets.self")
  @Billable(FeeType.ASSETS)
  public void assets(Actor actor, @Optional LocalPlayer creator)
      throws IllegalCommandSenderException, AuthorizationException {
    if (creator == null) {
      if (actor instanceof LocalPlayer) {
        creator = (LocalPlayer) actor;
      } else {
        throw new IllegalCommandSenderException(actor);
      }
    } else if (!actor.hasPermission("mywarp.cmd.assets")) {
      throw new AuthorizationException();
    }
    new AssetsPrinter(creator, limitService, settings).print(actor);

  }

  /**
   * Lists viewable Warps.
   *
   * @param actor   the Actor
   * @param page    the page to display
   * @param creator the optional creator
   * @param name    the optional name
   * @param radius  the optional radius
   * @param world   the optional world
   * @throws IllegalCommandSenderException if the {@code r} flag is used by an Actor that is not an Entity
   */
  @Command(aliases = {"list", "alist"}, desc = "list.description", help = "list.help")
  @Require("mywarp.cmd.list")
  @Billable(FeeType.LIST)
  public void list(final Actor actor, @Optional("1") int page, @Switch('c') final String creator,
                   @Switch('n') final String name,
                   @Switch('r') @Range(min = 1, max = Integer.MAX_VALUE) final Integer radius,
                   @Switch('w') final String world) throws IllegalCommandSenderException {

    // build the listing predicate
    List<Predicate<Warp>> predicates = new ArrayList<Predicate<Warp>>();
    predicates.add(authorizationService.isViewable(actor));

    if (creator != null) {
      predicates.add(new Predicate<Warp>() {
        @Override
        public boolean apply(Warp input) {
          com.google.common.base.Optional<String> creatorName = input.getCreator().getName();
          return creatorName.isPresent() && StringUtils.containsIgnoreCase(creatorName.get(), creator);
        }
      });
    }

    if (name != null) {
      predicates.add(new Predicate<Warp>() {
        @Override
        public boolean apply(Warp input) {
          return StringUtils.containsIgnoreCase(input.getName(), name);
        }
      });
    }

    if (radius != null) {
      if (!(actor instanceof LocalEntity)) {
        throw new IllegalCommandSenderException(actor);
      }

      LocalEntity entity = (LocalEntity) actor;

      final UUID worldId = entity.getWorld().getUniqueId();

      final int squaredRadius = radius * radius;
      final Vector3 position = entity.getPosition();
      predicates.add(new Predicate<Warp>() {
        @Override
        public boolean apply(Warp input) {
          return input.getWorldIdentifier().equals(worldId)
                 && input.getPosition().distanceSquared(position) <= squaredRadius;
        }
      });
    }

    if (world != null) {
      predicates.add(new Predicate<Warp>() {
        @Override
        public boolean apply(Warp input) {
          return StringUtils.containsIgnoreCase(input.getWorld(game).getName(), world);
        }
      });
    }

    //query the warps
    List<Warp> warps = Ordering.natural().sortedCopy(warpManager.filter(Predicates.<Warp>and(predicates)));

    Function<Warp, Message> mapping = new Function<Warp, Message>() {

      @Override
      public Message apply(Warp input) {
        // 'name' (xy) by player
        Message.Builder builder = Message.builder();
        builder.append("'");
        builder.append(input);
        builder.append("' (");
        builder.append(input.getWorld(game));
        builder.append(") ");
        builder.append(msg.getString("list.by"));
        builder.append(" ");

        if (actor instanceof LocalPlayer && input.isCreator((LocalPlayer) actor)) {
          builder.append(msg.getString("list.you"));
        } else {
          builder.append(input.getCreator());
        }
        return builder.build();
      }

    };

    // display
    StringPaginator.of(msg.getString("list.heading"), warps).withMapping(mapping).paginate().display(actor, page);
  }

  /**
   * Displays information about a Warp.
   *
   * @param actor the Actor
   * @param warp  the Warp
   */
  @Command(aliases = {"info", "stats"}, desc = "info.description", help = "info.help")
  @Require("mywarp.cmd.info")
  @Billable(FeeType.INFO)
  public void info(Actor actor, @Name(Condition.VIEWABLE) Warp warp) {
    new InfoPrinter(warp, authorizationService, game).print(actor);
  }
}
