package com.denkitronik.clienteservice.integration;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate; // Spring Boot 4.x
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate; // Spring Boot 4.x
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer; // Testcontainers 2.x

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate // Spring Boot 4.x: requerido para inyectar TestRestTemplate
@Testcontainers(disabledWithoutDocker = true) // si no hay Docker, se salta en vez de fallar
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E tests — flujo completo HTTP → PostgreSQL con Testcontainers")
class ClienteIntegrationTest {

    // Testcontainers levanta PostgreSQL real antes de los tests
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate; // para sembrar datos iniciales

    private static final String BASE = "/api/v1/cliente-service";
    private static final AtomicBoolean seeded = new AtomicBoolean(false);

    private static Long regionId;  // compartido entre tests
    private static Long idCreado;  // compartido entre tests

    @BeforeEach
    void seedDatosIniciales() {
        // Se ejecuta antes de cada test, pero solo siembra datos la primera vez
        if (seeded.compareAndSet(false, true)) {
            jdbcTemplate.execute("INSERT INTO regiones(nombre) VALUES ('América del Sur')");
            regionId = jdbcTemplate.queryForObject(
                    "SELECT id FROM regiones WHERE nombre = 'América del Sur'", Long.class);
        }
    }

    @Test
    @Order(1)
    @DisplayName("POST /clientes → 201 crea el cliente y retorna el ID")
    void crearCliente_debeRetornar201() {
        Region region = new Region();
        region.setId(regionId);

        Cliente nuevo = new Cliente();
        nuevo.setNombre("Margaret");
        nuevo.setApellido("Hamilton");
        nuevo.setEmail("margaret@apollo.nasa");
        nuevo.setRegion(region);

        ResponseEntity<Cliente> respuesta =
                restTemplate.postForEntity(BASE + "/clientes", nuevo, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().getId()).isNotNull();
        assertThat(respuesta.getBody().getNombre()).isEqualTo("Margaret");

        idCreado = respuesta.getBody().getId(); // lo guardamos para los tests siguientes
    }

    @Test
    @Order(2)
    @DisplayName("GET /clientes/{id} → 200 encuentra el cliente creado")
    void buscarClienteCreado_debeRetornar200() {
        ResponseEntity<Cliente> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/" + idCreado, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getEmail()).isEqualTo("margaret@apollo.nasa");
    }

    @Test
    @Order(3)
    @DisplayName("GET /clientes → 200 la lista contiene el cliente creado")
    void listarClientes_debeContenerClienteCreado() {
        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(BASE + "/clientes", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).contains("margaret@apollo.nasa");
    }

    @Test
    @Order(4)
    @DisplayName("GET /clientes/page/0 → 200 con content como lista (reto adicional)")
    void listarPaginado_debeRetornarContentComoLista() {
        ResponseEntity<Map> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/page/0", Map.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().get("content")).isInstanceOf(List.class);
    }

    @Test
    @Order(5)
    @DisplayName("PUT /clientes/{id} → 201 actualiza los datos")
    void actualizarCliente_debeRetornar201() {
        Region region = new Region();
        region.setId(regionId);

        Cliente actualizado = new Cliente();
        actualizado.setNombre("Margaret");
        actualizado.setApellido("Hamilton-Updated");
        actualizado.setEmail("margaret@apollo.nasa");
        actualizado.setRegion(region);

        HttpEntity<Cliente> entity = new HttpEntity<>(actualizado, headersJson());

        ResponseEntity<Cliente> respuesta = restTemplate.exchange(
                BASE + "/clientes/" + idCreado, HttpMethod.PUT, entity, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().getApellido()).isEqualTo("Hamilton-Updated");
    }

    @Test
    @Order(6)
    @DisplayName("GET /clientes/9999 → 404 ID inexistente")
    void idInexistente_debeRetornar404() {
        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/9999", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(7)
    @DisplayName("DELETE /clientes/{id} → 204 y luego GET confirma 404")
    void eliminarCliente_debeRetornar204YLuego404() {
        restTemplate.delete(BASE + "/clientes/" + idCreado);

        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/" + idCreado, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private HttpHeaders headersJson() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}