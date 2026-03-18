# GmmzAnncsu

Microservizio scritto in Kotlin per fornire autocomplete sul dataset di indirizzi pubblicato da ANNCSU (https://anncsu.gov.it/) 

Fornisco 2 immagini docker: Una "lightweight" che esegue solo il server (richiede di inizializzare il DB a mano tramite comando "init") e una "full" che include anche il DB già generato.

Le immagini full vengono generate su cadenza mensile, seguendo approssimativamente il release cycle di ANNCSU. 

## Uso rapido

Clonando la repository:

```bash
./gradlew run --args="init"
./gradlew run --args="serve"
```

Il database viene creato in `files/dati.sqlite`.

## CLI

- `init`: scarica dataset ANNCSU e costruisce il database SQLite su cui il programma opera.
- `serve`: avvia il server su porta `8080`.
- `server`: alias di `serve`.

## Docker

Due immagini pubblicate ad ogni push e ogni mese:

- lightweight: `<project-version>`
- full con DB inizializzato: `<project-version>-YYYYMMDD`

`<project-version>` viene estratta automaticamente da `build.gradle.kts`.

Il `Dockerfile` usa 2 stage:

1. `lightweight`: copia solo l'uber jar e avvia `serve`.
2. `runtime`: copia uber jar + `files/dati.sqlite` e avvia `serve`.

Build del jar e creazione DB avvengono fuori Docker (avevo bisogno di creare un immagine multi-arch, buildare il tutto dentro Docker sotto QEMU mi portava ad avere build di 1h30), poi vengono importati nel container.

Sotto immagine lightweight, consiglio di persistere il file del DB sul disco, per evitare di dover reindexare tutto il DB ogni volta che si vuole avviare il microservizio.
```bash
docker run --rm -v "<tuapath>/files:/app/files" ghcr.io/<owner>/gmmzanncsu:<project-version> init
docker run --rm -v "<tuapath>/files:/app/files" -p 8080:8080 ghcr.io/<owner>/gmmzanncsu:<project-version> serve
```

In questo modo il DB resta salvato su host in `files/dati.sqlite`.

## GitHub Actions

- Workflow mensile pubblica immagine completa con tag `<project-version>-YYYYMMDD`. 
- Workflow su push: pubblica
  - immagine lightweight con tag `<project-version>`,
  - immagine completa con tag `<project-version>-YYYYMMDD`.

## Note

- La prima inizializzazione richiede circa 11 minuti (circa 47MLN di record).
- L'immagine full pesa ~6.8G.
- La ricerca full-text è basata su SQLite con estensione FTS5 + tokenizer trigram. I codici civici vengono estratti a parte e cercati tramite una WHERE sul dato atomico, visto che la ricerca trigram non è ottimale per valori <3 caratteri
