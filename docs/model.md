# Vosk Model Setup

## Default model path

By default, WhatABubble looks for a model here:

```text
config/whatabubble/vosk-model
```

It also supports language-specific folders:

```text
config/whatabubble/vosk-model-de
config/whatabubble/vosk-model-en
```

## Download a model

You can find Vosk models here:

```text
https://alphacephei.com/vosk/models
```

After downloading, copy the extracted model into the `config/whatabubble/vosk-model` folder.

## Important behavior

The mod only loads the speech model once you are actually inside a world or on a server.

That means:

- launching the game alone is not enough
- enter a world or join a server
- wait for the in-game chat messages that confirm the model state

## Built-in speech hints

The default hint list already contains common Minecraft terms and localized variants, for example:

- Creeper
- Zombie
- Skeleton
- Enderman
- Villager
- Redstone
- Nether
- Netherite
- Diamond
- Obsidian
- Piglin
- Blaze
- Ghast
- Wither
- Ender Dragon

## Add your own hints

You can add extra terms in the JSON config:

```json
"additionalVoskHints": ["Warden", "Elytra", "Shulker"]
```

This is useful for:

- server-specific words
- custom item names
- mob names you use often
- words Vosk does not recognize reliably yet

