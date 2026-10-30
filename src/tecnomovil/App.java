package tecnomovil;

import tecnomovil.datos.GeneradorDatos;
import tecnomovil.modelo.Accion;
import tecnomovil.modelo.EstadoRuta;
import tecnomovil.modelo.Registro;
import tecnomovil.modelo.ResultadoTiempos;
import tecnomovil.procesamiento.Filtros;
import tecnomovil.procesamiento.ProcesadorTransporte;
import tecnomovil.reportes.Reporte;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Programa principal de TecnoMovil Data.
 *
 * Esta es la UNICA clase que tiene efectos secundarios (imprimir en pantalla
 * y medir el tiempo). Todo el calculo se hace en ProcesadorTransporte con
 * funciones puras. Asi se separa "calcular" de "mostrar".
 *
 * Uso: java -cp out tecnomovil.App [cantidadDeUsuarios]
 */
public final class App {

    private static final long SEMILLA = 2026L;
    private static final int USUARIOS_POR_DEFECTO = 150_000;
    private static final long CAPACIDAD_POR_HORA = 4_500L;   // pasajeros por hora por ruta (simulado)
    private static final double UMBRAL_CRITICO = 0.85;        // 85 % de la capacidad

    /** Resultado de una operacion y cuanto se demoro. */
    private record Medicion<T>(T valor, long milisegundos) { }

    private static <T> Medicion<T> medir(Supplier<T> operacion) {
        long inicio = System.nanoTime();
        T valor = operacion.get();
        return new Medicion<>(valor, (System.nanoTime() - inicio) / 1_000_000);
    }

    /** Todos los resultados juntos, para comparar el modo secuencial con el paralelo. */
    private record Resultados(Map<String, Long> afluencia,
                              Map<String, Long> usuariosUnicos,
                              Map<Integer, Long> flujoPorHora,
                              Map<String, Long> usosPorRuta,
                              Map<String, List<String>> patrones,
                              ResultadoTiempos tiempos,
                              List<EstadoRuta> estadoRutas) { }

    /** Ejecuta las seis tareas (a-f) sobre la misma lista. Es una funcion pura. */
    private static Resultados procesarTodo(List<Registro> registros, boolean paralelo) {
        return new Resultados(
                ProcesadorTransporte.afluenciaPorEstacion(registros, paralelo),
                ProcesadorTransporte.usuariosUnicosPorEstacion(registros, paralelo),
                ProcesadorTransporte.flujoPorHora(registros, paralelo),
                ProcesadorTransporte.usosPorRuta(registros, paralelo),
                ProcesadorTransporte.patronesPorUsuario(registros, paralelo),
                ProcesadorTransporte.tiemposEntreEstaciones(registros, paralelo),
                ProcesadorTransporte.estadoDeRutas(registros, CAPACIDAD_POR_HORA, UMBRAL_CRITICO, paralelo));
    }

