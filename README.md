<p align="center">
  <strong>OmniWheel</strong>
</p>

<p align="center">
  A client-side radial quick-action menu for Minecraft Java Edition on NeoForge 1.21.1.
</p>

## What It Does

OmniWheel lets the player build radial menus with:

- command entries
- chat entries
- gameplay function entries
- nested submenus
- per-entry icons, colors, shortcuts, and render toggles
- multiple profiles with profile switching

The mod is client-side. It does not require installation on the server.

## Controls

Default controls:

- `R` opens the radial menu
- `M` opens the manager

The radial also supports:

- directional mouse selection
- numpad position bindings for the current wheel layout
- optional per-entry shortcuts

## Manager

The manager is used to:

- create and delete profiles
- add command, chat, function, and submenu entries
- rename and recolor entries
- choose icons
- assign shortcuts
- reorder entries and profiles

Changes save automatically.

## Entry Types

- `Command`: runs one or more commands in order
- `Chat`: sends a chat message
- `Function`: triggers client-side gameplay or OmniWheel actions
- `Submenu`: opens another wheel

## Project Structure

- `src/main/java/de/artemis/omniwheel/client`
  overlays, screens, rendering, input, runtime, tutorials, profiles
- `src/main/java/de/artemis/omniwheel/common`
  shared actions, config, profile, and wheel data
- `src/main/resources/assets/omniwheel`
  language assets
- `src/main/templates/META-INF/neoforge.mods.toml`
  mod metadata template

## Development

Current baseline:

- Minecraft `1.21.1`
- NeoForge `21.1.244`
- Java `21`

Useful Gradle tasks:

- `runClient`
- `runServer`
- `compileJava`
- `build`

## License

OmniWheel is released under the MIT License. See [LICENSE](LICENSE).
