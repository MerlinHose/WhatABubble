# WhatABubble – einfache Projektdokumentation

## Einführung

`WhatABubble` ist eine Minecraft-Mod für Fabric.
Die Mod hört gesprochene Sprache über dein Mikrofon und zeigt daraus Sprechblasen im Spiel an.

Man kann sich das wie Untertitel über dem Kopf einer Figur vorstellen.
Wenn jemand etwas sagt, erscheint der gesprochene Inhalt als lesbarer Text direkt über der Person.

Das hilft besonders dann, wenn:

- Stimmen schwer zu verstehen sind
- mehrere Personen gleichzeitig reden
- jemand ohne Ton mitlesen möchte
- verschiedene Sprachen auf einem Server vorkommen

Zusätzlich kann die Mod empfangene Sprechblasen auf Wunsch in deine Sprache übersetzen.

> **Wichtiger Hinweis:** Das Projekt ist noch in Entwicklung. Einige Funktionen können sich also noch ändern.

## Ziel des Projekts

Das Ziel von `WhatABubble` ist, Gespräche in Minecraft leichter verständlich zu machen.

Statt Sprache nur zu hören, wird sie auch sichtbar.
So wird Kommunikation im Spiel klarer, zugänglicher und oft auch lustiger.

Einfach gesagt möchte die Mod drei Dinge erreichen:

1. Gesprochenes im Spiel sichtbar machen
2. Gespräche auch ohne perfektes Hören besser verständlich machen
3. Sprachbarrieren mit einer optionalen Übersetzung verringern

## Funktionsweise – Schritt für Schritt einfach erklärt

So arbeitet die Mod im normalen Spielbetrieb:

### 1. Du installierst die Mod

Die Mod kommt wie andere Fabric-Mods in den `mods`-Ordner.

### 2. Du startest Minecraft und betrittst eine Welt

Wichtig: Die Spracherkennung wird erst vorbereitet, wenn du wirklich in einer Welt oder auf einem Server bist.
Nur das Hauptmenü zu öffnen reicht noch nicht aus.

### 3. Die Mod sucht nach einem Sprachmodell

Ein **Sprachmodell** ist vereinfacht gesagt ein Sprachpaket, das der Mod hilft, gesprochene Wörter zu erkennen.

Wenn ein passendes Modell gefunden wird, kann echte Spracherkennung starten.
Wenn kein Modell vorhanden ist, läuft die Mod in einem Ersatzmodus weiter.
Dann gibt es aber keine echte Sprache-zu-Text-Erkennung.

### 4. Du sprichst in dein Mikrofon

Die Mod hört über das ausgewählte Mikrofon zu und versucht, deine gesprochenen Wörter in Text umzuwandeln.

### 5. Aus deiner Sprache wird Text

Erkannte Sätze werden automatisch in passende Zeilen aufgeteilt, damit sie in die Sprechblase passen.

### 6. Die Sprechblase erscheint im Spiel

Der Text wird als Sprechblase über der Figur angezeigt.

Wichtige Besonderheiten dabei:

- Deine eigene Sprechblase siehst du nur in der **Third-Person-Ansicht**, also in einer Kamerasicht von außen auf deine Figur.
- Wenn eine Figur schleicht, werden Sprechblasen versteckt.
- Andere Spieler in der Nähe können die Sprechblasen ebenfalls erhalten.

### 7. Optional: Übersetzung

Wenn du die Übersetzung einschaltest, kann dein Spiel empfangene Sprechblasen in deine Sprache umwandeln.

Das ist so, als würde jemand etwas sagen und du siehst direkt eine verständliche Übersetzung darüber.

## Wichtige Bestandteile

Hier sind die wichtigsten Teile des Projekts – bewusst ohne unnötigen Fachjargon erklärt.

### Die Mod selbst

Das ist das eigentliche Zusatzprogramm für Minecraft.
Es verbindet alle anderen Teile miteinander.

### Das Mikrofon

