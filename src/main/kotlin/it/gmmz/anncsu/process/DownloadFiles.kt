package it.gmmz.anncsu.process

import klite.info
import klite.logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.outputStream

private const val DATASET_ZIP_URL = "https://anncsu.open.agenziaentrate.gov.it/age-inspire/opendata/anncsu/getds.php?INDIR_ITA"
private const val COMUNI_CSV_URL = "https://www.istat.it/storage/codici-unita-amministrative/Elenco-comuni-italiani.csv"
private const val DATASET_ZIP_NAME = "indirizziautocomplete_ita.zip"
private const val COMUNI_CSV_NAME = "Elenco-comuni-italiani.csv"
private val log = logger("DownloadFiles")

fun downloadFiles(dataDir: Path = Path.of("files")): Pair<Path, Path> {
    log.info("Download process started (dataDir=$dataDir)")
    Files.createDirectories(dataDir)
    val zip = dataDir.resolve(DATASET_ZIP_NAME)
    val comuni = dataDir.resolve(COMUNI_CSV_NAME)
    log.info("Downloading ANNCSU zip -> $zip")
    download(DATASET_ZIP_URL, zip)
    log.info("Downloading comuni csv -> $comuni")
    download(COMUNI_CSV_URL, comuni)
    log.info("Download process completed")
    return zip to comuni
}

private fun download(url: String, target: Path) {
    log.info("HTTP GET $url")
    val response = HttpClient.newHttpClient().send(
        HttpRequest.newBuilder(URI.create(url)).GET().build(),
        HttpResponse.BodyHandlers.ofInputStream()
    )
    log.info("HTTP status ${response.statusCode()} for ${target.fileName}")
    require(response.statusCode() in 200..299) { "Download failed for $url (${response.statusCode()})" }
    response.body().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
    log.info("Saved ${target.fileName} (${Files.size(target)} bytes)")
}