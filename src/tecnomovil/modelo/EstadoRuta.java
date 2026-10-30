package tecnomovil.modelo;

/**
 * Resultado del analisis de ocupacion de una ruta.
 *
 * @param ruta            nombre de la ruta
 * @param usosTotales     entradas registradas en el dia
 * @param horaPico        hora con mas entradas en la ruta
 * @param entradasEnPico  entradas registradas en esa hora
 * @param ocupacion       entradasEnPico / capacidad por hora (1.0 = 100 %)
 * @param critica         true si la ocupacion supera el umbral definido
 */
public record EstadoRuta(String ruta,
                         long usosTotales,
                         int horaPico,
                         long entradasEnPico,
                         double ocupacion,
                         boolean critica) {

    public String estado() {
        return critica ? "CRITICA" : "NORMAL";
    }
}