Über das Mikrofon bekommt die Mod die gesprochene Sprache.
Im Einstellungsfenster kannst du auswählen, welches Mikrofon verwendet werden soll.

### Das Sprachmodell

Das Sprachmodell hilft der Mod beim Verstehen gesprochener Wörter.
Ohne dieses Modell kann die Mod zwar starten, aber keine echte Sprache erkennen.

In diesem Projekt wird dafür `Vosk` verwendet.
Das ist ein Werkzeug zur Spracherkennung, das lokal auf dem eigenen Rechner arbeiten kann.

### Die Sprechblasen

Die Sprechblasen sind die sichtbaren Textfelder über den Figuren.
Du kannst zum Beispiel Farbe, Textfarbe und Zeilenlänge anpassen.

### Die Übersetzung

Auf Wunsch kann die Mod empfangene Sprechblasen übersetzen.
Dafür wird ein Dienst verwendet, der mit `LibreTranslate` zusammenarbeitet.

Einfach gesagt: Die Mod schickt den Text an einen Übersetzer und zeigt danach die übersetzte Version an.

### Die Konfigurationsdatei

Viele Einstellungen werden in einer Datei gespeichert.
Dadurch bleiben deine Einstellungen auch nach dem Neustart erhalten.

Die Datei liegt normalerweise hier:

```text
config/whatabubble.json
```

## Nutzung / Anleitung

### Schnellstart

Wenn du einfach nur loslegen möchtest, gehe so vor:

1. Installiere Fabric und die Mod.
2. Lege die Mod-Datei in deinen `mods`-Ordner.
3. Lege ein Vosk-Sprachmodell in den passenden Ordner.
4. Starte Minecraft.
5. Betritt eine Welt oder einen Server.
6. Öffne das Einstellungsfenster mit der Taste `B`.
7. Wähle dein Mikrofon aus.
8. Passe die Darstellung der Sprechblasen an.

### Einstellungen im Spiel

Mit der Taste `B` öffnest du das Einstellungsfenster.

Dort findest du vor allem diese Punkte:

- **Mikrofon** – welches Eingabegerät benutzt wird
- **Bubbles** – welche Sprechblasen sichtbar sein sollen
- **Bubble RGB** – Farbe der Blase
- **Text RGB** – Farbe des Textes
- **Zeilenumbruch** – wie lang eine Textzeile sein darf
- **Debug** – zeigt eine Testblase zum Prüfen an

### Was bedeuten diese Punkte?

#### Mikrofon

Hier wählst du das Aufnahmegerät aus.
`Standard (System)` nutzt das Mikrofon, das in Windows als Standard festgelegt ist.

#### Bubbles

Hier legst du fest, welche Blasen du sehen möchtest:

- alle
- alle außer deine eigene
- keine

#### Bubble RGB

Damit stellst du die Farbe des Hintergrunds ein.
`RGB` bedeutet vereinfacht: Rot, Grün und Blau mischen zusammen eine Farbe.

#### Text RGB

Damit legst du die Farbe des Textes in der Blase fest.

#### Zeilenumbruch

Dieser Wert bestimmt, wann ein langer Satz in die nächste Zeile springt.
So bleibt die Blase lesbar und wird nicht zu breit.

#### Debug

Das ist eine Testfunktion.
Sie zeigt eine Beispiel-Sprechblase an, damit du schnell prüfen kannst, ob die Darstellung funktioniert.

## Weitere Detailseiten

Wenn du noch tiefer einsteigen möchtest, helfen diese Seiten weiter:

- [`setup.md`](setup.md) – Installation und erste Schritte
- [`model.md`](model.md) – Sprachmodell und Wort-Hilfen
- [`bubbles.md`](bubbles.md) – Darstellung, Farben und eigene Blasen-Grafiken
- [`translation.md`](translation.md) – Übersetzung von empfangenen Sprechblasen
- [`troubleshooting.md`](troubleshooting.md) – Hilfe bei Problemen

## Beispiele

Hier sind einige typische Alltagssituationen.

### Beispiel 1: Gemeinsam im Abenteuer

