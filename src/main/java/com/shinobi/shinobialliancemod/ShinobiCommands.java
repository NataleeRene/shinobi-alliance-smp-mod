package com.shinobi.shinobialliancemod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShinobiCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shinobi")
            .then(Commands.literal("setkage")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("village", StringArgumentType.word())
                        .executes(context -> setKage(context))
                    )
                )
            )
            .then(Commands.literal("unsetkage")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> unsetKage(context))
                )
            )
            .then(Commands.literal("checkrank")
                .executes(context -> checkRank(context))
            )
            .then(Commands.literal("claims")
                .executes(ctx -> openClaimsHud(ctx))
            )
            .then(Commands.literal("hud")
                .then(Commands.literal("pin").executes(ctx -> pinHud(ctx)))
                .then(Commands.literal("unpin").executes(ctx -> unpinHud(ctx)))
                .then(Commands.literal("hide").executes(ctx -> unpinHud(ctx)))
                .then(Commands.literal("show").executes(ctx -> pinHud(ctx)))
                .then(Commands.literal("progress")
                    .then(Commands.literal("on").executes(ctx -> toggleProgress(ctx, true)))
                    .then(Commands.literal("off").executes(ctx -> toggleProgress(ctx, false)))
                )
            )
            .then(Commands.literal("resync")
                .executes(ctx -> resyncSelf(ctx))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> resyncOther(ctx))
                )
            )
            .then(Commands.literal("points")
                .executes(context -> showPoints(context))
            )
            .then(Commands.literal("reset")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> resetPlayer(context))
                )
            )
        );

        // Village selection commands
        dispatcher.register(Commands.literal("village")
            .then(Commands.literal("choose")
                .then(Commands.argument("village", StringArgumentType.word())
                    .executes(ctx -> chooseVillage(ctx))
                )
            )
            .then(Commands.literal("list")
                .executes(ctx -> listVillages(ctx))
            )
        );
        
        // War commands
        dispatcher.register(Commands.literal("declarewar")
            .then(Commands.argument("targets", StringArgumentType.greedyString())
                .executes(context -> declareWarMulti(context))
            )
        );
        
        dispatcher.register(Commands.literal("endwar")
            .then(Commands.argument("playerName", StringArgumentType.word())
                .executes(context -> endWar(context))
            )
        );
        
        dispatcher.register(Commands.literal("warstatus")
            .executes(context -> warStatus(context))
        );
        
        dispatcher.register(Commands.literal("warwho")
            .executes(context -> warWho(context))
            .then(Commands.argument("playerName", StringArgumentType.word())
                .executes(context -> warWhoPlayer(context))
            )
        );

        dispatcher.register(Commands.literal("war")
            .then(Commands.literal("addally")
                .then(Commands.argument("playerName", StringArgumentType.word())
                    .executes(ctx -> addWarAlly(ctx))
                )
            )
            .then(Commands.literal("optin")
                .executes(ctx -> warOptIn(ctx))
            )
            .then(Commands.literal("bypass")
                .then(Commands.argument("playerName", StringArgumentType.word())
                    .executes(ctx -> warBypass(ctx))
                )
            )
        );
    }

    private static int setKage(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String villageId = StringArgumentType.getString(context, "village");
            Village village = Village.fromId(villageId);

            if (village == null) {
                context.getSource().sendFailure(Component.literal("§cInvalid village! Use: leaf, sand, mist, stone, or cloud"));
                return 0;
            }

            MinecraftServer server = context.getSource().getServer();
            
            // Assign village first (in case they don't have it yet)
            LuckPermsService.assignVillage(target, village, server);
            
            // Set as Kage
            LuckPermsService.setKage(target, village, server);
            
            // Update scoreboard team and rank to Kage
            ScoreboardTeamManager.assignPlayerRankTeam(target, village, Rank.KAGE, server);
            PlayerPointsManager.setRank(target, Rank.KAGE);

            // Broadcast message
            Component broadcast = Component.literal("§6[Shinobi Alliance] §eA new " + village.getDisplayName() + 
                " Kage has risen: §a" + target.getName().getString());
            context.getSource().getServer().getPlayerList().broadcastSystemMessage(broadcast, false);

            context.getSource().sendSuccess(() -> Component.literal("§aSet " + target.getName().getString() + 
                " as Kage of " + village.getDisplayName()), true);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    private static int unsetKage(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            MinecraftServer server = context.getSource().getServer();
            Village village = RankManager.getPlayerVillage(target, server);
            
            if (village == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer has no village assigned!"));
                return 0;
            }

            // Remove Kage role
            LuckPermsService.removeKage(target, village, server);
            
            // Recalculate rank based on points
            Rank newRank = PlayerPointsManager.getRank(target);
            ScoreboardTeamManager.assignPlayerRankTeam(target, village, newRank, server);
            PlayerPointsManager.setRank(target, newRank);

            // Broadcast message
            Component broadcast = Component.literal("§6[Shinobi Alliance] §e" + target.getName().getString() + 
                " is no longer the " + village.getDisplayName() + " Kage");
            server.getPlayerList().broadcastSystemMessage(broadcast, false);

            context.getSource().sendSuccess(() -> Component.literal("§aRemoved Kage status from " + 
                target.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * /war bypass <playerName>
     * Both Kage must agree to skip grace; applies bypass immediately when both have voted.
     */
    private static int warBypass(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            MinecraftServer server = source.getServer();

            String targetName = StringArgumentType.getString(context, "playerName");

            // Find target player by name (online only for clarity; offline war still possible but bypass needs both online to apply)
            UUID targetUUID = null;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.getName().getString().equalsIgnoreCase(targetName)) {
                    targetUUID = p.getUUID();
                    break;
                }
            }

            if (targetUUID == null) {
                source.sendFailure(Component.literal("§cPlayer '" + targetName + "' not found online."));
                return 0;
            }

            String result = WarManager.requestGraceBypass(player.getUUID(), targetUUID, server);
            if (result.toLowerCase().contains("cannot") || result.startsWith("No ")) {
                source.sendFailure(Component.literal("§c" + result));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("§e" + result), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * /shinobi reset <player>
     * Admin command to completely reset a player's shinobi profile
     * Works from console or in-game with OP level 3+
     */
    private static int resetPlayer(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            MinecraftServer server = source.getServer();
            
            // Check permission level (3 = admin level)
            if (!source.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) {
                source.sendFailure(Component.literal("§cYou don't have permission to reset players! (Requires OP level 3)"));
                return 0;
            }
            
            // Get admin player (if executed by player) or use console
            ServerPlayer admin = null;
            try {
                admin = source.getPlayerOrException();
            } catch (Exception ignored) {
                // Executed from console, admin stays null
            }
            
            // Perform the reset
            if (admin != null) {
                PlayerResetManager.adminResetPlayer(target, admin, server);
            } else {
                // Console execution - call reset directly
                PlayerResetManager.resetPlayer(target, server);
                source.sendSuccess(() -> Component.literal("§a✅ Successfully reset " + target.getName().getString() + "'s shinobi profile."), true);
                System.out.println("[ShinobiAllianceMod] Console reset completed on " + target.getName().getString());
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * /declarewar <playerName>
     * Declare war on another Kage
     */
    private static int declareWar(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer attacker = source.getPlayerOrException();
            MinecraftServer server = source.getServer();
            
            String targetName = StringArgumentType.getString(context, "playerName");
            
            // Find target player by name
            ServerPlayer defender = null;
            UUID defenderUUID = null;
            
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getName().getString().equalsIgnoreCase(targetName)) {
                    defender = player;
                    defenderUUID = player.getUUID();
                    break;
                }
            }
            
            if (defender == null) {
                source.sendFailure(Component.literal("§cPlayer '" + targetName + "' not found! They must be online."));
                return 0;
            }
            
            // Declare war via WarManager
            boolean success = WarManager.declareWar(attacker, defenderUUID, server);
            
            return success ? 1 : 0;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * /declarewar <name1 name2 ...>
     */
    private static int declareWarMulti(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer attacker;
        try { attacker = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be a player")); return 0; }
        MinecraftServer server = source.getServer();
        String raw = StringArgumentType.getString(context, "targets");
        String[] names = raw.split("\\s+");
        int success = 0;
        for (String name : names) {
            ServerPlayer defender = server.getPlayerList().getPlayers().stream()
                .filter(p -> p.getName().getString().equalsIgnoreCase(name))
                .findFirst().orElse(null);
            if (defender == null) {
                source.sendFailure(Component.literal("§cPlayer '" + name + "' not found"));
                continue;
            }
            if (WarManager.declareWar(attacker, defender.getUUID(), server)) success++;
        }
        return success > 0 ? 1 : 0;
    }

    /** Add ally to all wars the executing Kage is involved in */
    private static int addWarAlly(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer kage;
        try { kage = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be a player")); return 0; }
        MinecraftServer server = source.getServer();
        Rank rank = PlayerPointsManager.getRank(kage);
        if (rank != Rank.KAGE) { source.sendFailure(Component.literal("§cOnly Kage can add war allies")); return 0; }

        String allyName = StringArgumentType.getString(context, "playerName");
        ServerPlayer ally = server.getPlayerList().getPlayers().stream()
            .filter(p -> p.getName().getString().equalsIgnoreCase(allyName))
            .findFirst().orElse(null);
        if (ally == null) { source.sendFailure(Component.literal("§cAlly player must be online")); return 0; }

        int invited = WarManager.requestAllyOptIn(kage.getUUID(), ally.getUUID(), server);
        if (invited > 0) {
            final int invitedFinal = invited;
            source.sendSuccess(() -> Component.literal("§aInvited ally " + allyName + " to " + invitedFinal + " war(s). They must /war optin."), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§cYou are not in any wars to invite allies to."));
            return 0;
        }
    }

    /** /war optin - Kage party members opt-in to active wars they're eligible for */
    private static int warOptIn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be a player")); return 0; }
        MinecraftServer server = source.getServer();
        Rank rank = PlayerPointsManager.getRank(player);
        if (rank != Rank.KAGE) { source.sendFailure(Component.literal("§cOnly Kage can opt in.")); return 0; }

        int joined = 0;
        for (var war : WarManager.getAllWars()) {
            // Opt into any pending invitations for current wars
            joined += WarManager.optInAlly(player.getUUID(), server);
        }
        if (joined > 0) {
            final int joinedFinal = joined;
            source.sendSuccess(() -> Component.literal("§aOpted into " + joinedFinal + " war(s). Grace period starts now."), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§cNo wars available to opt into."));
            return 0;
        }
    }

    private static int openClaimsHud(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // Auto-pin HUD so claims are visible
            ShinobiScheduler.pinHud(player);
            player.sendSystemMessage(Component.literal("§aHUD enabled. Claims: §6" + 
                ShinobiScheduler.getClaimedChunks(player) + "/" + ShinobiScheduler.getBonusClaims(player)));
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cPlayer only command"));
            return 0;
        }
    }

    /** /village list */
    private static int listVillages(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6Villages: §aleaf§7, §asand§7, §amist§7, §astone§7, §acloud"), false);
        return 1;
    }

    /** /village choose <id> */
    private static int chooseVillage(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MinecraftServer server = context.getSource().getServer();
            String villageId = StringArgumentType.getString(context, "village").toLowerCase();
            Village village = Village.fromId(villageId);
            if (village == null) {
                player.sendSystemMessage(Component.literal("§cInvalid village. Use /village list"));
                return 0;
            }
            if (ShinobiAllianceMod.hasSelectedVillage(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cYou have already selected a village."));
                return 0;
            }

            // Assign village & starting rank
            LuckPermsService.assignVillage(player, village, server);
            ScoreboardTeamManager.assignPlayerRankTeam(player, village, Rank.GENIN, server);
            PlayerPointsManager.setRank(player, Rank.GENIN);
            ShinobiClaimBridge.applyClaimLimits(player);

            // Mark selected
            ShinobiAllianceMod.clearPlayerCache(player.getUUID());
            PlayerFreezeManager.unfreezePlayer(player);

            // Remove one village scroll if present (iterate through slots safely)
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                var stack = inv.getItem(i);
                if (!stack.isEmpty() && ShinobiItems.isVillageScroll(stack)) {
                    stack.shrink(1);
                    inv.setItem(i, stack);
                    break;
                }
            }

            player.sendSystemMessage(Component.literal("§aVillage selected: §6" + village.getDisplayName() + " §7(You are now a Genin)"));
            server.getPlayerList().broadcastSystemMessage(Component.literal("§6[Shinobi] §f" + player.getName().getString() + " joined the §e" + village.getDisplayName() + " Village"), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cPlayer only"));
            return 0;
        }
    }
    private static int pinHud(CommandContext<CommandSourceStack> context) {
        try { 
            ServerPlayer p = context.getSource().getPlayerOrException(); 
            ShinobiScheduler.pinHud(p); 
            p.sendSystemMessage(Component.literal("§aHUD pinned. Use §e/shinobi hud hide§a to remove."));
            ShinobiScheduler.sendHudStatus(p); 
            return 1; 
        } catch (Exception e) { return 0; }
    }
    private static int unpinHud(CommandContext<CommandSourceStack> context) {
        try { ServerPlayer p = context.getSource().getPlayerOrException(); ShinobiScheduler.unpinHud(p); p.sendSystemMessage(Component.literal("§7HUD hidden.")); return 1; } catch (Exception e) { return 0; }
    }
    private static int toggleProgress(CommandContext<CommandSourceStack> context, boolean enabled) {
        try { 
            ServerPlayer p = context.getSource().getPlayerOrException(); 
            ShinobiScheduler.toggleProgress(p, enabled); 
            p.sendSystemMessage(Component.literal(enabled ? "§aProgress bar enabled." : "§7Progress bar disabled.")); 
            return 1; 
        } catch (Exception e) { return 0; }
    }
    private static int resyncSelf(CommandContext<CommandSourceStack> context) {
        try { ServerPlayer p = context.getSource().getPlayerOrException(); LuckPermsService.syncPlayerGroups(p, context.getSource().getServer()); ShinobiClaimBridge.applyClaimLimits(p); p.sendSystemMessage(Component.literal("§aResync complete.")); return 1; } catch (Exception e) { return 0; }
    }
    private static int resyncOther(CommandContext<CommandSourceStack> context) {
        CommandSourceStack src = context.getSource();
        if (!src.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) { src.sendFailure(Component.literal("§cRequires OP level 3")); return 0; }
        try { ServerPlayer target = EntityArgument.getPlayer(context, "player"); LuckPermsService.syncPlayerGroups(target, src.getServer()); ShinobiClaimBridge.applyClaimLimits(target); src.sendSuccess(() -> Component.literal("§aResync complete for " + target.getName().getString()), false); return 1; } catch (Exception e) { src.sendFailure(Component.literal("§c" + e.getMessage())); return 0; }
    }

    /**
     * /endwar <playerName>
     * End war with another Kage (only the person who declared war can end it)
     */
    private static int endWar(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            MinecraftServer server = source.getServer();
            
            String targetName = StringArgumentType.getString(context, "playerName");
            
            // Find target player by name
            UUID targetUUID = null;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.getName().getString().equalsIgnoreCase(targetName)) {
                    targetUUID = p.getUUID();
                    break;
                }
            }
            
            if (targetUUID == null) {
                source.sendFailure(Component.literal("§cPlayer '" + targetName + "' not found!"));
                return 0;
            }
            
            UUID playerUUID = player.getUUID();
            
            // Only allow the attacker (who declared war) to end it
            if (!WarManager.isAtWar(playerUUID, targetUUID)) {
                source.sendFailure(Component.literal("§cYou cannot end this war. Only the player who declared war can end it."));
                return 0;
            }
            
            // End the war
            boolean success = WarManager.endWar(playerUUID, targetUUID, server);
            
            return success ? 1 : 0;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * /warstatus
     * List all active wars
     */
    private static int warStatus(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            MinecraftServer server = source.getServer();
            
            var wars = WarManager.getAllWars();
            
            if (wars.isEmpty()) {
                source.sendSuccess(() -> Component.literal("🕊 There are no active wars."), false);
                return 1;
            }
            
            source.sendSuccess(() -> Component.literal("§6⚔ Active Wars:"), false);
            
            for (var war : wars) {
                String attackerName = getPlayerNameFromUUID(war.attackerUUID, server);
                String defenderName = getPlayerNameFromUUID(war.defenderUUID, server);
                
                // Get ranks for display
                ServerPlayer attacker = server.getPlayerList().getPlayer(war.attackerUUID);
                ServerPlayer defender = server.getPlayerList().getPlayer(war.defenderUUID);
                
                String attackerDisplay = attacker != null ? 
                    PlayerPointsManager.getRank(attacker).getDisplayName() + " " + attackerName : 
                    "Kage " + attackerName;
                    
                String defenderDisplay = defender != null ? 
                    PlayerPointsManager.getRank(defender).getDisplayName() + " " + defenderName : 
                    "Kage " + defenderName;

                // Grace remaining
                long remainingMs = getWarGraceRemaining(war.attackerUUID, war.defenderUUID);
                String graceText = remainingMs > 0 ? (" §7(Grace: " + (remainingMs / 60000) + " min left)") : " §a(Active)";
                source.sendSuccess(() -> Component.literal("§c  • §6" + attackerDisplay + " §cvs §6" + defenderDisplay + graceText), false);

                // Allies list with their grace
                var allies = getWarAlliesWithNames(war.attackerUUID, war.defenderUUID, server);
                if (!allies.isEmpty()) {
                    for (String line : allies) {
                        source.sendSuccess(() -> Component.literal("    " + line), false);
                    }
                }
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * /warwho <playerName>
     * Show wars involving a specific player
     */
    /**
     * /warwho
     * Show all wars and their status
     */
    private static int warWho(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            MinecraftServer server = source.getServer();
            
            var wars = WarManager.getAllWars();
            
            if (wars.isEmpty()) {
                source.sendSuccess(() -> Component.literal("🕊 There are no active wars."), false);
                return 1;
            }
            
            source.sendSuccess(() -> Component.literal("§6⚔ Active Wars:"), false);
            
            for (var war : wars) {
                String attackerName = getPlayerNameFromUUID(war.attackerUUID, server);
                String defenderName = getPlayerNameFromUUID(war.defenderUUID, server);
                
                // Get ranks for display
                ServerPlayer attacker = server.getPlayerList().getPlayer(war.attackerUUID);
                ServerPlayer defender = server.getPlayerList().getPlayer(war.defenderUUID);
                
                String attackerDisplay = attacker != null ? 
                    PlayerPointsManager.getRank(attacker).getDisplayName() + " " + attackerName : 
                    "Kage " + attackerName;
                    
                String defenderDisplay = defender != null ? 
                    PlayerPointsManager.getRank(defender).getDisplayName() + " " + defenderName : 
                    "Kage " + defenderName;
                
                // Grace remaining
                long remainingMs = getWarGraceRemaining(war.attackerUUID, war.defenderUUID);
                String graceText = remainingMs > 0 ? (" §7(Grace: " + (remainingMs / 60000) + " min left)") : " §a(Active)";
                source.sendSuccess(() -> Component.literal("  §c• §6" + attackerDisplay + " §cvs §6" + defenderDisplay + graceText), false);
                
                // Allies list with their grace and names
                var allies = getWarAlliesWithNames(war.attackerUUID, war.defenderUUID, server);
                if (!allies.isEmpty()) {
                    for (String line : allies) {
                        source.sendSuccess(() -> Component.literal("    " + line), false);
                    }
                }
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * /warwho <playerName>
     * Show wars involving a specific player
     */
    private static int warWhoPlayer(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            MinecraftServer server = source.getServer();
            
            String targetName = StringArgumentType.getString(context, "playerName");
            
            // Find target player by name
            UUID targetUUID = null;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.getName().getString().equalsIgnoreCase(targetName)) {
                    targetUUID = p.getUUID();
                    break;
                }
            }
            
            if (targetUUID == null) {
                source.sendFailure(Component.literal("§cPlayer '" + targetName + "' not found!"));
                return 0;
            }
            
            var wars = WarManager.getWarsForPlayer(targetUUID);
            
            if (wars.isEmpty()) {
                source.sendSuccess(() -> Component.literal("🕊 §6" + targetName + " §ris not involved in any wars."), false);
                return 1;
            }
            
            // Collect opponent names
            List<String> opponents = new ArrayList<>();
            for (var war : wars) {
                UUID opponentUUID = war.attackerUUID.equals(targetUUID) ? war.defenderUUID : war.attackerUUID;
                String opponentName = getPlayerNameFromUUID(opponentUUID, server);
                opponents.add(opponentName);
            }
            
            String opponentList = String.join(", ", opponents);
            source.sendSuccess(() -> Component.literal("⚔ §6" + targetName + " §ris at war with: §c" + opponentList), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Helper: Get player name from UUID
     */
    private static String getPlayerNameFromUUID(UUID uuid, MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            return player.getName().getString();
        }
        return uuid.toString();
    }

    private static long getWarGraceRemaining(UUID attacker, UUID defender) {
        try {
            java.lang.reflect.Method isGraceOver = WarManager.class.getDeclaredMethod("isGracePeriodOver", java.util.UUID.class, java.util.UUID.class);
            isGraceOver.setAccessible(true);
            boolean over = (boolean) isGraceOver.invoke(null, attacker, defender);
            if (over) return 0;
            // Access start time via warStartTimes (not publicly exposed); show approximate remaining
            java.lang.reflect.Field warStartTimes = WarManager.class.getDeclaredField("warStartTimes");
            warStartTimes.setAccessible(true);
            @SuppressWarnings("unchecked") java.util.Map<String, Long> starts = (java.util.Map<String, Long>) warStartTimes.get(null);
            String key = attacker.toString() + ":" + defender.toString();
            Long start = starts.get(key);
            if (start == null) return 0;
            long elapsed = System.currentTimeMillis() - start;
            long total = 60L * 60L * 1000L;
            return Math.max(0, total - elapsed);
        } catch (Throwable ignored) {}
        return 0;
    }

    private static java.util.List<String> getWarAllies(UUID attacker, UUID defender) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        try {
            java.lang.reflect.Field warAlliesField = WarManager.class.getDeclaredField("warAllies");
            warAlliesField.setAccessible(true);
            @SuppressWarnings("unchecked") java.util.Map<String, Object> warAllies = (java.util.Map<String, Object>) warAlliesField.get(null);
            String key = attacker.toString() + ":" + defender.toString();
            Object wa = warAllies.get(key);
            if (wa == null) return lines;
            java.lang.reflect.Field attackerSide = wa.getClass().getDeclaredField("attackerSide");
            java.lang.reflect.Field defenderSide = wa.getClass().getDeclaredField("defenderSide");
            attackerSide.setAccessible(true);
            defenderSide.setAccessible(true);
            @SuppressWarnings("unchecked") java.util.Set<java.util.UUID> aSide = (java.util.Set<java.util.UUID>) attackerSide.get(wa);
            @SuppressWarnings("unchecked") java.util.Set<java.util.UUID> dSide = (java.util.Set<java.util.UUID>) defenderSide.get(wa);

            java.lang.reflect.Field allyGraceStartsField = WarManager.class.getDeclaredField("allyGraceStarts");
            allyGraceStartsField.setAccessible(true);
            @SuppressWarnings("unchecked") java.util.Map<String, java.util.Map<java.util.UUID, Long>> allyStarts = (java.util.Map<String, java.util.Map<java.util.UUID, Long>>) allyGraceStartsField.get(null);
            java.util.Map<java.util.UUID, Long> map = allyStarts.get(key);

            java.util.function.Function<java.util.UUID, String> format = (uuid) -> {
                long remain = 0;
                if (map != null && map.containsKey(uuid)) {
                    Long start = map.get(uuid);
                    if (start != null) {
                        long elapsed = System.currentTimeMillis() - start;
                        long total = 60L * 60L * 1000L;
                        remain = Math.max(0, total - elapsed);
                    } else {
                        remain = totalWaitingText();
                    }
                }
                return "§7- Ally §f" + uuid.toString().substring(0, 8) + (remain > 0 ? (" §7(Ally grace: " + (remain / 60000) + "m)") : " §a(active)");
            };

            for (java.util.UUID u : aSide) lines.add(format.apply(u));
            for (java.util.UUID u : dSide) lines.add(format.apply(u));
        } catch (Throwable ignored) {}
        return lines;
    }

    private static java.util.List<String> getWarAlliesWithNames(UUID attacker, UUID defender, MinecraftServer server) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        try {
            java.lang.reflect.Field warAlliesField = WarManager.class.getDeclaredField("warAllies");
            warAlliesField.setAccessible(true);
            @SuppressWarnings("unchecked") java.util.Map<String, Object> warAllies = (java.util.Map<String, Object>) warAlliesField.get(null);
            String key = attacker.toString() + ":" + defender.toString();
            Object wa = warAllies.get(key);
            if (wa == null) return lines;
            java.lang.reflect.Field attackerSide = wa.getClass().getDeclaredField("attackerSide");
            java.lang.reflect.Field defenderSide = wa.getClass().getDeclaredField("defenderSide");
            attackerSide.setAccessible(true);
            defenderSide.setAccessible(true);
            @SuppressWarnings("unchecked") java.util.Set<java.util.UUID> aSide = (java.util.Set<java.util.UUID>) attackerSide.get(wa);
            @SuppressWarnings("unchecked") java.util.Set<java.util.UUID> dSide = (java.util.Set<java.util.UUID>) defenderSide.get(wa);

            java.lang.reflect.Field allyGraceStartsField = WarManager.class.getDeclaredField("allyGraceStarts");
            allyGraceStartsField.setAccessible(true);
            @SuppressWarnings("unchecked") java.util.Map<String, java.util.Map<java.util.UUID, Long>> allyStarts = (java.util.Map<String, java.util.Map<java.util.UUID, Long>>) allyGraceStartsField.get(null);
            java.util.Map<java.util.UUID, Long> map = allyStarts.get(key);

            java.util.function.Function<java.util.UUID, String> format = (uuid) -> {
                String allyName = getPlayerNameFromUUID(uuid, server);
                long remain = 0;
                if (map != null && map.containsKey(uuid)) {
                    Long start = map.get(uuid);
                    if (start != null) {
                        long elapsed = System.currentTimeMillis() - start;
                        long total = 60L * 60L * 1000L;
                        remain = Math.max(0, total - elapsed);
                    } else {
                        remain = totalWaitingText();
                    }
                }
                return "§7- Ally §f" + allyName + (remain > 0 ? (" §7(Ally grace: " + (remain / 60000) + "m)") : " §a(active)");
            };

            for (java.util.UUID u : aSide) lines.add(format.apply(u));
            for (java.util.UUID u : dSide) lines.add(format.apply(u));
        } catch (Throwable ignored) {}
        return lines;
    }

    private static long totalWaitingText() { return 60L * 60L * 1000L; }

    /**
     * Check and update player's rank based on their completed advancements
     */
    private static int checkRank(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MinecraftServer server = context.getSource().getServer();
            
            // Scan all advancements and award points
            int totalPoints = 0;
            int newAdvancements = 0;
            
            for (var entry : server.getAdvancements().getAllAdvancements()) {
                AdvancementHolder advancement = entry;
                
                // Check if advancement is tracked in our system
                if (AdvancementPointSystem.isTracked(advancement)) {
                    // Check if player has completed this advancement
                    var progress = player.getAdvancements().getOrStartProgress(advancement);
                    
                    if (progress.isDone()) {
                        int points = AdvancementPointSystem.getPoints(advancement);
                        totalPoints += points;
                        newAdvancements++;
                    }
                }
            }
            
            // Update player's points
            PlayerPointsManager.setPoints(player, totalPoints);
            
            // Check for rank promotion
            Rank oldRank = PlayerPointsManager.getRank(player);
            boolean rankChanged = RankManager.checkAndUpdateRank(player, server);
            Rank newRank = PlayerPointsManager.getRank(player);
            Village village = RankManager.getPlayerVillage(player, server);
            
            // Send feedback
            player.sendSystemMessage(Component.literal("§6=== Shinobi Rank Status ==="));
            player.sendSystemMessage(Component.literal("§eAdvancements Completed: §f" + newAdvancements + "/" + AdvancementPointSystem.getTotalAdvancements()));
            player.sendSystemMessage(Component.literal("§eTotal Points: §f" + totalPoints + "/" + AdvancementPointSystem.getMaxPoints()));
            player.sendSystemMessage(Component.literal("§eCurrent Rank: §6" + newRank.getDisplayName(village)));
            
            if (rankChanged) {
                player.sendSystemMessage(Component.literal("§a✦ Rank updated from " + oldRank.getDisplayName(village) + " to " + newRank.getDisplayName(village) + "!"));
            }
            
            // Show next rank threshold
            if (newRank != Rank.KAGE) {
                Rank nextRank = Rank.values()[newRank.ordinal() + 1];
                int pointsNeeded = nextRank.getMinPoints() - totalPoints;
                player.sendSystemMessage(Component.literal("§7Next: §6" + nextRank.getDisplayName(village) + 
                    " §7(§f" + pointsNeeded + " points§7)"));
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Show player's current points and rank
     */
    private static int showPoints(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MinecraftServer server = context.getSource().getServer();
            Village village = RankManager.getPlayerVillage(player, server);
            
            int points = PlayerPointsManager.getPoints(player);
            Rank rank = PlayerPointsManager.getRank(player);
            
            player.sendSystemMessage(Component.literal("§6Your Stats:"));
            player.sendSystemMessage(Component.literal("§ePoints: §f" + points));
            player.sendSystemMessage(Component.literal("§eRank: §6" + rank.getDisplayName(village)));
            
            if (rank != Rank.KAGE) {
                Rank nextRank = Rank.values()[rank.ordinal() + 1];
                int pointsNeeded = nextRank.getMinPoints() - points;
                player.sendSystemMessage(Component.literal("§7" + pointsNeeded + " points until §6" + nextRank.getDisplayName(village)));
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
}
