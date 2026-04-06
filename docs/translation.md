# Translation

## What translation does

When enabled, your client can translate received bubbles from other players into your own language.

The transmitted bubble includes:

- the speaker's source language
- the original message text

Your client translates the received text locally into your selected language.

The debug bubble can also be translated.

## JSON options

```json
{
  "translateReceivedBubbles": true,
  "translationApiUrl": "",
  "translationApiKey": "",
  "translationLocalDir": "whatabubble/libretranslate",
  "translationLocalStartScript": "start.bat",
  "translationAutoStartLocalService": true
}
```

## Meaning of the options

- `translateReceivedBubbles`: enables or disables translation
- `translationApiUrl`: external or local LibreTranslate endpoint
- `translationApiKey`: API key, if your service requires one
- `translationLocalDir`: local folder for your own translation starter
- `translationLocalStartScript`: startup file, for example `start.bat`
- `translationAutoStartLocalService`: starts the local translator automatically when needed

If `translationApiUrl` is empty, this endpoint is used by default:

```text
http://127.0.0.1:5000/translate
```

## Local LibreTranslate folder

Default folder:

```text
config/whatabubble/libretranslate
```

In a development run, this is often:

```text
run/config/whatabubble/libretranslate
```

You can place your own starter there, for example:

- `start.bat`
- `start.cmd`
- `start.ps1`
- `libretranslate.exe`

The started service must provide a LibreTranslate-compatible endpoint:

```text
http://127.0.0.1:5000/translate
```

## Example: local auto-start

```json
{
  "translateReceivedBubbles": true,
  "translationApiUrl": "",
  "translationLocalDir": "whatabubble/libretranslate",
  "translationLocalStartScript": "start.bat",
  "translationAutoStartLocalService": true
}
```

## Notes

- translation only affects received bubbles, not the original sender's local bubble
- if your local service is slow to start, the first translations may fail until the service is ready
- if your service requires a key, set `translationApiKey`

