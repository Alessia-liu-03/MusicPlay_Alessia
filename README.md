# Simple Background Player (ESP 2021)

## Descrizione
Applicazione Android per la riproduzione musicale in background con supporto tablet.

## Funzionalità principali
* **Player Service**: Gestisce la riproduzione continua anche a schermo spento.
* **Database Locale**: Memorizza i percorsi dei brani e le copertine.
* **UI Adattiva**: Layout ottimizzato per Smartphone e Tablet (Fragment).
* **Rotazione Disco**: Animazione sincronizzata con lo stato di riproduzione.

## Come usare
1. Aggiungi i brani cliccando sul tasto aggiungi.
2. Il sistema estrarrà metadati e copertine salvandoli internamente nel database.
3. Clicca su un brano per avviare il Player.
4. La modalità risparmia batteria puo' creare problemi nel produrre la musica in background.
5. Per eliminare il brano, nel song list trascinarlo verso sx, e poi premere sul pulsante delete(cestino rosso). Per chiudere la finestra di delete, scorri la lista verso su' o giu'.
6. Per passare al brano successivo/precedente, clicca su "Successivo" o "Precedente". Dopo aver cliccato i pulsanti puo' succedere che la seekbar e il time non viene aggiornato, basta cliccare su un pulsante qualsiasi per farlo aggiornare.
7. Clicca sul pulsante stop/play per stoppare la musica o per farlo riprodurre.
8. Il pulsante a sx del pulsante "Precedente" serve per modificare i modi: sequenziale, repeat, random. Produce i prossimi brani secondo la modalità scelta.
9. Se clicchi su "Successivo" quando hai il mode "repeat", suona la prossima canzone. Lo stesso vale per "Precedente".
10. Per modificare il nome del brano o il nome dell'artista, clicca su icona a dx del "Successivo".
11. Per impostare un timer alla canzone, clicca su "Timer". Per disattivarlo, clicca su "disattiva".

## Dispositivi di Test

### 1. Tablet (Dispositivo principale)
* **Modello:** Samsung Galaxy Tab A8 (Fisico)
* **Versione Android:** Android 14 (API 34)
* **Note:** Testato le funzioni.

### 2. Smartphone (Dispositivo secondario)
* **Modello:** Poco X4 Pro (Fisico)
* **Versione Android:** Android 13 (API 33)
* **Note:** Verificato il corretto funzionamento del tasto "Back" e della navigazione tra fragment.

### 3. Smartphone
* **Modello:** iQOO Z7 (Fisico)
* **Versione Android:** Android 15 (API 35)
* **Note:** Modificato il codice per farlo adattare a diverse grandezze dello schermo (smartphone).

## 3. Smartphone
* **Modello:** iQOO Z7 (Fisico)
* **Versione Android:** Android 14 (API 34)
* **Note:** Aggiunto una funzione per chiedere i permessi dei file music in modo persistente.