# Troubleshooting

## I cannot see my own bubble

- switch to **Third Person**
- check the `Bubbles` setting
- check whether you are currently sneaking
- try enabling `Debug`

## Nothing is being recognized

- check the selected microphone
- check whether a Vosk model is in the correct folder
- re-enter the world so the model gets initialized
- wait for the chat message that confirms the model is ready

## The settings only update after refresh

Normally, `Save` should now apply settings immediately.

If something still looks outdated:

- press `Refresh` in the settings menu
- re-enter the world
- check whether you edited the JSON file manually while the game was already running

## Translation is not working

- check `translateReceivedBubbles`
- check whether `translationApiUrl` is correct
- check whether your local LibreTranslate service is actually running
- check the logs if `translationAutoStartLocalService` is enabled
- if your local service just started, wait a little and try again

## My custom texture looks stretched incorrectly

- adjust `sliceBorders`
- adjust `padding`
- check whether your image is suitable for Nine-Slice rendering

## I do not have a Vosk model yet

Without a model, WhatABubble falls back to a dummy speech service.

To enable real speech recognition:

1. download a Vosk model
2. extract it into `config/whatabubble/vosk-model`
3. enter a world again

