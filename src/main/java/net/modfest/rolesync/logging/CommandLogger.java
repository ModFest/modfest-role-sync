package net.modfest.rolesync.logging;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public record CommandLogger(ServerCommandSource source) implements MiniLogger {
	@Override
	public void error(String error) {
		source.sendError(Text.literal(error));
	}

	@Override
	public void warn(String warning) {
		source.sendError(Text.literal(warning).formatted(Formatting.YELLOW));
	}

	@Override
	public void info(String info) {
		source.sendFeedback(() -> Text.literal(info), false);
	}
}
