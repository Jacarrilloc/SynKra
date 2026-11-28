/** Módulo Despliegue: planes, verificación, confirmación y reversión (prefijo dep_). */
@ApplicationModule(allowedDependencies = {"catalogo :: api", "kubernetes :: api", "registro :: api", "plataforma"})
package io.synkra.despliegue;

import org.springframework.modulith.ApplicationModule;
