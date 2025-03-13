package net.modfest.rolesync.logging;

import org.slf4j.Logger;

public record Slf4jLogger(Logger l) implements MiniLogger {
	@Override
	public void error(String error) {
		l.error(error);
	}

	@Override
	public void warn(String warning) {
		l.warn(warning);
	}

	@Override
	public void info(String info) {
		l.info(info);
	}
}
