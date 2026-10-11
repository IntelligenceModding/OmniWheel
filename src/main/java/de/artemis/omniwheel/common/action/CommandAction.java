package de.artemis.omniwheel.common.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CommandAction implements WheelAction {
    private final List<String> commands;
    private final boolean requireConfirmation;

    public CommandAction(List<String> commands, boolean requireConfirmation) {

        Objects.requireNonNull(commands, "commands");
        List<String> normalized = new ArrayList<>(commands.size());
        for (String command : commands) {
            String trimmed = Objects.requireNonNull(command, "command").trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("commands must not be empty");
        }
        commands = com.google.common.collect.ImmutableList.copyOf(normalized);

        this.commands = commands;
        this.requireConfirmation = requireConfirmation;
    }

    public List<String> commands() {
        return commands;
    }

    public boolean requireConfirmation() {
        return requireConfirmation;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CommandAction)) return false;
        CommandAction that = (CommandAction) other;
        return java.util.Objects.equals(commands, that.commands) && java.util.Objects.equals(requireConfirmation, that.requireConfirmation);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(commands, requireConfirmation);
    }

    @Override
    public String toString() {
        return "CommandAction[" + "commands=" + commands + ", " + "requireConfirmation=" + requireConfirmation + "]";
    }

    public CommandAction(String command, boolean requireConfirmation) {
        this(com.google.common.collect.ImmutableList.of(command), requireConfirmation);
    }

    @Override
    public boolean requiresConfirmation() {
        return requireConfirmation;
    }
}
