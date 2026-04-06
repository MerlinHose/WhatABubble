# Setup

## Requirements

- Minecraft with Fabric
- the `WhatABubble` mod
- for real speech recognition: a Vosk model
- optional for translations: a LibreTranslate-compatible service

## Install the mod

Place the mod in your `mods` folder as usual.

## First launch

WhatABubble only initializes parts of the speech recognition system after you are inside a world or on a server.

When you enter a world, you may see chat messages such as:

- `Entered world. Preparing speech recognition...`
- `Starting speech model...`
- `Model found: vosk-model`
- `Vosk model loaded. Speech recognition is ready.`

If no model is found, you will also receive a message in-game.

## Open the settings menu

Default key:

```text
B
```

The menu contains:

- `Microphone`
- `Bubbles`
- `Bubble RGB`
- `Text RGB`
- `Line wrapping`
- `Debug`
- `Save`
- `Refresh`
- `Cancel`

## What the menu options do

### Microphone

Selects the input device for speech recognition.

- `Default (System)` uses the Windows default recording device
- a device change only becomes active after pressing `Save`

### Bubbles

Defines which speech bubbles are visible:

- `Show all`
- `Show all except my own`
- `Show none`

### Bubble RGB

Changes the bubble background color.

- range: `0-255`

### Text RGB

Changes the bubble text color.

- range: `0-255`

### Line wrapping

Defines after how many characters the text wraps.

- range: `10-100`

### Debug

Shows a test bubble so you can quickly verify rendering, colors, and texture setup.

## Notes

- `Save` writes the values to the JSON file and applies them immediately
- `Refresh` reloads the JSON file from disk
- the microphone is only reloaded if it was actually changed
- your own bubble is not rendered in First Person
- bubbles are not shown while a player is sneaking

