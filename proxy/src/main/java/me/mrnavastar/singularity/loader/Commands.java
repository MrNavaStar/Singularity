package me.mrnavastar.singularity.loader;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import me.mrnavastar.singularity.common.networking.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class Commands {

    public static void register(CommandManager commandManager, Velocity velocity) {
        CommandMeta commandMeta = commandManager.metaBuilder("singularity").plugin(velocity).build();
        BrigadierCommand command = new BrigadierCommand(BrigadierCommand.literalArgumentBuilder("singularity")
                //.requires(source -> source.hasPermission("singularity.commands"))
                .then(BrigadierCommand.literalArgumentBuilder("config")
                        .executes(Commands::config)
                        .then(BrigadierCommand.requiredArgumentBuilder("group", StringArgumentType.string())
                                .executes(ctx -> settings(ctx, StringArgumentType.getString(ctx, "group")))
                        )
                ));
        commandManager.register(commandMeta, command);
    }

    private static int config(CommandContext<CommandSource> ctx) {
        ComponentBuilder<TextComponent, TextComponent.Builder> builder = Component.text();
        SingularityConfig.getGroups().forEach(group -> builder.append(group.getPretty()));
        ctx.getSource().sendMessage(builder.build());
        return 1;
    }

    private static Component getSetting(String setting, boolean value) {
        String color = value ? "green" : "red";
        return MiniMessage.miniMessage().deserialize(String.format("\n%s : <%s>%b</%s>", setting, color, value, color));
    }

    private static int settings(CommandContext<CommandSource> ctx, String name) {
        SingularityConfig.getGroup(name).ifPresent(group -> {
            Settings settings = group.getSettings();
            ComponentBuilder<TextComponent, TextComponent.Builder> builder = Component.text();
            builder.append(group.getPrettyName());
            builder.append(getSetting("singularity.player", settings.syncPlayerData));
            builder.append(getSetting("singularity.stats", settings.syncPlayerStats));
            builder.append(getSetting("singularity.advancements", settings.syncPlayerAdvancements));
            builder.append(getSetting("singularity.ops", settings.syncOps));
            builder.append(getSetting("singularity.whitelist", settings.syncWhitelist));
            builder.append(getSetting("singularity.bans", settings.syncBans));
            SingularityConfig.getRegisteredBlacklists().forEach(blacklist -> builder.append(getSetting(blacklist, !settings.nbtBlacklists.contains(blacklist))));
            ctx.getSource().sendMessage(builder.build());
        });
        return 1;
    }
}