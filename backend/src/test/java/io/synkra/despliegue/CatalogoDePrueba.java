package io.synkra.despliegue;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.LectorDescriptor;
import io.synkra.verificador.SuiteCombinaciones;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

/** Carga en el catálogo real la suite de combinaciones (imágenes a, b y c), como lo hace {@code CrearPlanTest}. */
final class CatalogoDePrueba {

    static final String IMAGEN_A = "localhost:5001/synkra-core@sha256:" + "a".repeat(64);
    static final String IMAGEN_B = "localhost:5001/synkra-core@sha256:" + "b".repeat(64);
    static final String IMAGEN_C = "localhost:5001/otro-core@sha256:" + "c".repeat(64);

    private CatalogoDePrueba() {
    }

    static void cargar(CatalogoApi catalogo) throws IOException {
        for (var c : SuiteCombinaciones.contenido()) {
            for (var v : c.versiones()) {
                var imagen = c.core().equals("otro-core") ? IMAGEN_C : v.toString().equals("2.1.0") ? IMAGEN_A : IMAGEN_B;
                catalogo.registrarCore(c.core(), v.toString(), imagen);
            }
            for (var d : c.plugins()) {
                catalogo.subirPlugin(c.core(), new ByteArrayInputStream(jar(d)), Map.of());
            }
        }
    }

    private static byte[] jar(Descriptor d) throws IOException {
        var salida = new ByteArrayOutputStream();
        try (var z = new ZipArchiveOutputStream(salida)) {
            z.putArchiveEntry(new ZipArchiveEntry(LectorDescriptor.ARCHIVO));
            z.write(LectorDescriptor.aYaml(d).getBytes(StandardCharsets.UTF_8));
            z.closeArchiveEntry();
        }
        return salida.toByteArray();
    }
}
