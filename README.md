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

- `init`: scarica file sorgente e costruisce indice/FTS.
- `serve`: avvia il server su porta `8080`.
- `server`: alias di `serve`.

## Docker

Tag immagini pubblicate:

- lightweight (push): `<project-version>`
- full con DB inizializzato (push + mensile): `<project-version>-YYYYMMDD`

`<project-version>` viene estratta automaticamente da `build.gradle.kts`.

Il `Dockerfile` usa 3 stage:

1. `lightweight`: copia solo l'uber jar e avvia `serve`.
2. `runtime`: copia uber jar + `files/dati.sqlite` e avvia `serve`.

Build jar e DB avvengono fuori Docker (piu veloce), poi vengono importati nel container.

Build ed esecuzione:

```bash
./gradlew --no-daemon shadowJar
java -jar build/libs/gmmzanncsu-all.jar init
docker build -t gmmzanncsu .
docker run --rm -p 8080:8080 gmmzanncsu
```

Persistenza DB con immagine lightweight (`:<project-version>`):

```bash
docker run --rm -v "<tuapath>/files:/app/files" ghcr.io/<owner>/gmmzanncsu:<project-version> init
docker run --rm -v "<tuapath>/files:/app/files" -p 8080:8080 ghcr.io/<owner>/gmmzanncsu:<project-version> serve
```

In questo modo il DB resta persistito su host in `files/dati.sqlite`.

## GitHub Actions (container)

- Workflow mensile pubblica immagine completa (con `init` gia eseguito) con tag `<project-version>-YYYYMMDD`.
- Workflow su push: pubblica
  - immagine lightweight con tag `<project-version>`,
  - immagine completa con tag `<project-version>-YYYYMMDD`.

## Note

- La prima inizializzazione richiede circa 11 minuti (circa 47MLN di record).
- Ricerca full-text basata su SQLite FTS5 con tokenizer trigram.

