package tecnomovil.reportes;

import tecnomovil.modelo.EstadoRuta;
import tecnomovil.modelo.ResultadoTiempos;
import tecnomovil.modelo.Tramo;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Convierte los resultados en texto para mostrarlos.
 * Son funciones puras: reciben datos y DEVUELVEN un String; no imprimen nada.
 * Quien imprime es la clase App (el unico lugar con efectos secundarios).
 */
public final class Reporte {

    private Reporte() { }

    private static final int ANCHO_BARRA = 30;

    /** Titulo de una seccion. */
    public static String titulo(String texto) {
        String linea = "=".repeat(72);
        return "\n" + linea + "\n " + texto + "\n" + linea;
    }

    /** Numero con separador de miles (ej: 12.345). */
    public static String numero(long n) {
        return String.format(Locale.GERMANY, "%,d", n);
    }

    /** Barra de texto proporcional al valor maximo. */
    public static String barra(long valor, long maximo) {
        int largo = maximo == 0 ? 0 : (int) Math.round((double) valor / maximo * ANCHO_BARRA);
        return "#".repeat(largo);
    }

    /** Tabla de "llave -> cantidad" con barras. La funcion etiqueta decide como se muestra la llave. */
    public static <K> String tablaConBarras(List<Map.Entry<K, Long>> filas, Function<K, String> etiqueta) {
        long maximo = filas.stream().mapToLong(Map.Entry::getValue).max().orElse(0);
        return filas.stream()
                .map(e -> String.format("  %-26s %9s  %s",
                        etiqueta.apply(e.getKey()), numero(e.getValue()), barra(e.getValue(), maximo)))
                .collect(Collectors.joining("\n"));
    }

    /** Muestra "06:00 - 06:59" a partir de la hora 6. */
    public static String rangoHora(int hora) {
        return String.format("%02d:00 - %02d:59", hora, hora);
    }

    /** Patrones de viaje de algunos usuarios. */
    public static String patrones(Map<String, List<String>> patrones, int cuantos) {
        return patrones.entrySet().stream()
                .limit(cuantos)
                .map(e -> "  " + e.getKey() + ": " + String.join(" -> ", e.getValue()))
                .collect(Collectors.joining("\n"));
    }

    /** Resumen de tiempos entre estaciones y los tramos mas usados. */
    public static String tiempos(ResultadoTiempos r, int cuantos) {
        String encabezado = String.format(Locale.US,
                "  Desplazamientos analizados: %s%n  Tiempo promedio general: %.1f minutos%n%n"
                        + "  %-45s %8s %10s", numero(r.totalDesplazamientos()), r.promedioGeneralMinutos(),
                "Tramo (los mas recorridos)", "Viajes", "Promedio");
        String filas = r.viajesPorTramo().entrySet().stream()
                .sorted(Map.Entry.<Tramo, Long>comparingByValue().reversed()
                        .thenComparing(e -> e.getKey().toString()))
                .limit(cuantos)
                .map(e -> String.format(Locale.US, "  %-45s %8s %7.1f min", e.getKey(),
                        numero(e.getValue()), r.promedioPorTramo().get(e.getKey())))
                .collect(Collectors.joining("\n"));
        return encabezado + "\n" + filas;
    }

    /** Tabla de estado de las rutas. */
    public static String estadoRutas(List<EstadoRuta> estados, long capacidad, double umbral) {
        String encabezado = String.format(Locale.US,
                "  Capacidad simulada: %s pasajeros por hora | Umbral: %.0f %%%n%n  %-27s %8s %13s %9s  %s",
                numero(capacidad), umbral * 100, "Ruta", "Usos dia", "Hora pico", "Ocupacion", "Estado");
        String filas = estados.stream()
                .sorted(Comparator.comparingDouble(EstadoRuta::ocupacion).reversed())
                .map(e -> String.format(Locale.US, "  %-27s %8s %13s %8.1f%%  %s",
                        e.ruta(), numero(e.usosTotales()), rangoHora(e.horaPico()),
                        e.ocupacion() * 100, e.critica() ? "CRITICA (!)" : "normal"))
                .collect(Collectors.joining("\n"));
        return encabezado + "\n" + filas;
    }
}