    public static void main(String[] args) {
        int usuarios = args.length > 0 ? Integer.parseInt(args[0]) : USUARIOS_POR_DEFECTO;

        System.out.println(Reporte.titulo("TECNOMOVIL DATA - Procesamiento funcional del transporte de TecnoValle"));
        System.out.println("  Java " + System.getProperty("java.version")
                + " | Nucleos del procesador: " + Runtime.getRuntime().availableProcessors());

        // 1. Generar los datos simulados ------------------------------------------------
        Medicion<List<Registro>> generacion = medir(() -> GeneradorDatos.generar(usuarios, SEMILLA));
        List<Registro> registros = generacion.valor();
        int huellaAntes = registros.hashCode();
        System.out.println("  Fecha simulada: " + GeneradorDatos.FECHA
                + " | Usuarios: " + Reporte.numero(usuarios)
                + " | Registros generados: " + Reporte.numero(registros.size())
                + " (" + generacion.milisegundos() + " ms)");

        // 2. Comprobar que la lista original no se puede modificar -----------------------
        System.out.println(Reporte.titulo("INMUTABILIDAD - Intento de modificar la lista original"));
        try {
            registros.add(new Registro("U999999", "Ruta falsa", "Estacion falsa", Accion.ENTRADA,
                    LocalDateTime.now()));
            System.out.println("  ERROR: la lista se dejo modificar");
        } catch (UnsupportedOperationException e) {
            System.out.println("  OK: Java no deja agregar registros -> UnsupportedOperationException");
        }

        // 3. Procesar en modo secuencial ---------------------------------------------------
        Medicion<Resultados> secuencial = medir(() -> procesarTodo(registros, false));
        Resultados r = secuencial.valor();

        System.out.println(Reporte.titulo("a) AFLUENCIA POR ESTACION (top 10 por entradas)"));
        System.out.println(String.format("  %-26s %9s", "Estacion", "Entradas"));
        System.out.println(Reporte.tablaConBarras(ProcesadorTransporte.topN(r.afluencia(), 10), e -> e));
        System.out.println("\n  Estacion con mas usuarios distintos: "
                + ProcesadorTransporte.topN(r.usuariosUnicos(), 1).get(0).getKey()
                + " (" + Reporte.numero(ProcesadorTransporte.topN(r.usuariosUnicos(), 1).get(0).getValue())
                + " usuarios)");

        System.out.println(Reporte.titulo("b) HORAS PICO (flujo de entradas + salidas por hora)"));
        System.out.println(Reporte.tablaConBarras(List.copyOf(r.flujoPorHora().entrySet()), Reporte::rangoHora));
        System.out.println("\n  Las 3 horas pico: " + ProcesadorTransporte.horasPico(r.flujoPorHora(), 3).stream()
                .map(e -> Reporte.rangoHora(e.getKey()) + " (" + Reporte.numero(e.getValue()) + ")")
                .reduce((x, y) -> x + ", " + y).orElse(""));
        long entradasPicoManana = registros.stream().filter(Filtros.ENTRADA_PICO_MANANA).count();
        System.out.println("  Entradas entre 6:00 y 8:59 (predicados compuestos): "
                + Reporte.numero(entradasPicoManana));

        System.out.println(Reporte.titulo("c) RUTAS MAS UTILIZADAS"));
        System.out.println(Reporte.tablaConBarras(ProcesadorTransporte.topN(r.usosPorRuta(), 8), e -> e));

        System.out.println(Reporte.titulo("d) PATRONES DE VIAJE POR USUARIO (primeros 8 de "
                + Reporte.numero(r.patrones().size()) + ")"));
        System.out.println(Reporte.patrones(r.patrones(), 8));

        System.out.println(Reporte.titulo("e) TIEMPO PROMEDIO ENTRE ESTACIONES CONSECUTIVAS"));
        System.out.println(Reporte.tiempos(r.tiempos(), 8));

        System.out.println(Reporte.titulo("f) DETECCION DE SOBRECARGA EN RUTAS"));
        System.out.println(Reporte.estadoRutas(r.estadoRutas(), CAPACIDAD_POR_HORA, UMBRAL_CRITICO));
        System.out.println("\n  Rutas criticas: " + r.estadoRutas().stream()
                .filter(EstadoRuta::critica).map(EstadoRuta::ruta).toList());

        // 4. Procesar en paralelo y comparar ------------------------------------------------
        Medicion<Resultados> paralelo = medir(() -> procesarTodo(registros, true));
        boolean mismosResultados = r.equals(paralelo.valor());

        System.out.println(Reporte.titulo("PARALELISMO - stream() contra parallelStream()"));
        System.out.println("  Secuencial: " + Reporte.numero(secuencial.milisegundos()) + " ms | Paralelo: "
                + Reporte.numero(paralelo.milisegundos()) + " ms");
        System.out.println("  Los dos modos dan exactamente los mismos resultados: " + (mismosResultados ? "SI" : "NO"));

        // 5. La lista original sigue igual ---------------------------------------------------
        boolean listaIntacta = huellaAntes == registros.hashCode();
        System.out.println(Reporte.titulo("VERIFICACION FINAL"));
        System.out.println("  La lista original no cambio despues de todo el proceso: " + (listaIntacta ? "SI" : "NO"));
        System.out.println("  Registros: " + Reporte.numero(registros.size()));
    }
}
