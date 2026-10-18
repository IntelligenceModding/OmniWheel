package de.artemis.omniwheel.common.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CommandAction implements WheelAction {

    private final List<String> commands;

    private final boolean requireConfirmation;

    private final boolean preset;

    public CommandAction(List<String> commands, boolean requireConfirmation) {
        this(commands, requireConfirmation, false);
    }

    public CommandAction(String command, boolean requireConfirmation) {
        this(de.artemis.omniwheel.common.Java8.list(command), requireConfirmation);
    }

    @Override
    public boolean requiresConfirmation() {
        return requireConfirmation;
    }

    public CommandAction(List<String> commands, boolean requireConfirmation, boolean preset) {
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
        commands = de.artemis.omniwheel.common.Java8.copyList(normalized);
        this.commands = commands;
        this.requireConfirmation = requireConfirmation;
        this.preset = preset;
    }

    public List<String> commands() {
        return commands;
    }

    public boolean requireConfirmation() {
        return requireConfirmation;
    }

    public boolean preset() {
        return preset;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value)
            return true;
        if (value == null || value.getClass() != getClass())
            return false;
        CommandAction other = (CommandAction) value;
        return true && java.util.Objects.equals(commands, other.commands) && java.util.Objects.equals(requireConfirmation, other.requireConfirmation) && java.util.Objects.equals(preset, other.preset);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(commands, requireConfirmation, preset);
    }

    @Override
    public String toString() {
        return "CommandAction[" + "commands=" + commands + ", requireConfirmation=" + requireConfirmation + ", preset=" + preset + "]";
    }
}
