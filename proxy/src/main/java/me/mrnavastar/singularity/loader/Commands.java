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
                .requires(source -> source.hasPermission("singularity.commands"))
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

    private static int settings(CommandContext<CommandSource> ctx, String name) {
        SingularityConfig.getGroup(name).ifPresent(group -> {
            Settings settings = group.getSettings();
            ComponentBuilder<TextComponent, TextComponent.Builder> builder = Component.text();
            builder.append(group.getPrettyName());
            builder.append(MiniMessage.miniMessage().deserialize(String.format("\nsingularity.player : %s", settings.syncPlayerData)));
            builder.append(MiniMessage.miniMessage().deserialize(String.format("\nsingularity.stats : %s", settings.syncPlayerStats)));
            builder.append(MiniMessage.miniMessage().deserialize(String.format("\nsingularity.advancements : %s", settings.syncPlayerAdvancements)));
            builder.append(MiniMessage.miniMessage().deserialize(String.format("\nsingularity.ops : %s", settings.syncOps)));
            builder.append(MiniMessage.miniMessage().deserialize(String.format("\nsingularity.whitelist : %s", settings.syncWhitelist)));
            builder.append(MiniMessage.miniMessage().deserialize(String.format("\nsingularity.bans : %s", settings.syncBans)));
            SingularityConfig.getRegisteredBlacklists().forEach(blacklist -> builder.append(MiniMessage.miniMessage().deserialize(String.format("\n%s : %s", blacklist, !settings.nbtBlacklists.contains(blacklist)))));
            ctx.getSource().sendMessage(builder.build());
        });
        return 1;
    }
}