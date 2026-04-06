# Einrichtung und erste Schritte

## Was du brauchst

Für den normalen Einsatz brauchst du:

- Minecraft mit Fabric
- die Mod `WhatABubble`
- für echte Spracherkennung ein Vosk-Sprachmodell
- optional für Übersetzungen einen passenden Übersetzungsdienst

## Schritt 1: Mod installieren

Lege die Mod-Datei in deinen `mods`-Ordner.

Das ist derselbe Ordner, in den man auch andere Fabric-Mods legt.

## Schritt 2: Sprachmodell vorbereiten

Für echte Spracherkennung braucht die Mod ein Sprachmodell.

Ein Sprachmodell ist vereinfacht gesagt eine Sammlung von Sprachwissen.
Sie hilft der Mod dabei, gesprochene Wörter als Text zu erkennen.

Wie du das Modell einrichtest, steht genauer in [`model.md`](model.md).

## Schritt 3: Spiel starten

Starte Minecraft wie gewohnt.

Wichtig:
Die Spracherkennung wird nicht schon im Hauptmenü vollständig vorbereitet.
Sie startet erst, wenn du wirklich eine Welt betrittst oder einem Server beitrittst.

## Schritt 4: Eine Welt betreten

Sobald du in einer Welt bist, beginnt die Mod mit der Vorbereitung.

Im Chat können dabei Hinweise erscheinen, zum Beispiel:

- Welt betreten
- Sprachmodell wird gestartet
- Modell gefunden
- Spracherkennung ist bereit

Wenn kein Modell gefunden wurde, informiert dich die Mod ebenfalls.

## Schritt 5: Einstellungen öffnen

Standardmäßig öffnest du das Einstellungsfenster mit dieser Taste:

```text
B
```

## Was du im Einstellungsfenster findest

Im Fenster gibt es diese wichtigsten Punkte:

- `Mikrofon`
- `Bubbles`
- `Bubble RGB`
- `Text RGB`
- `Zeilenumbruch`
- `Debug`
- `Speichern`
- `Refresh`
- `Abbrechen`

## Die Menüpunkte einfach erklärt

### Mikrofon

Hier wählst du aus, welches Mikrofon genutzt werden soll.

- `Standard (System)` benutzt das Standard-Mikrofon von Windows
- eine Änderung wird erst nach `Speichern` übernommen

### Bubbles

Hier legst du fest, welche Sprechblasen sichtbar sein sollen.

Möglich sind zum Beispiel:

- alle Blasen anzeigen
- alle außer der eigenen anzeigen
- keine Blasen anzeigen

### Bubble RGB

Damit änderst du die Farbe des Blasen-Hintergrunds.

Die Zahlen reichen von `0` bis `255`.
So werden Rot, Grün und Blau gemischt.

### Text RGB

Damit bestimmst du die Farbe des Textes in der Blase.

Auch hier gilt der Bereich `0` bis `255`.

### Zeilenumbruch

Hier bestimmst du, wann ein längerer Satz in die nächste Zeile springt.

So wird die Blase besser lesbar.
Der erlaubte Bereich liegt zwischen `10` und `100`.

### Debug

Diese Funktion zeigt eine Testblase an.
Das ist praktisch, wenn du schnell sehen möchtest, ob Farben und Darstellung stimmen.

### Speichern

Speichert deine Änderungen dauerhaft.
Die Werte werden in die Konfigurationsdatei geschrieben und direkt angewendet.

### Refresh

Lädt die Konfigurationsdatei erneut von der Festplatte.

Das ist nützlich, wenn du die Datei von Hand geändert hast und die neuen Werte übernehmen möchtest.

### Abbrechen

Schließt das Fenster, ohne neue Änderungen zu speichern.

## Wichtige Hinweise für den Alltag

- Deine eigene Sprechblase ist nur in der **Third-Person-Ansicht** sichtbar, also wenn du deine Figur von außen siehst.
- In der Ego-Perspektive siehst du deine eigene Blase nicht.
- Während eine Figur schleicht, werden Sprechblasen versteckt.
- Das Mikrofon wird nur neu geladen, wenn sich die Auswahl wirklich geändert hat.

## Wenn etwas nicht funktioniert

Dann schau in [`troubleshooting.md`](troubleshooting.md).

Dort findest du einfache Lösungen für häufige Probleme.
