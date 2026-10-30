package tecnomovil.datos;

import tecnomovil.modelo.Accion;
import tecnomovil.modelo.Registro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Genera datos simulados de un dia de operacion del transporte de TecnoValle.
 *
 * Todo el generador es PURO: no usa Random con estado. Cada valor "aleatorio"
 * se calcula con una formula a partir de la semilla y del numero del usuario.
 * Por eso, con la misma semilla y la misma cantidad de usuarios, el resultado
 * es SIEMPRE el mismo, y se puede generar en paralelo sin problemas.
 */
public final class GeneradorDatos {

    private GeneradorDatos() { }

    /** Dia que se simula. */
    public static final LocalDate FECHA = LocalDate.of(2026, 10, 30);

    /** Definicion de una ruta: su nombre, sus estaciones en orden y su "popularidad". */
    public record Ruta(String nombre, List<String> estaciones, int peso) {
        public Ruta {
            estaciones = List.copyOf(estaciones);
        }
    }

    /** Las 8 rutas del sistema (datos ficticios). */
    public static final List<Ruta> RUTAS = List.of(
        new Ruta("T1-Troncal Norte", List.of("Portal Norte", "Calle 100", "Unicentro", "Estacion Central",
                "Plaza Mayor", "Parque Berrio"), 20),
        new Ruta("T2-Troncal Sur", List.of("Portal Sur", "Ciudad Jardin", "La Esperanza", "Estacion Central",
                "Terminal Sur"), 17),
        new Ruta("T3-Avenida Oriental", List.of("Portal Oriente", "Hospital General", "Universidad",
                "Plaza Mayor", "Centro Administrativo"), 14),
        new Ruta("A1-Alimentadora Los Pinos", List.of("Los Pinos", "Villa Nueva", "Portal Norte"), 9),
        new Ruta("A2-Alimentadora El Bosque", List.of("El Bosque", "San Antonio", "Portal Sur"), 8),
        new Ruta("C1-Circular Centro", List.of("Estacion Central", "Mercado Municipal", "Parque Berrio",
                "Biblioteca", "Plaza Mayor"), 12),
        new Ruta("E1-Expreso Aeropuerto", List.of("Estacion Central", "Zona Franca", "Aeropuerto"), 5),
        new Ruta("C2-Circular Universitaria", List.of("Universidad", "Parque Tecnologico", "Biblioteca",
                "Hospital General"), 10)
    );

    /** Peso de cada hora del dia (0 a 23). Mas peso = mas viajes empiezan a esa hora. */
    private static final int[] PESO_HORA = {
        0, 0, 0, 0, 0, 3, 14, 20, 16, 8, 6, 6, 9, 8, 6, 6, 10, 17, 19, 12, 7, 4, 2, 0
    };

    /** Lista de horas repetidas segun su peso (para elegir horas con mas probabilidad). */
    private static final List<Integer> HORAS_PONDERADAS = IntStream.range(0, 24)
            .boxed()
            .flatMap(h -> Collections.nCopies(PESO_HORA[h], h).stream())
            .toList();

    /** Lista de rutas repetidas segun su peso. */
    private static final List<Ruta> RUTAS_PONDERADAS = RUTAS.stream()
            .flatMap(r -> Collections.nCopies(r.peso(), r).stream())
            .toList();

    /**
     * Genera los registros de un dia completo, ordenados por hora.
     *
     * @param cantidadUsuarios cuantos usuarios distintos viajan
     * @param semilla          numero que define la simulacion
     * @return lista INMUTABLE de registros
     */
    public static List<Registro> generar(int cantidadUsuarios, long semilla) {
        return IntStream.range(0, cantidadUsuarios)
                .parallel()
                .boxed()
                .flatMap(u -> registrosDeUsuario(u, semilla))
                .sorted(Comparator.comparing(Registro::timestamp)
                        .thenComparing(Registro::idUsuario)
                        .thenComparing(Registro::accion))
                .toList(); // toList() devuelve una lista que no se puede modificar
    }

    /** Todos los registros de un usuario: 1 a 3 viajes, cada uno con una ENTRADA y una SALIDA. */
    private static Stream<Registro> registrosDeUsuario(int usuario, long semilla) {
        String id = String.format("U%06d", usuario + 1);
        int viajes = 1 + elegir(mezclar(semilla, usuario, 1), 3);
        int horaInicio = HORAS_PONDERADAS.get(elegir(mezclar(semilla, usuario, 2), HORAS_PONDERADAS.size()));
        return IntStream.range(0, viajes)
                .boxed()
                .flatMap(viaje -> registrosDeViaje(id, usuario, viaje, horaInicio, semilla));
    }

    /** Los dos registros (entrada y salida) de un viaje. Devuelve un stream vacio si el viaje queda despues de las 22 h. */
    private static Stream<Registro> registrosDeViaje(String id, int usuario, int viaje, int horaInicio, long semilla) {
        long base = usuario * 10L + viaje;
        int hora = horaInicio + viaje * (3 + elegir(mezclar(semilla, base, 3), 4));
        if (hora > 22) {
            return Stream.empty();
        }
        Ruta ruta = RUTAS_PONDERADAS.get(elegir(mezclar(semilla, base, 4), RUTAS_PONDERADAS.size()));
        int n = ruta.estaciones().size();
        int origen = elegir(mezclar(semilla, base, 5), n);
        int destino = (origen + 1 + elegir(mezclar(semilla, base, 6), n - 1)) % n;   // siempre distinto al origen
        int paradas = Math.abs(destino - origen);
        int minutosViaje = paradas * (3 + elegir(mezclar(semilla, base, 7), 3)) + elegir(mezclar(semilla, base, 8), 5);

        LocalDateTime entrada = FECHA.atTime(hora, elegir(mezclar(semilla, base, 9), 60),
                elegir(mezclar(semilla, base, 10), 60));
        LocalDateTime salida = entrada.plusMinutes(minutosViaje).plusSeconds(elegir(mezclar(semilla, base, 11), 60));

        return Stream.of(
                new Registro(id, ruta.nombre(), ruta.estaciones().get(origen), Accion.ENTRADA, entrada),
                new Registro(id, ruta.nombre(), ruta.estaciones().get(destino), Accion.SALIDA, salida));
    }

    /**
     * Funcion pura que "revuelve" tres numeros y devuelve uno que parece aleatorio
     * (algoritmo SplitMix64). Mismas entradas -> misma salida, siempre.
     */
    static long mezclar(long semilla, long indice, long sal) {
        long z = semilla + indice * 0x9E3779B97F4A7C15L + sal * 0xD1B54A32D192ED03L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Convierte un numero revuelto en una posicion entre 0 y n-1. */
    static int elegir(long numero, int n) {
        return (int) Math.floorMod(numero, (long) n);
    }
}
