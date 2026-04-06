# Sprechblasen anpassen

## Sichtbarkeit der Sprechblasen

Mit der Einstellung `Bubbles` legst du fest, welche Sprechblasen angezeigt werden.

Je nach Auswahl kannst du:

- alle Blasen sehen
- alle außer deiner eigenen sehen
- keine Blasen sehen

## Wichtige Regeln

Es gibt dabei einige feste Verhaltensweisen:

- Deine eigene Sprechblase ist nur in der **Third-Person-Ansicht** sichtbar, also wenn du deine Figur von außen siehst.
- In der Ego-Perspektive wird deine eigene Blase nicht angezeigt.
- Wenn eine Figur schleicht, werden Sprechblasen versteckt.

## Farben der Blase

Die Farben kannst du direkt im Einstellungsfenster verändern.

### Bubble RGB

Dieser Wert steuert die Hintergrundfarbe der Sprechblase.

### Text RGB

Dieser Wert steuert die Farbe des Textes in der Blase.

### Was bedeutet RGB?

`RGB` steht für Rot, Grün und Blau.
Aus diesen drei Farbanteilen wird die endgültige Farbe gemischt.

Jeder Wert liegt zwischen:

```text
0 bis 255
```

## Zeilenumbruch

Mit `Zeilenumbruch` legst du fest, wann ein längerer Text in die nächste Zeile springt.

Das macht die Blase besser lesbar.

Erlaubter Bereich:

```text
10 bis 100
```

## Wo werden die Einstellungen gespeichert?

Die Konfiguration liegt normalerweise hier:

```text
config/whatabubble.json
```

In einer Entwicklungsumgebung kann sie auch hier liegen:

```text
run/config/whatabubble.json
```

## Beispiel für die Konfigurationsdatei

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

## Eigene Blasen-Grafik verwenden

Wenn dir die eingebaute Grafik nicht gefällt, kannst du eine eigene Bilddatei verwenden.

Das geht über den Wert `bubbleTexture`.

### Mögliche Inhalte für `bubbleTexture`

- leer lassen = Standardgrafik der Mod verwenden
- lokaler Dateipfad
- Pfad mit `file:`
- Internetadresse mit `http://` oder `https://`

### Beispiele

Standardgrafik:

```json
{
  "bubbleTexture": ""
}
```

Lokale Datei:

```json
{
  "bubbleTexture": "resourcepacks/WhatABubble/assets/whatabubble/textures/gui/bubble_atlas.png"
}
```

Datei aus dem Internet:

```json
{
  "bubbleTexture": "https://example.com/my-bubble.png"
}
```

## Abstand und Ränder der Blase

Die Mod verwendet immer eine Darstellungsart namens `Nine-Slice`.

Das klingt technisch, ist aber leicht erklärt:
Man kann sich die Blase wie einen Bilderrahmen vorstellen.
Die Ecken bleiben schön erhalten, während die Mitte mitwachsen darf.

So wird die Blase größer, ohne dass die Ecken hässlich verzerrt werden.

### `padding`

Dieser Wert bestimmt den inneren Abstand zwischen Text und Blasenrand.

Aufbau:

```text
padding = [links, oben, rechts, unten]
```

Beispiel:

```json
{
  "padding": [8, 5, 8, 2]
}
```

### `sliceBorders`

Dieser Wert bestimmt, welche Teile der Grafik fest bleiben und welche gedehnt werden dürfen.

Aufbau:

```text
sliceBorders = [links, oben, rechts, unten]
```

Beispiel:

```json
{
  "sliceBorders": [20, 5, 20, 5]
}
```

## Nach Änderungen

Wenn du die Konfigurationsdatei oder die Blasen-Grafik geändert hast, gibt es zwei einfache Wege:

- im Einstellungsfenster `Refresh` drücken
- die Welt erneut betreten

## Tipp aus dem Alltag

Wenn deine eigene Grafik seltsam gedehnt aussieht, liegt das oft nicht am Bild selbst, sondern an `padding` oder `sliceBorders`.

Schon kleine Änderungen an diesen Werten können die Darstellung deutlich verbessern.
