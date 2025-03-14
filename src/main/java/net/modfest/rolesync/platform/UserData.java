package net.modfest.rolesync.platform;

import java.util.List;
import java.util.Set;

/**
 * Pojo-representation of platform data. Only includes relevant data
 */
public record UserData(
	String id,
	String role,
	List<String> minecraft_accounts,
	Set<String> registered
) {
}
