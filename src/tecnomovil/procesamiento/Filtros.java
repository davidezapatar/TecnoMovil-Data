package tecnomovil.procesamiento;

import tecnomovil.modelo.Accion;
import tecnomovil.modelo.Registro;

import java.time.LocalDateTime;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Funciones pequenas y reutilizables (predicados y funciones) que se
 * combinan entre si con and(), or(), negate() y andThen().
 */
public final class Filtros {

    private Filtros() { }

    /** true si el registro es una ENTRADA. */
    public static final Predicate<Registro> ES_ENTRADA = r -> r.accion() == Accion.ENTRADA;

    /** true si el registro es una SALIDA (se construye negando el anterior). */
    public static final Predicate<Registro> ES_SALIDA = ES_ENTRADA.negate();

    /** Composicion de funciones: Registro -> fecha y hora -> hora del dia. */
    public static final Function<Registro, Integer> HORA_DEL_REGISTRO =
            ((Function<Registro, LocalDateTime>) Registro::timestamp).andThen(LocalDateTime::getHour);

    /**
     * Funcion de orden superior: RECIBE dos horas y DEVUELVE un predicado nuevo.
     * Ejemplo: entreHoras(6, 9) es "el registro ocurrio entre las 6:00 y las 8:59".
     */
    public static Predicate<Registro> entreHoras(int desde, int hastaSinIncluir) {
        return r -> HORA_DEL_REGISTRO.apply(r) >= desde && HORA_DEL_REGISTRO.apply(r) < hastaSinIncluir;
    }

    /** Funcion de orden superior: devuelve un predicado que revisa la ruta. */
    public static Predicate<Registro> deRuta(String ruta) {
        return r -> r.ruta().equals(ruta);
    }

    /** Funcion de orden superior: devuelve un predicado que dice si una ocupacion supera el umbral. */
    public static DoublePredicate superaUmbral(double umbral) {
        return ocupacion -> ocupacion > umbral;
    }

    /** Ejemplo de composicion: entradas en la hora pico de la manana (6:00 a 8:59). */
    public static final Predicate<Registro> ENTRADA_PICO_MANANA = ES_ENTRADA.and(entreHoras(6, 9));
}
