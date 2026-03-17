# AetherSense

## Requisiti
- Java 21
- Maven
- PostgreSQL
- Linux con systemd

## Configurazione

### 1. Variabili d'ambiente
Crea il file dei segreti sul server:
```bash
mkdir -p /etc/aethersense
nano /etc/aethersense/secrets.env
chmod 600 /etc/aethersense/secrets.env
```

Inserisci nel file:
```
DB_URL=jdbc:postgresql://localhost:5432/nome_db
DB_USER=utente_db
DB_PASS=password_db
API_KEY=genera_con_openssl_rand_hex_32
```

### 2. Prima compilazione
```bash
cd /root/AetherSense
./mvnw package -DskipTests
```

### 3. Servizio systemd
Copia e abilita il servizio:
```bash
cp deploy/aethersense.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable aethersense
systemctl start aethersense
```

L'applicazione si avvia automaticamente 
ad ogni riavvio del server.

### 4. Aggiornamento dopo modifiche al codice
```bash
systemctl stop aethersense
./mvnw package -DskipTests
systemctl start aethersense
```

### 5. Monitoraggio
```bash
systemctl status aethersense
journalctl -u aethersense -f
```

## API Key TTN
Genera la chiave:
```bash
openssl rand -hex 32
```
Aggiungila come header nei webhook TTN:
```
X-API-Key: il_valore_generato
```

## Note di sicurezza
- Il file `secrets.env` non va mai 
  committato su GitHub
- Generare una nuova API Key per ogni 
  ambiente di deployment