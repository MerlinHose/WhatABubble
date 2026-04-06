# Sprachmodell einrichten

## Was ist ein Sprachmodell?

Ein Sprachmodell ist vereinfacht gesagt ein Sprachpaket.
Es hilft der Mod dabei, deine gesprochenen Wörter in geschriebenen Text umzuwandeln.

Ohne ein solches Modell kann `WhatABubble` zwar starten, aber keine echte Spracherkennung durchführen.

## Wo sucht die Mod nach dem Modell?

Standardmäßig wird hier gesucht:

```text
config/whatabubble/vosk-model
```

Zusätzlich unterstützt das Projekt auch sprachbezogene Ordner, zum Beispiel:

```text
config/whatabubble/vosk-model-de
config/whatabubble/vosk-model-en
```

Das bedeutet:

- `de` steht zum Beispiel für Deutsch
- `en` steht zum Beispiel für Englisch

## Wo bekommt man ein Modell?

Die Vosk-Modelle findest du hier:

```text
https://alphacephei.com/vosk/models
```

## So richtest du das Modell ein

1. Lade ein passendes Modell herunter.
2. Entpacke die heruntergeladene Datei.
3. Kopiere den entpackten Modellordner in diesen Pfad:

```text
config/whatabubble/vosk-model
```

Danach kann die Mod versuchen, das Modell beim Betreten einer Welt zu laden.

## Wichtiger Hinweis

Die Mod lädt das Sprachmodell erst dann, wenn du wirklich in einer Welt oder auf einem Server bist.

Das heißt konkret:

- nur das Spiel zu starten reicht nicht
- betrete eine Welt oder einen Server
- warte auf die Hinweise im Spielchat

## Was passiert, wenn kein Modell gefunden wird?

Dann wechselt die Mod in einen Ersatzmodus.

Das ist hilfreich, damit das Spiel nicht sofort scheitert.
Aber echte Sprache-zu-Text-Erkennung funktioniert dann nicht.

## Eingebaute Wort-Hilfen

Damit typische Minecraft-Begriffe besser erkannt werden, bringt das Projekt bereits einige hilfreiche Wörter mit.

Dazu gehören unter anderem:

- Creeper
- Zombie
- Skelett
- Enderman
- Dorfbewohner
- Redstone
- Nether
- Netherite
- Diamant
- Obsidian
- Piglin
- Blaze
- Ghast
- Wither
- Enderdrache

Diese Liste hilft der Erkennung besonders bei typischen Spielbegriffen.

## Eigene Wörter hinzufügen

Du kannst selbst weitere Begriffe ergänzen.
Das ist nützlich, wenn auf deinem Server spezielle Namen, Orte oder Gegenstände oft vorkommen.

Beispiel in der Konfigurationsdatei:

```json
{
  "additionalVoskHints": ["Warden", "Elytra", "Shulker"]
}
```

Das kann helfen bei:

- besonderen Server-Begriffen
- eigenen Item-Namen
- Namen von Kreaturen
- Wörtern, die bisher nicht zuverlässig erkannt werden

## Alltagstipp

Wenn ein Wort immer wieder falsch erkannt wird, lohnt es sich oft, dieses Wort in die Zusatzliste einzutragen.

Das ist ein bisschen so, als würde man einem Sprachassistenten vorher sagen, auf welche Wörter er besonders achten soll.
