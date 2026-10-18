package com.denkitronik.clienteservice.domain.services;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import com.denkitronik.clienteservice.domain.repositories.IClienteDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests — ClienteServiceImpl con Mockito")
class ClienteServiceImplTest {

    @Mock
    private IClienteDao clienteDao; // mock: no toca la BD

    @InjectMocks
    private ClienteServiceImpl clienteService; // instancia real con el mock inyectado

    private Cliente cliente;
    private Region region;

    @BeforeEach
    void setUp() {
        // Arrange compartido: datos de prueba reutilizados en todos los tests
        region = new Region();
        region.setId(1L);
        region.setNombre("Sudamérica");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Linus");
        cliente.setApellido("Torvalds");
        cliente.setEmail("linus@kernel.org");
        cliente.setRegion(region);
    }

    // ── findAll ─────────────────────────────────────────────

    @Test
    @DisplayName("findAll(pageable) → devuelve la página del DAO")
    void findAll_conPaginacion_debeRetornarPagina() {
        Pageable pageable = PageRequest.of(0, 4);
        Page<Cliente> pagina = new PageImpl<>(List.of(cliente), pageable, 1);
        when(clienteDao.findAll(pageable)).thenReturn(pagina);

        Page<Cliente> resultado = clienteService.findAll(pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findAll() → devuelve la lista completa")
    void findAll_sinPaginacion_debeRetornarLista() {
        when(clienteDao.findAll()).thenReturn(List.of(cliente));

        List<Cliente> resultado = clienteService.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Linus");
    }

    // ── findById ────────────────────────────────────────────

    @Test
    @DisplayName("findById — ID existente → devuelve el cliente")
    void findById_idExistente_debeRetornarCliente() {
        // Arrange: programamos el mock
        when(clienteDao.findById(1L)).thenReturn(Optional.of(cliente));

        // Act: llamamos al método real del servicio
        Cliente resultado = clienteService.findById(1L);

        // Assert: verificamos el resultado
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Linus");
        verify(clienteDao, times(1)).findById(1L);
    }

    @Test
    @DisplayName("findById — ID inexistente → lanza ClienteNotFoundException")
    void findById_idInexistente_debeLanzarExcepcion() {
        when(clienteDao.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.findById(999L))
                .isInstanceOf(ClienteNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── save ────────────────────────────────────────────────

    @Test
    @DisplayName("save — datos válidos → guarda y retorna el cliente")
    void save_datosValidos_debeRetornarCliente() {
        when(clienteDao.save(cliente)).thenReturn(cliente);

        Cliente resultado = clienteService.save(cliente);

        assertThat(resultado.getEmail()).isEqualTo("linus@kernel.org");
        verify(clienteDao, times(1)).save(cliente);
    }

    @Test
    @DisplayName("save — email duplicado → lanza ClienteServiceException")
    void save_emailDuplicado_debeLanzarClienteServiceException() {
        // Arrange: simulamos que la BD rechaza por email duplicado
        when(clienteDao.save(cliente))
                .thenThrow(new DataIntegrityViolationException("duplicate key value"));

        // Act + Assert
        assertThatThrownBy(() -> clienteService.save(cliente))
                .isInstanceOf(ClienteServiceException.class)
                .hasMessageContaining("duplicados");
    }

    // ── update ──────────────────────────────────────────────

    @Test
    @DisplayName("update — ID existente → actualiza y retorna cliente modificado")
    void update_idExistente_debeActualizarYRetornar() {
        Cliente datosNuevos = new Cliente();
        datosNuevos.setNombre("Linus Updated");
        datosNuevos.setApellido("Torvalds");
        datosNuevos.setEmail("linus2@kernel.org");
        datosNuevos.setRegion(region);

        when(clienteDao.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteDao.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente resultado = clienteService.update(1L, datosNuevos);

        assertThat(resultado.getNombre()).isEqualTo("Linus Updated");
        assertThat(resultado.getEmail()).isEqualTo("linus2@kernel.org");
        verify(clienteDao, times(1)).findById(1L);
        verify(clienteDao, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("update — ID inexistente → lanza ClienteNotFoundException")
    void update_idInexistente_debeLanzarExcepcion() {
        when(clienteDao.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.update(999L, cliente))
                .isInstanceOf(ClienteNotFoundException.class)
                .hasMessageContaining("999");

        verify(clienteDao, never()).save(any(Cliente.class));
    }

    // ── delete ──────────────────────────────────────────────

    @Test
    @DisplayName("delete — ID existente → llama deleteById")
    void delete_idExistente_debeEliminar() {
        when(clienteDao.existsById(1L)).thenReturn(true);

        clienteService.delete(1L);

        verify(clienteDao, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("delete — ID inexistente → lanza excepción sin llamar deleteById")
    void delete_idInexistente_noDebeEliminar() {
        when(clienteDao.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> clienteService.delete(999L))
                .isInstanceOf(ClienteNotFoundException.class);

        // deleteById NUNCA debe llamarse
        verify(clienteDao, never()).deleteById(any());
    }

    // ── findAllRegiones ─────────────────────────────────────

    @Test
    @DisplayName("findAllRegiones → devuelve las regiones del DAO")
    void findAllRegiones_debeRetornarLista() {
        when(clienteDao.findAllRegiones()).thenReturn(List.of(region));

        List<Region> resultado = clienteService.findAllRegiones();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Sudamérica");
    }
}