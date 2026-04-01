package net.modfest.rolesync.logging;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public record CommandLogger(CommandSourceStack source) implements MiniLogger {
	@Override
	public void error(String error) {
		source.sendFailure(Component.literal(error));
	}

	@Override
	public void warn(String warning) {
		source.sendFailure(Component.literal(warning).withStyle(ChatFormatting.YELLOW));
	}

	@Override
	public void info(String info) {
		source.sendSuccess(() -> Component.literal(info), false);
	}
}
