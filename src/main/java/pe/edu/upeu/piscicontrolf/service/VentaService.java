package pe.edu.upeu.piscicontrolf.service;

import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import pe.edu.upeu.piscicontrolf.model.Venta;
import pe.edu.upeu.piscicontrolf.repository.VentaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class VentaService {

    private final VentaRepository repository = new VentaRepository();

    // ADMINISTRADOR Y OPERADOR PUEDEN CONSULTAR.
    private void verificarPermisoConsulta() {
        if (!SesionUsuario.esAdministrador()
                && !SesionUsuario.esOperador()) {
            throw new SecurityException(
                    "Debe iniciar sesion con un usuario autorizado.");
        }
    }

    // ADMINISTRADOR Y OPERADOR PUEDEN REGISTRAR VENTAS.
    private void verificarPermisoRegistro() {
        if (!SesionUsuario.esAdministrador()
                && !SesionUsuario.esOperador()) {
            throw new SecurityException(
                    "No tiene permiso para registrar ventas.");
        }
    }

    // SOLO EL ADMINISTRADOR PUEDE MODIFICAR Y ANULAR.
    private void verificarPermisoAdministrador() {
        if (!SesionUsuario.esAdministrador()) {
            throw new SecurityException(
                    "Solo el administrador puede modificar o anular ventas.");
        }
    }

    // REGISTRAR VENTA.
    public void registrar(Venta venta) throws SQLException {
        verificarPermisoRegistro();
        validarVenta(venta);

        if (venta.getId() != null) {
            throw new IllegalArgumentException(
                    "Una venta nueva no debe tener un ID asignado.");
        }

        venta.setEstado("REGISTRADA");
        repository.registrar(venta);
    }

    // LISTAR TODAS LAS VENTAS.
    public List<Venta> listar() throws SQLException {
        verificarPermisoConsulta();
        return repository.listar();
    }

    // LISTAR VENTAS DE UNA PRODUCCION.
    public List<Venta> listarPorProduccion(Long produccionId)
            throws SQLException {
        verificarPermisoConsulta();
        validarId(produccionId, "Seleccione una produccion valida.");
        return repository.listarPorProduccion(produccionId);
    }

    // BUSCAR VENTA POR ID.
    public Venta buscarPorId(Long id) throws SQLException {
        verificarPermisoConsulta();
        validarId(id, "Seleccione una venta valida.");
        return repository.buscarPorId(id);
    }

    // MODIFICAR VENTA REGISTRADA.
    public void actualizar(Venta venta) throws SQLException {
        verificarPermisoAdministrador();

        if (venta == null) {
            throw new IllegalArgumentException(
                    "Seleccione una venta valida.");
        }
        validarId(venta.getId(), "Seleccione una venta valida.");
        validarVenta(venta);

        Venta existente = repository.buscarPorId(venta.getId());
        if (existente == null) {
            throw new IllegalArgumentException(
                    "La venta seleccionada no existe.");
        }
        if (!"REGISTRADA".equals(existente.getEstado())) {
            throw new IllegalArgumentException(
                    "No se puede modificar una venta anulada.");
        }
        if (!Objects.equals(
                existente.getProduccionId(), venta.getProduccionId())) {
            throw new IllegalArgumentException(
                    "No puede cambiar la produccion de una venta. "
                            + "Anule la venta y registre otra.");
        }

        venta.setEstado("REGISTRADA");
        repository.actualizar(venta);
    }

    // ANULAR VENTA SIN ELIMINARLA.
    public void anular(Long id) throws SQLException {
        verificarPermisoAdministrador();
        validarId(id, "Seleccione una venta valida.");

        Venta existente = repository.buscarPorId(id);
        if (existente == null) {
            throw new IllegalArgumentException(
                    "La venta seleccionada no existe.");
        }
        if (!"REGISTRADA".equals(existente.getEstado())) {
            throw new IllegalArgumentException(
                    "La venta ya esta anulada.");
        }

        repository.anular(id);
    }

    // STOCK DISPONIBLE DE UNA PRODUCCION (EN KG).
    public BigDecimal obtenerKgDisponibles(Long produccionId)
            throws SQLException {
        verificarPermisoConsulta();
        validarId(produccionId, "Seleccione una produccion valida.");
        return repository.obtenerKgDisponibles(produccionId);
    }

    // TOTAL DE KG VENDIDOS EN UNA PRODUCCION.
    public BigDecimal obtenerCantidadVendidaKg(Long produccionId)
            throws SQLException {
        verificarPermisoConsulta();
        validarId(produccionId, "Seleccione una produccion valida.");
        return repository.obtenerCantidadVendidaKg(produccionId);
    }

    // CALCULAR IMPORTE PARA LA VISTA Y PARA EL REGISTRO.
    public BigDecimal calcularTotal(BigDecimal cantidadKg,
                                   BigDecimal precioUnitarioKg) {
        if (cantidadKg == null
                || cantidadKg.compareTo(BigDecimal.ZERO) <= 0
                || cantidadKg.stripTrailingZeros().scale() > 3
                || cantidadKg.compareTo(new BigDecimal("999999999.999")) > 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser positiva y tener maximo tres decimales.");
        }
        if (precioUnitarioKg == null
                || precioUnitarioKg.compareTo(BigDecimal.ZERO) <= 0
                || precioUnitarioKg.stripTrailingZeros().scale() > 2
                || precioUnitarioKg.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "El precio debe ser positivo y tener maximo dos decimales.");
        }

        BigDecimal total = cantidadKg.multiply(precioUnitarioKg)
                .setScale(2, RoundingMode.HALF_UP);

        if (total.compareTo(BigDecimal.ZERO) <= 0
                || total.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "El total debe ser mayor a S/ 0.00 y estar dentro del rango permitido.");
        }
        return total;
    }

    // VALIDACIONES DE NEGOCIO (EL REPOSITORIO REVALIDA EL STOCK EN TRANSACCION).
    private void validarVenta(Venta venta) {
        if (venta == null) {
            throw new IllegalArgumentException(
                    "Los datos de la venta son obligatorios.");
        }

        validarId(venta.getProduccionId(),
                "Seleccione una produccion valida.");

        LocalDate fecha = venta.getFecha();
        if (fecha == null || fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Ingrese una fecha valida, no futura.");
        }

        String cliente = venta.getCliente();
        if (cliente == null || cliente.isBlank()
                || cliente.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "Ingrese un cliente de maximo 120 caracteres.");
        }
        venta.setCliente(cliente.trim());

        String observaciones = venta.getObservaciones();
        if (observaciones != null && observaciones.trim().length() > 255) {
            throw new IllegalArgumentException(
                    "Las observaciones no deben superar 255 caracteres.");
        }
        venta.setObservaciones(
                observaciones == null ? "" : observaciones.trim());

        // Nunca confiar en un total enviado por la interfaz.
        venta.setTotal(calcularTotal(
                venta.getCantidadKg(), venta.getPrecioUnitarioKg()));
    }

    private void validarId(Long id, String mensaje) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}
