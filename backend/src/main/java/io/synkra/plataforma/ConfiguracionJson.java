package io.synkra.plataforma;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Un número o un bool donde va texto es un error de tipo, no se convierte a texto en silencio (C-07). */
@Configuration
class ConfiguracionJson {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer sinCoercionATexto() {
        return b -> b.postConfigurer(m -> {
            var texto = m.coercionConfigFor(LogicalType.Textual);
            texto.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
            texto.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
            texto.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
        });
    }
}
