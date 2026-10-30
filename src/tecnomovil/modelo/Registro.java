package tecnomovil.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Un registro del sistema de transporte (lo que envia un validador o sensor).
 *
 * Es un "record": Java crea automaticamente el constructor, los metodos de
 * lectura, equals, hashCode y toString. Todos sus campos son final, por eso
 * un Registro NO se puede modificar despues de creado (es inmutable).
 */
public record Registro(String idUsuario,
                       String ruta,
                       String estacion,
                       Accion accion,
                       LocalDateTime timestamp) {

    /** Constructor compacto: impide crear registros con datos nulos. */
    public Registro {
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(ruta, "ruta");
        Objects.requireNonNull(estacion, "estacion");
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    /** Hora del dia (0 a 23) en que ocurrio el registro. */
    public int hora() {
        return timestamp.getHour();
    }

    public boolean esEntrada() {
        return accion == Accion.ENTRADA;
    }
}
