# AetherSense

## Configurazione del database

L'applicazione legge i dettagli di connessione al database dalle seguenti variabili d'ambiente:

| Variabile | Descrizione |
|-----------|-------------|
| `DB_URL`  | URL JDBC del database, ad esempio `jdbc:postgresql://localhost:5432/nome_db` |
| `DB_USER` | Nome utente del database |
| `DB_PASS` | Password del database |

### Impostazione delle variabili su una VPS

Per configurare le variabili su una VPS basata su Linux:

1. Accedere al server tramite SSH.
2. Esportare le variabili nella shell corrente (sostituendo i valori di esempio con quelli reali):

   ```bash
   export DB_URL="jdbc:postgresql://<host>:5432/<database>"
   export DB_USER="<utente>"
   export DB_PASS="<password>"
   ```

3. Per renderle persistenti tra i riavvii, aggiungere le stesse righe al file `~/.bashrc` dell'utente oppure definirle in un file di servizio `systemd` utilizzando la direttiva `Environment=`.

Una volta impostate le variabili, avviare l'applicazione con `./mvnw spring-boot:run` o tramite il sistema di gestione dei servizi preferito.