Eine Person sagt: „Achtung, hinter dir ist ein Creeper!“

Mit `WhatABubble` erscheint dieser Satz als Sprechblase über der Figur.
Auch wenn du das Gesagte akustisch nicht gut verstanden hast, kannst du es sofort lesen.

### Beispiel 2: Schlechte Mikrofonqualität

Jemand spricht sehr leise oder das Mikrofon rauscht.
Der Ton ist schwer verständlich.

Die Sprechblase hilft, weil du die Worte zusätzlich als Text siehst.

### Beispiel 3: Mehrsprachiger Server

Eine Person spricht Englisch, du möchtest aber Deutsch lesen.
Mit aktivierter Übersetzung kann dein Spiel die empfangene Sprechblase in deine Sprache umwandeln.

### Beispiel 4: Test ohne lange Suche

Du bist unsicher, ob die Blasen überhaupt angezeigt werden.
Dann schaltest du **Debug** ein.
So erscheint eine Testblase, mit der du Farben und Darstellung direkt prüfen kannst.

## Vorteile und Nutzen

`WhatABubble` kann in vielen Situationen hilfreich sein:

- Gespräche werden leichter verständlich
- Gesprochenes ist zusätzlich sichtbar
- Kommunikation wirkt lebendiger und direkter
- Missverständnisse werden seltener
- verschiedene Sprachen lassen sich besser überbrücken
- die Darstellung lässt sich an den eigenen Geschmack anpassen

Ein Vergleich aus dem Alltag:
Wenn man in einem Film etwas akustisch verpasst, helfen Untertitel.
`WhatABubble` erfüllt im Spiel eine ähnliche Rolle.

## Häufige Fragen (FAQ)

### Was brauche ich mindestens?

Du brauchst Minecraft mit Fabric, die Mod selbst und für echte Spracherkennung ein passendes Vosk-Sprachmodell.

### Warum passiert nach dem Spielstart noch nichts?

Die Mod bereitet die Spracherkennung erst vor, wenn du wirklich eine Welt betrittst.

### Warum sehe ich meine eigene Sprechblase nicht?

Deine eigene Blase wird nur in der Third-Person-Ansicht angezeigt, also wenn du deine Figur von außen siehst.
In der Ego-Perspektive ist sie nicht sichtbar.

### Warum sehe ich gar keine Sprechblasen?

Mögliche Gründe sind:

- ein falsches Mikrofon
- kein Sprachmodell vorhanden
- die Sichtbarkeit ist ausgeschaltet
- du oder die andere Figur schleicht gerade

### Funktioniert die Mod ohne Internet?

Die Spracherkennung mit Vosk ist grundsätzlich lokal gedacht.
Für die optionale Übersetzung kann aber ein Übersetzungsdienst nötig sein.

### Werden auch entfernte Spieler erreicht?

Nach aktuellem Projektstand werden Sprechblasen an Spielerinnen und Spieler in der Nähe weitergegeben.
Im Code ist dabei ein Bereich von ungefähr 32 Blöcken vorgesehen.

### Kann ich eigene Wörter verbessern?

Ja. In der Konfigurationsdatei kannst du zusätzliche Begriffe hinterlegen, damit bestimmte Wörter besser erkannt werden.

### Kann ich das Aussehen ändern?

Ja. Farben, Zeilenlänge und sogar eine eigene Blasen-Grafik lassen sich anpassen.

## Annahmen und Grenzen

Einige Punkte lassen sich aus dem aktuellen Projektstand ableiten, sind aber noch nicht als feste Endfassung zu verstehen:

- Das Projekt ist noch nicht abgeschlossen.
- Das Einstellungsfenster konzentriert sich momentan vor allem auf Mikrofon, Darstellung und Testfunktionen.
- Erweiterte Funktionen wie Übersetzung oder tiefergehende Anpassungen laufen teilweise über die Konfigurationsdatei.

Wenn sich das Projekt weiterentwickelt, können sich einzelne Menüs oder Abläufe noch ändern.
