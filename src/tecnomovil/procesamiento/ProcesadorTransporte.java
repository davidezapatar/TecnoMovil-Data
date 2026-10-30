package tecnomovil.procesamiento;

import tecnomovil.modelo.EstadoRuta;
import tecnomovil.modelo.Registro;
import tecnomovil.modelo.ResultadoTiempos;
import tecnomovil.modelo.Tramo;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Modulo de procesamiento funcional de TecnoMovil Data.
 *
 * Reglas que cumplen TODOS los metodos de esta clase:
 *  1. Son funciones puras: el resultado depende solo de los parametros y no
 *     modifican nada fuera de ellas (no imprimen, no cambian variables globales).
 *  2. Nunca modifican la lista de registros que reciben.
 *  3. Devuelven colecciones que no se pueden modificar.
 *  4. Con paralelo = true usan parallelStream() y el resultado es el mismo.
 */
public final class ProcesadorTransporte {

    private ProcesadorTransporte() { }

    /** Crea el stream secuencial o paralelo segun se pida. */
    private static Stream<Registro> flujo(List<Registro> registros, boolean paralelo) {
        return paralelo ? registros.parallelStream() : registros.stream();
    }

    // ---------------------------------------------------------------------
    // a) Afluencia por estacion
    // ---------------------------------------------------------------------

