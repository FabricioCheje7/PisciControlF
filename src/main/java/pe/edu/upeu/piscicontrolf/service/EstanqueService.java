
package pe.edu.upeu.piscicontrolf.service;

import pe.edu.upeu.piscicontrolf.model.Estanque;
import pe.edu.upeu.piscicontrolf.repository.EstanqueRepository;

import java.sql.SQLException;
import java.util.List;

public class EstanqueService {

    private final EstanqueRepository repository =
            new EstanqueRepository();

    // REGISTRAR ESTANQUE
    public void registrar(Estanque estanque)
            throws SQLException {

        validarEstanque(estanque);

        estanque.setEstado("ACTIVO");

        repository.registrar(estanque);
    }

    // CONSULTAR ESTANQUES
    public List<Estanque> listar()
            throws SQLException {

        return repository.listar();
    }

    // MODIFICAR ESTANQUE
    public void actualizar(Estanque estanque)
            throws SQLException {

        validarEstanque(estanque);

        if (estanque.getId() == null ||
                estanque.getId() <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un estanque válido.");
        }

        repository.actualizar(estanque);
    }

    // DESACTIVAR ESTANQUE
    public void desactivar(Long id)
            throws SQLException {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un estanque válido.");
        }

        repository.desactivar(id);
    }

    // VALIDACIONES DEL SISTEMA
    private void validarEstanque(Estanque estanque) {

        if (estanque == null) {
            throw new IllegalArgumentException(
                    "Los datos del estanque son obligatorios.");
        }

        if (estanque.getNombre() == null ||
                estanque.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                    "Ingrese el nombre del estanque.");
        }

        String nombre = estanque.getNombre().trim();

        if (nombre.length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre no debe superar 100 caracteres.");
        }

        if (estanque.getCapacidad() <= 0) {
            throw new IllegalArgumentException(
                    "La capacidad debe ser mayor que cero.");
        }

        String ubicacion = estanque.getUbicacion();

        if (ubicacion != null &&
                ubicacion.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "La ubicación no debe superar 150 caracteres.");
        }

        estanque.setNombre(nombre);

        if (ubicacion != null) {
            estanque.setUbicacion(ubicacion.trim());
        }
    }
}
