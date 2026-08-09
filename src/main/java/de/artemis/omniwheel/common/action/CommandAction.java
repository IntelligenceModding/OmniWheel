package de.artemis.omniwheel.common.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record CommandAction(List<String> commands, boolean requireConfirmation) implements WheelAction {
    public CommandAction {
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
        commands = List.copyOf(normalized);
    }

    public CommandAction(String command, boolean requireConfirmation) {
        this(List.of(command), requireConfirmation);
    }

    @Override
    public boolean requiresConfirmation() {
        return requireConfirmation;
    }
}
