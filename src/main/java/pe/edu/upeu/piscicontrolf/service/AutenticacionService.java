
package pe.edu.upeu.piscicontrolf.service;

import pe.edu.upeu.piscicontrolf.model.Usuario;
import pe.edu.upeu.piscicontrolf.repository.UsuarioRepository;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;

public class AutenticacionService {

    private final UsuarioRepository repository =
            new UsuarioRepository();

    private static final int ITERACIONES = 600000;
    private static final int LONGITUD_SALT = 16;
    private static final int LONGITUD_HASH = 256;

    // INICIAR SESION
    public Usuario iniciarSesion(
            String nombreUsuario,
            String contrasena) throws SQLException {

        if (nombreUsuario == null ||
                nombreUsuario.isBlank() ||
                contrasena == null ||
                contrasena.isBlank()) {

            throw new IllegalArgumentException(
                    "Ingrese su usuario y contraseña.");
        }

        Usuario encontrado = repository.buscarPorUsuario(
                nombreUsuario.trim());

        if (encontrado == null ||
                !"ACTIVO".equalsIgnoreCase(
                        encontrado.getEstado()) ||
                !verificarContrasena(
                        contrasena,
                        encontrado.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "Credenciales incorrectas o cuenta inactiva.");
        }

        String rol = encontrado.getRol();

        if (rol == null ||
                (!rol.equalsIgnoreCase("ADMINISTRADOR") &&
                        !rol.equalsIgnoreCase("OPERADOR"))) {

            throw new IllegalArgumentException(
                    "El usuario no tiene un rol autorizado.");
        }

        // DEVOLVER DATOS SIN EXPONER EL HASH
        return new Usuario(
                encontrado.getId(),
                encontrado.getNombre(),
                encontrado.getUsuario(),
                null,
                rol.toUpperCase(java.util.Locale.ROOT),
                encontrado.getEstado()
        );
    }

    // GENERAR HASH SEGURO PARA NUEVAS CUENTAS
    public static String generarHash(String contrasena) {

        if (contrasena == null ||
                contrasena.length() < 8) {

            throw new IllegalArgumentException(
                    "La contraseña debe tener mínimo 8 caracteres.");
        }

        byte[] salt = new byte[LONGITUD_SALT];

        new SecureRandom().nextBytes(salt);

        char[] clave = contrasena.toCharArray();

        try {
            byte[] hash = derivarClave(
                    clave, salt, ITERACIONES);

            return "PBKDF2-SHA256$" +
                    ITERACIONES + "$" +
                    Base64.getEncoder().encodeToString(salt) +
                    "$" +
                    Base64.getEncoder().encodeToString(hash);

        } finally {
            Arrays.fill(clave, '\0');
        }
    }

    // VERIFICAR CONTRASEÑA
    private static boolean verificarContrasena(
            String contrasena,
            String hashGuardado) {

        if (hashGuardado == null) {
            return false;
        }

        try {
            String[] partes = hashGuardado.split("\\$");

            if (partes.length != 4 ||
                    !partes[0].equals("PBKDF2-SHA256")) {
                return false;
            }

            int iteraciones = Integer.parseInt(partes[1]);

            if (iteraciones < 100000 ||
                    iteraciones > 2000000) {
                return false;
            }

            byte[] salt =
                    Base64.getDecoder().decode(partes[2]);

            byte[] hashEsperado =
                    Base64.getDecoder().decode(partes[3]);

            if (salt.length != LONGITUD_SALT ||
                    hashEsperado.length != LONGITUD_HASH / 8) {
                return false;
            }

            char[] clave = contrasena.toCharArray();

            try {
                byte[] hashCalculado =
                        derivarClave(clave, salt, iteraciones);

                return MessageDigest.isEqual(
                        hashEsperado,
                        hashCalculado
                );

            } finally {
                Arrays.fill(clave, '\0');
            }

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ALGORITMO PBKDF2 CON SHA-256
    private static byte[] derivarClave(
            char[] clave,
            byte[] salt,
            int iteraciones) {

        PBEKeySpec especificacion = new PBEKeySpec(
                clave,
                salt,
                iteraciones,
                LONGITUD_HASH
        );

        try {
            SecretKeyFactory fabrica =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256");

            return fabrica.generateSecret(
                    especificacion).getEncoded();

        } catch (GeneralSecurityException e) {

            throw new IllegalStateException(
                    "No se pudo procesar la contraseña.", e);

        } finally {
            especificacion.clearPassword();
        }
    }
}
