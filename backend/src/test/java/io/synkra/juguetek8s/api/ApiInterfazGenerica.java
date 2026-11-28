package io.synkra.juguetek8s.api;

import io.fabric8.kubernetes.api.model.Service;
import java.util.function.Supplier;

/** Juguete: Fabric8 como argumento de tipo de una interfaz. */
public interface ApiInterfazGenerica extends Supplier<Service> {}
