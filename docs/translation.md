# Übersetzung von Sprechblasen

## Was macht die Übersetzung?

Wenn die Übersetzung eingeschaltet ist, kann dein Spiel empfangene Sprechblasen anderer Personen in deine Sprache umwandeln.

Ein einfaches Beispiel:

- jemand spricht Englisch
- du spielst auf Deutsch
- dein Spiel zeigt dir die empfangene Blase auf Deutsch an

Die ursprüngliche Nachricht enthält dabei unter anderem:

- die Sprache des Sprechers
- den originalen Text

Danach übersetzt dein Spiel die Nachricht in deine eingestellte Sprache.

Auch die Testblase aus dem Debug-Modus kann übersetzt werden.

## Wichtiger Hinweis

Die Übersetzung betrifft nur empfangene Sprechblasen.
Die eigene lokale Sprechblase der sprechenden Person bleibt davon unberührt.

## Einstellungen in der Konfigurationsdatei

Beispiel:

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

## Die Werte einfach erklärt

### `translateReceivedBubbles`

Schaltet die Übersetzung ein oder aus.

### `translationApiUrl`

Hier steht die Adresse des Übersetzungsdienstes.
Das kann ein lokaler Dienst auf deinem Rechner oder ein externer Dienst sein.

### `translationApiKey`

Manche Dienste verlangen einen Schlüssel zur Nutzung.
Falls dein Dienst so etwas braucht, kommt dieser Wert hier hinein.

### `translationLocalDir`

Das ist der Ordner, in dem deine lokale Übersetzungslösung liegt.

### `translationLocalStartScript`

Das ist die Datei, mit der der lokale Übersetzungsdienst gestartet wird.

Beispiele:

- `start.bat`
- `start.cmd`
- `start.ps1`
- `libretranslate.exe`

### `translationAutoStartLocalService`

Wenn dieser Wert aktiv ist, versucht die Mod den lokalen Übersetzungsdienst bei Bedarf automatisch zu starten.

## Standard-Adresse

Wenn `translationApiUrl` leer bleibt, wird standardmäßig diese Adresse verwendet:

```text
http://127.0.0.1:5000/translate
```

Das ist eine lokale Adresse auf deinem eigenen Rechner.

## Standardordner für einen lokalen Übersetzer

Normalerweise wird dieser Ordner verwendet:

```text
config/whatabubble/libretranslate
```

In einer Entwicklungsumgebung ist es oft dieser Pfad:

```text
run/config/whatabubble/libretranslate
```

## Beispiel: lokaler Autostart

```json
{
  "translateReceivedBubbles": true,
  "translationApiUrl": "",
  "translationLocalDir": "whatabubble/libretranslate",
  "translationLocalStartScript": "start.bat",
  "translationAutoStartLocalService": true
}
```

## Was sollte der lokale Dienst können?

Der gestartete Dienst muss mit `LibreTranslate` zusammenarbeiten können.

Einfach gesagt:
Er muss Anfragen annehmen können, die wie bei LibreTranslate aufgebaut sind.

Die Mod erwartet dafür standardmäßig diese Adresse:

```text
http://127.0.0.1:5000/translate
```

## Typische Stolperstellen

- Wenn der lokale Dienst langsam startet, kann die erste Übersetzung fehlschlagen.
- Wenn die Adresse falsch ist, kommt keine Übersetzung zurück.
- Wenn ein Schlüssel nötig ist, muss `translationApiKey` gesetzt sein.

## Alltagserklärung

Man kann sich diese Funktion wie eine kleine Dolmetscherin im Hintergrund vorstellen:
Jemand sagt etwas, die Mod hört nicht neu zu, sondern nimmt den schon erkannten Text und lässt ihn zusätzlich übersetzen.
