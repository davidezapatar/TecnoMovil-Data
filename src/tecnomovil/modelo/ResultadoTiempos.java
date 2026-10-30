package tecnomovil.modelo;

import java.util.Map;

/**
 * Resultado del calculo de tiempos entre estaciones consecutivas.
 *
 * @param promedioGeneralMinutos tiempo promedio de todos los desplazamientos
 * @param totalDesplazamientos   cantidad de desplazamientos analizados
 * @param promedioPorTramo       tiempo promedio (minutos) de cada tramo origen -> destino
 * @param viajesPorTramo         cuantas veces se recorrio cada tramo
 */
public record ResultadoTiempos(double promedioGeneralMinutos,
                               long totalDesplazamientos,
                               Map<Tramo, Double> promedioPorTramo,
                               Map<Tramo, Long> viajesPorTramo) {

    public ResultadoTiempos {
        promedioPorTramo = Map.copyOf(promedioPorTramo); // copias inmutables
        viajesPorTramo = Map.copyOf(viajesPorTramo);
    }
}