    /** Cuantas entradas hubo en cada estacion durante el dia. */
    public static Map<String, Long> afluenciaPorEstacion(List<Registro> registros, boolean paralelo) {
        return flujo(registros, paralelo)
                .filter(Filtros.ES_ENTRADA)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Registro::estacion, TreeMap::new, Collectors.counting()),
                        Collections::unmodifiableMap));
    }

    /** Cuantos usuarios DISTINTOS entraron a cada estacion (un usuario cuenta una sola vez). */
    public static Map<String, Long> usuariosUnicosPorEstacion(List<Registro> registros, boolean paralelo) {
        return flujo(registros, paralelo)
                .filter(Filtros.ES_ENTRADA)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Registro::estacion, TreeMap::new,
                                Collectors.collectingAndThen(
                                        Collectors.mapping(Registro::idUsuario, Collectors.toSet()),
                                        usuarios -> (long) usuarios.size())),
                        Collections::unmodifiableMap));
    }

    // ---------------------------------------------------------------------
    // b) Horas pico
    // ---------------------------------------------------------------------

    /** Flujo total (entradas + salidas) agrupado por hora del dia. */
    public static Map<Integer, Long> flujoPorHora(List<Registro> registros, boolean paralelo) {
        return flujo(registros, paralelo)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Filtros.HORA_DEL_REGISTRO, TreeMap::new, Collectors.counting()),
                        Collections::unmodifiableMap));
    }

    /** Las n horas con mas flujo. */
    public static List<Map.Entry<Integer, Long>> horasPico(Map<Integer, Long> flujoPorHora, int n) {
        return topN(flujoPorHora, n);
    }

    // ---------------------------------------------------------------------
    // c) Rutas mas utilizadas
    // ---------------------------------------------------------------------

    /** Usos (entradas) de cada ruta. */
    public static Map<String, Long> usosPorRuta(List<Registro> registros, boolean paralelo) {
        return flujo(registros, paralelo)
                .filter(Filtros.ES_ENTRADA)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Registro::ruta, TreeMap::new, Collectors.counting()),
                        Collections::unmodifiableMap));
    }

    // ---------------------------------------------------------------------
    // d) Patrones de viaje por usuario
    // ---------------------------------------------------------------------

    /**
     * Para cada usuario, la lista de estaciones que visito, en el mismo orden
     * en que aparecen en los registros. groupingBy conserva ese orden aunque
     * el stream sea paralelo, porque la lista de origen esta ordenada.
     */
    public static Map<String, List<String>> patronesPorUsuario(List<Registro> registros, boolean paralelo) {
        return flujo(registros, paralelo)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Registro::idUsuario, TreeMap::new,
                                Collectors.mapping(Registro::estacion, Collectors.toUnmodifiableList())),
                        Collections::unmodifiableMap));
    }

    // ---------------------------------------------------------------------
    // e) Tiempo promedio entre estaciones consecutivas
    // ---------------------------------------------------------------------

    /** Un desplazamiento: va de una ENTRADA a la SALIDA siguiente del mismo usuario, en otra estacion. */
    private static final BiPredicate<Registro, Registro> ES_DESPLAZAMIENTO =
            (a, b) -> a.esEntrada() && !b.esEntrada() && !a.estacion().equals(b.estacion());

    /** Minutos entre dos registros (con decimales). */
    private static double minutosEntre(Registro a, Registro b) {
        return Duration.between(a.timestamp(), b.timestamp()).toSeconds() / 60.0;
    }

    /** Par (tramo, minutos) de un desplazamiento. */
    private record Desplazamiento(Tramo tramo, double minutos) { }

    /** Convierte la lista de registros de UN usuario en sus desplazamientos. */
    private static Stream<Desplazamiento> desplazamientosDe(List<Registro> registrosDelUsuario) {
        List<Registro> ordenados = registrosDelUsuario.stream()
                .sorted(Comparator.comparing(Registro::timestamp))
                .toList();                                   // lista nueva, no se toca la original
        return IntStream.range(0, ordenados.size() - 1)
                .mapToObj(i -> List.of(ordenados.get(i), ordenados.get(i + 1)))  // pares consecutivos
                .filter(par -> ES_DESPLAZAMIENTO.test(par.get(0), par.get(1)))
                .map(par -> new Desplazamiento(
                        new Tramo(par.get(0).estacion(), par.get(1).estacion()),
                        minutosEntre(par.get(0), par.get(1))));
    }

    /** Tiempo promedio general y por tramo entre estaciones consecutivas. */
    public static ResultadoTiempos tiemposEntreEstaciones(List<Registro> registros, boolean paralelo) {
        List<Desplazamiento> desplazamientos = flujo(registros, paralelo)
                .collect(Collectors.groupingBy(Registro::idUsuario, TreeMap::new, Collectors.toList()))
                .values()                                   // TreeMap: mismo orden siempre
                .stream()
                .flatMap(ProcesadorTransporte::desplazamientosDe)
                .toList();

        double promedioGeneral = desplazamientos.stream()
                .mapToDouble(Desplazamiento::minutos)
                .average()
                .orElse(0.0);

        Map<Tramo, Double> porTramo = desplazamientos.stream()
                .collect(Collectors.groupingBy(Desplazamiento::tramo,
                        Collectors.averagingDouble(Desplazamiento::minutos)));

        Map<Tramo, Long> viajesPorTramo = desplazamientos.stream()
                .collect(Collectors.groupingBy(Desplazamiento::tramo, Collectors.counting()));

        return new ResultadoTiempos(promedioGeneral, desplazamientos.size(), porTramo, viajesPorTramo);
    }

    // ---------------------------------------------------------------------
    // f) Deteccion de sobrecarga en rutas
    // ---------------------------------------------------------------------

    /**
     * Calcula la ocupacion de cada ruta en su hora con mas entradas y la marca
     * como CRITICA si supera el umbral.
     *
     * @param capacidadPorHora pasajeros que la ruta puede mover en una hora (simulado)
     * @param umbral           por ejemplo 0.85 = 85 % de la capacidad
     */
    public static List<EstadoRuta> estadoDeRutas(List<Registro> registros, long capacidadPorHora,
                                                 double umbral, boolean paralelo) {
        DoublePredicate esCritica = Filtros.superaUmbral(umbral);

        Map<String, Map<Integer, Long>> entradasPorRutaYHora = flujo(registros, paralelo)
                .filter(Filtros.ES_ENTRADA)
                .collect(Collectors.groupingBy(Registro::ruta,
                        Collectors.groupingBy(Registro::hora, Collectors.counting())));

        Function<Map.Entry<String, Map<Integer, Long>>, EstadoRuta> aEstado = e -> {
            Map.Entry<Integer, Long> pico = topN(e.getValue(), 1).get(0);
            long total = e.getValue().values().stream().mapToLong(Long::longValue).sum();
            double ocupacion = (double) pico.getValue() / capacidadPorHora;
            return new EstadoRuta(e.getKey(), total, pico.getKey(), pico.getValue(),
                    ocupacion, esCritica.test(ocupacion));
        };

        return entradasPorRutaYHora.entrySet().stream()
                .map(aEstado)
                .sorted(Comparator.comparingDouble(EstadoRuta::ocupacion).reversed()
                        .thenComparing(EstadoRuta::ruta))
                .toList();
    }

    // ---------------------------------------------------------------------
    // Funcion generica reutilizable
    // ---------------------------------------------------------------------

    /**
     * Las n parejas (llave, cantidad) con mayor cantidad. Si hay empate, ordena por la llave.
     * Es generica: sirve para estaciones, horas, rutas, etc.
     */
    public static <K extends Comparable<? super K>> List<Map.Entry<K, Long>> topN(Map<K, Long> conteos, int n) {
        return conteos.entrySet().stream()
                .sorted(Map.Entry.<K, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.<K, Long>comparingByKey()))
                .limit(n)
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .toList();
    }
}
