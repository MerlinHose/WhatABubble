# Hilfe bei Problemen

## Ich sehe meine eigene Sprechblase nicht

Prüfe bitte diese Punkte:

- Wechsle in die **Third-Person-Ansicht**, also in eine Sicht von außen auf deine Figur.
- Prüfe die Einstellung `Bubbles`.
- Achte darauf, ob deine Figur gerade schleicht.
- Aktiviere testweise `Debug`, um eine Testblase anzuzeigen.

## Es wird gar nichts erkannt

Dann sind oft diese Punkte die Ursache:

- Das falsche Mikrofon ist ausgewählt.
- Es liegt kein Vosk-Sprachmodell im richtigen Ordner.
- Du hast noch keine Welt betreten.
- Das Modell wurde noch nicht vollständig geladen.

Hilfreiche Schritte:

1. Prüfe dein ausgewähltes Mikrofon.
2. Prüfe, ob ein Modell in `config/whatabubble/vosk-model` liegt.
3. Betritt die Welt erneut.
4. Warte auf die Chat-Hinweise, dass die Spracherkennung bereit ist.

## Meine Änderungen erscheinen erst nach `Refresh`

Normalerweise sollte `Speichern` die Einstellungen direkt übernehmen.

Wenn etwas trotzdem alt aussieht:

- drücke `Refresh`
- betrete die Welt erneut
- prüfe, ob du die JSON-Datei während des laufenden Spiels von Hand verändert hast

## Die Übersetzung funktioniert nicht

Prüfe diese Punkte:

- Ist `translateReceivedBubbles` eingeschaltet?
- Ist `translationApiUrl` korrekt?
- Läuft dein lokaler Übersetzungsdienst wirklich?
- Falls der automatische Start aktiv ist: Gibt es Hinweise in den Logs?
- Wenn der Dienst gerade erst gestartet wurde: Kurz warten und erneut versuchen.

## Meine eigene Blasen-Grafik sieht verzerrt aus

Dann helfen oft diese Schritte:

- `sliceBorders` anpassen
- `padding` anpassen
- prüfen, ob die Grafik gut für diese Art der Skalierung geeignet ist

Einfach erklärt:
Wenn Ecken oder Ränder falsch festgelegt sind, wird das Bild beim Vergrößern unschön gedehnt.

## Ich habe noch kein Sprachmodell

Ohne Sprachmodell nutzt die Mod einen Ersatzdienst.
Damit läuft das Projekt zwar weiter, aber echte Spracherkennung ist dann nicht möglich.

So aktivierst du die echte Erkennung:

1. Lade ein Vosk-Modell herunter.
2. Entpacke es nach `config/whatabubble/vosk-model`.
3. Betritt die Welt erneut.

## Ich bin unsicher, ob die Anzeige grundsätzlich funktioniert

Dann aktiviere `Debug` im Einstellungsfenster.

Damit zeigt die Mod eine Testblase an.
So kannst du schnell prüfen, ob das Anzeigen der Blasen grundsätzlich klappt.

## Die Mod startet, aber es wirkt noch nicht fertig

Das passt zum aktuellen Projektstand.
`WhatABubble` ist noch in Entwicklung.

Wenn also einzelne Funktionen noch unfertig wirken oder sich später ändern, ist das im Moment erwartbar.
