package tecnomovil;

import tecnomovil.modelo.Accion;
import tecnomovil.modelo.EstadoRuta;
import tecnomovil.modelo.Registro;
import tecnomovil.modelo.ResultadoTiempos;
import tecnomovil.modelo.Tramo;
import tecnomovil.procesamiento.Filtros;
import tecnomovil.procesamiento.ProcesadorTransporte;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Pruebas del modulo SIN frameworks externos.
 *
 * Se usa un conjunto pequeno de registros hechos a mano, donde el resultado
 * correcto se puede calcular con lapiz y papel, y se compara con lo que
 * devuelven las funciones.
 *
 * Uso: java -cp out tecnomovil.Verificacion
 */
public final class Verificacion {

    /** Resultado de una prueba. */
    private record Prueba(String nombre, boolean paso, String detalle) { }

    private static Prueba prueba(String nombre, Object esperado, Supplier<Object> obtenido) {
        Object valor = obtenido.get();
        return new Prueba(nombre, esperado.equals(valor), "esperado=" + esperado + " | obtenido=" + valor);
    }

    private static LocalDateTime t(int hora, int minuto) {
        return LocalDateTime.of(2026, 10, 30, hora, minuto);
    }

    /** Datos de prueba: 3 usuarios, 2 rutas, 10 registros. */
    private static final List<Registro> DATOS = List.of(
            new Registro("U1", "R1", "A", Accion.ENTRADA, t(7, 0)),
            new Registro("U2", "R1", "A", Accion.ENTRADA, t(7, 10)),
            new Registro("U1", "R1", "C", Accion.SALIDA, t(7, 20)),
            new Registro("U3", "R2", "B", Accion.ENTRADA, t(7, 30)),
            new Registro("U2", "R1", "B", Accion.SALIDA, t(7, 40)),
            new Registro("U3", "R2", "D", Accion.SALIDA, t(8, 0)),
            new Registro("U1", "R2", "C", Accion.ENTRADA, t(17, 0)),
            new Registro("U1", "R2", "D", Accion.SALIDA, t(17, 30)),
            new Registro("U2", "R1", "B", Accion.ENTRADA, t(18, 0)),
            new Registro("U2", "R1", "A", Accion.SALIDA, t(18, 10))
    );

    public static void main(String[] args) {
        int huellaAntes = DATOS.hashCode();
        ResultadoTiempos tiempos = ProcesadorTransporte.tiemposEntreEstaciones(DATOS, false);

        List<Prueba> pruebas = List.of(
            prueba("a) Afluencia por estacion",
                Map.of("A", 2L, "B", 2L, "C", 1L),
                () -> ProcesadorTransporte.afluenciaPorEstacion(DATOS, false)),
            prueba("a) Usuarios unicos por estacion",
                Map.of("A", 2L, "B", 2L, "C", 1L),
                () -> ProcesadorTransporte.usuariosUnicosPorEstacion(DATOS, false)),
            prueba("b) Flujo por hora",
                Map.of(7, 5L, 8, 1L, 17, 2L, 18, 2L),
                () -> ProcesadorTransporte.flujoPorHora(DATOS, false)),
            prueba("b) Hora pico",
                7,
                () -> ProcesadorTransporte.horasPico(ProcesadorTransporte.flujoPorHora(DATOS, false), 1).get(0).getKey()),
            prueba("c) Usos por ruta",
                Map.of("R1", 3L, "R2", 2L),
                () -> ProcesadorTransporte.usosPorRuta(DATOS, false)),
            prueba("d) Patron del usuario U1",
                List.of("A", "C", "C", "D"),
                () -> ProcesadorTransporte.patronesPorUsuario(DATOS, false).get("U1")),
            prueba("d) Patron del usuario U2",
                List.of("A", "B", "B", "A"),
                () -> ProcesadorTransporte.patronesPorUsuario(DATOS, false).get("U2")),
            // Desplazamientos: U1 A->C 20 min, U1 C->D 30 min, U2 A->B 30 min, U2 B->A 10 min, U3 B->D 30 min
            prueba("e) Cantidad de desplazamientos",
                5L,
                tiempos::totalDesplazamientos),
            prueba("e) Tiempo promedio general (minutos)",
                24.0,
                tiempos::promedioGeneralMinutos),
            prueba("e) Tiempo promedio del tramo A -> C",
                20.0,
                () -> tiempos.promedioPorTramo().get(new Tramo("A", "C"))),
            // Entradas por ruta y hora: R1 -> 7h: 2, 18h: 1 | R2 -> 7h: 1, 17h: 1. Capacidad 2, umbral 0.75
            prueba("f) Rutas criticas (capacidad 2, umbral 75 %)",
                List.of("R1"),
                () -> ProcesadorTransporte.estadoDeRutas(DATOS, 2, 0.75, false).stream()
                        .filter(EstadoRuta::critica).map(EstadoRuta::ruta).toList()),
            prueba("f) Ocupacion de R1 en su hora pico",
                1.0,
                () -> ProcesadorTransporte.estadoDeRutas(DATOS, 2, 0.75, false).get(0).ocupacion()),
            prueba("Predicados compuestos: entradas entre 6:00 y 8:59",
                3L,
                () -> DATOS.stream().filter(Filtros.ENTRADA_PICO_MANANA).count()),
            prueba("Paralelo da lo mismo que secuencial (patrones)",
                ProcesadorTransporte.patronesPorUsuario(DATOS, false),
                () -> ProcesadorTransporte.patronesPorUsuario(DATOS, true)),
            prueba("Inmutabilidad: el resultado no se puede modificar",
                "UnsupportedOperationException",
                () -> {
                    try {
                        ProcesadorTransporte.afluenciaPorEstacion(DATOS, false).put("Z", 1L);
                        return "se modifico";
                    } catch (UnsupportedOperationException e) {
                        return "UnsupportedOperationException";
                    }
                }),
            prueba("Inmutabilidad: la lista original no cambio",
                huellaAntes,
                DATOS::hashCode)
        );

        System.out.println("PRUEBAS DEL MODULO TECNOMOVIL DATA (sin frameworks)\n");
        pruebas.forEach(p -> System.out.println((p.paso() ? "  [OK]    " : "  [FALLA] ") + p.nombre()
                + (p.paso() ? "" : "\n          " + p.detalle())));

        long aprobadas = pruebas.stream().filter(Prueba::paso).count();
        System.out.println("\n  Resultado: " + aprobadas + " de " + pruebas.size() + " pruebas aprobadas");
        if (aprobadas != pruebas.size()) {
            System.exit(1);
        }
    }
}
