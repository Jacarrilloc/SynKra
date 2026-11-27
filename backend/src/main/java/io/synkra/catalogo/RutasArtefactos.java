package io.synkra.catalogo;

import io.synkra.catalogo.api.CatalogoApi;
import java.io.IOException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** H-15, R-C13: descarga de un JAR por su SHA-256, en flujo. Sin autenticación ni rangos (U10). */
@RestController
@RequestMapping("/api/artefactos")
class RutasArtefactos {

    static final String CABECERA_SHA256 = "X-Synkra-Sha256";
    private static final MediaType JAR = MediaType.parseMediaType("application/java-archive");

    private final CatalogoApi api;

    RutasArtefactos(CatalogoApi api) {
        this.api = api;
    }

    @GetMapping("/{sha256}")
    ResponseEntity<InputStreamResource> obtener(@PathVariable String sha256) throws IOException {
        var a = api.abrirArtefacto(sha256);
        return ResponseEntity.ok().contentType(JAR).contentLength(a.tamano())
                .header(CABECERA_SHA256, a.sha256())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(a.nombreDescarga()).build().toString())
                .body(new InputStreamResource(a.flujo()));
    }
}
