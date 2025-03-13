package net.modfest.rolesync.config;

public record BehaviourConfig(
	String eventId,
	String participantRole,
	String teamMemberRole
) {
}
