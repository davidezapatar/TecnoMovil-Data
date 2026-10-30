package tecnomovil.modelo;

/**
 * Desplazamiento entre dos estaciones consecutivas (origen -> destino).
 * Se usa como llave para agrupar los tiempos de viaje.
 */
public record Tramo(String origen, String destino) {

    @Override
    public String toString() {
        return origen + " -> " + destino;
    }
}
