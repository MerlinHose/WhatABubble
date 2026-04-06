# Bubble Settings and Customization

## Bubble visibility

The `Bubbles` setting defines which speech bubbles are visible:

- `Show all`
- `Show all except my own`
- `Show none`

Important behavior:

- your own bubble is only visible in **Third Person**
- your own bubble is not rendered in First Person
- bubbles are hidden while sneaking

## Bubble colors

You can configure both colors directly in the settings menu.

### Bubble RGB

Changes the bubble background color.

- range: `0-255`

### Text RGB

Changes the text color inside the bubble.

- range: `0-255`

## Line wrapping

The `Line wrapping` setting defines after how many characters the text wraps.

- range: `10-100`

## JSON configuration

The configuration file is stored here:

```text
config/whatabubble.json
```

In a development run, this may be:

```text
run/config/whatabubble.json
```

Example:

```json
{
  "bubbleVisibility": "ALL",
  "maxBubbleLineChars": 25,
  "bubbleRed": 255,
  "bubbleGreen": 255,
  "bubbleBlue": 255,
  "textRed": 0,
  "textGreen": 0,
  "textBlue": 0,
  "bubbleTexture": "",
  "padding": [8, 5, 8, 2],
  "sliceBorders": [20, 5, 20, 5]
}
```

## Custom bubble texture

You can use your own image instead of the built-in bubble texture.

### `bubbleTexture`

Supported values:

- empty value = built-in mod texture
- local file path
- `file:` path
- `http://` or `https://` URL

Examples:

```json
"bubbleTexture": ""
```

```json
"bubbleTexture": "resourcepacks/WhatABubble/assets/whatabubble/textures/gui/bubble_atlas.png"
```

```json
"bubbleTexture": "https://example.com/my-bubble.png"
```

## Nine-Slice settings

WhatABubble always uses Nine-Slice rendering.

### `padding`

Inner content padding:

```json
"padding": [left, top, right, bottom]
```

Example:

```json
"padding": [8, 5, 8, 2]
```

### `sliceBorders`

Nine-slice borders for the image:

```json
"sliceBorders": [left, top, right, bottom]
```

Example:

```json
"sliceBorders": [20, 5, 20, 5]
```

This lets you define—similar to Unity—which image borders should not stretch and which center area should scale.

## After making changes

If you changed the JSON file or the bubble texture:

- press `Refresh` in the settings menu
- or re-enter the world

