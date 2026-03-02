package com.mystichorizons.mysticnametags.commands.admin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.mystichorizons.mysticnametags.MysticNameTagsPlugin;
import com.mystichorizons.mysticnametags.commands.AbstractTagsAdminSubCommand;
import com.mystichorizons.mysticnametags.config.LanguageManager;
import com.mystichorizons.mysticnametags.integrations.IntegrationManager;
import com.mystichorizons.mysticnametags.integrations.StatProvider;
import com.mystichorizons.mysticnametags.playtime.PlaytimeService;
import com.mystichorizons.mysticnametags.stats.PlayerStatManager;
import com.mystichorizons.mysticnametags.util.ColorFormatter;
import com.zib.playtime.api.PlaytimeAPI;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class TagsAdminStatsSubCommand extends AbstractTagsAdminSubCommand {

    private final RequiredArg<PlayerRef> targetArg;

    public TagsAdminStatsSubCommand() {
        super("stats", "Debug MysticNameTags stats for a player");

        // /tagsadmin stats <player>
        this.targetArg = withRequiredArg(
                "target",
                "Target player (must be online)",
                ArgTypes.PLAYER_REF
        );
    }

    @Override
    protected void executeAdmin(@Nonnull CommandContext context) {
        LanguageManager lang = LanguageManager.get();

        if (!hasAdminPermission(context)) {
            context.sender().sendMessage(colored(lang.tr("cmd.admin.no_permission", Map.of(
                    "usage", "/tagsadmin stats <player>"
            ))));
            return;
        }

        PlayerRef target = context.get(targetArg);
        if (target == null) {
            context.sender().sendMessage(colored("&cPlayer not found or not online.&r"));
            return;
        }

        UUID uuid = target.getUuid();
        String name = target.getUsername();

        MysticNameTagsPlugin plugin = MysticNameTagsPlugin.getInstance();
        if (plugin == null) {
            context.sender().sendMessage(colored("&cPlugin instance not available (this should not happen).&r"));
            return;
        }

        IntegrationManager integrations = plugin.getIntegrations();
        if (integrations == null) {
            context.sender().sendMessage(colored("&cIntegrationManager not available.&r"));
            return;
        }

        StatProvider statProvider = integrations.getStatProvider();

        // ---------------------------------------------------------------------
        // 1) Playtime (resolved provider via IntegrationManager)
        // ---------------------------------------------------------------------
        Integer playtimeMinutes = null;
        try {
            playtimeMinutes = integrations.getPlaytimeMinutes(uuid);
        } catch (Throwable t) {
            plugin.getLogger().at(Level.WARNING)
                    .withCause(t)
                    .log("[MysticNameTags] Error resolving playtime (provider) for " + uuid);
        }

        // ---------------------------------------------------------------------
        // 2) Internal playtime via PlayerStatManager (custom.playtime_seconds)
        // ---------------------------------------------------------------------
        Long internalSeconds = null;
        Long internalMinutes = null;
        try {
            PlayerStatManager mgr = PlayerStatManager.get();
            if (mgr != null) {
                long sec = mgr.getStatLong(uuid, PlaytimeService.STAT_KEY);
                internalSeconds = sec;
                internalMinutes = sec / 60L;
            }
        } catch (Throwable t) {
            plugin.getLogger().at(Level.FINE)
                    .withCause(t)
                    .log("[MysticNameTags] Error reading internal playtime stat for " + uuid);
        }

        // ---------------------------------------------------------------------
        // 3) Zid's Playtime API (if present)
        // ---------------------------------------------------------------------
        Long zibMillis = null;
        Long zibMinutes = null;
        String zibFormatted = null;

        try {
            // Only attempt if the class is present
            Class.forName("com.zib.playtime.api.PlaytimeAPI");

            PlaytimeAPI api = PlaytimeAPI.get();
            if (api != null) {
                zibMillis = api.getTotalPlaytime(uuid);
                if (zibMillis != null && zibMillis > 0L) {
                    zibMinutes = TimeUnit.MILLISECONDS.toMinutes(zibMillis);
                    zibFormatted = api.formatTime(zibMillis);
                }
            }
        } catch (ClassNotFoundException ignored) {
            // Zid's mod not installed; ignore
        } catch (Throwable t) {
            plugin.getLogger().at(Level.FINE)
                    .withCause(t)
                    .log("[MysticNameTags] Error reading Zid Playtime API for " + uuid);
        }

        // ---------------------------------------------------------------------
        // 4) Sample stats via StatProvider / PlayerStatManager
        // ---------------------------------------------------------------------
        Integer damageDealt = null;
        Integer damageTaken = null;
        Integer killsTotal  = null;
        Integer playerKills = null;

        try {
            if (statProvider != null) {
                damageDealt = statProvider.getStatValue(uuid, "custom.damage_dealt");
                damageTaken = statProvider.getStatValue(uuid, "custom.damage_taken");
                killsTotal  = statProvider.getStatValue(uuid, "custom.kills_total");
                playerKills = statProvider.getStatValue(uuid, "killed.Player");
            }
        } catch (Throwable t) {
            plugin.getLogger().at(Level.FINE)
                    .withCause(t)
                    .log("[MysticNameTags] Error reading StatProvider stats for " + uuid);
        }

        // Fallback directly to PlayerStatManager if available
        try {
            PlayerStatManager mgr = PlayerStatManager.get();
            if (mgr != null) {
                if (damageDealt == null) {
                    damageDealt = mgr.getStatValue(uuid, "custom.damage_dealt");
                }
                if (damageTaken == null) {
                    damageTaken = mgr.getStatValue(uuid, "custom.damage_taken");
                }
                if (killsTotal == null) {
                    killsTotal = mgr.getStatValue(uuid, "custom.kills_total");
                }
                if (playerKills == null) {
                    playerKills = mgr.getStatValue(uuid, "killed.Player");
                }
            }
        } catch (Throwable ignored) {
        }

        // ---------------------------------------------------------------------
        // 5) Build debug output
        // ---------------------------------------------------------------------
        StringBuilder sb = new StringBuilder();
        sb.append("&bMysticNameTags Stats Debug&r\n");
        sb.append("&7Player: &f").append(name)
                .append(" &7(").append(uuid).append(")&r\n");

        sb.append("&8------------------------------&r\n");
        sb.append("&ePlaytime (provider)&7: &f")
                .append(playtimeMinutes != null ? playtimeMinutes + " min" : "null")
                .append("&r\n");

        sb.append("&ePlaytime (internal stat)&7: &f");
        if (internalMinutes != null || internalSeconds != null) {
            sb.append(internalMinutes != null ? internalMinutes + " min" : "null")
                    .append(" &7(")
                    .append(internalSeconds != null ? internalSeconds + " sec" : "no seconds")
                    .append(")&r\n");
        } else {
            sb.append("no data&r\n");
        }

        sb.append("&ePlaytime (Zid API)&7: &f");
        if (zibMillis != null) {
            sb.append(zibFormatted != null ? zibFormatted : (zibMinutes != null ? zibMinutes + " min" : zibMillis + " ms"))
                    .append(" &7(")
                    .append(zibMinutes != null ? zibMinutes + " min" : "no minutes")
                    .append(")&r\n");
        } else {
            sb.append("not installed / no data&r\n");
        }

        sb.append("&8------------------------------&r\n");
        sb.append("&eStats (via StatProvider / PlayerStatManager)&r\n");
        sb.append("&7custom.damage_dealt: &f")
                .append(damageDealt != null ? damageDealt : "null").append("&r\n");
        sb.append("&7custom.damage_taken: &f")
                .append(damageTaken != null ? damageTaken : "null").append("&r\n");
        sb.append("&7custom.kills_total: &f")
                .append(killsTotal != null ? killsTotal : "null").append("&r\n");
        sb.append("&7killed.Player: &f")
                .append(playerKills != null ? playerKills : "null").append("&r\n");

        context.sender().sendMessage(ColorFormatter.toMessage(sb.toString()));
    }
}