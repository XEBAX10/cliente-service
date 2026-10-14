package com.denkitronik.clienteservice.controllers;

import com.denkitronik.clienteservice.entities.Cliente;
import com.denkitronik.clienteservice.entities.Region;
import com.denkitronik.clienteservice.services.IClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/${api.version}/cliente-service")
@CrossOrigin(origins = { "http://localhost:4200" })
public class ClienteRestController {

    @Autowired
    private IClienteService clienteService;

    // ── GET /clientes ──
    @GetMapping("/clientes")
    public ResponseEntity<List<Cliente>> listarClientes() {
        List<Cliente> clientes = clienteService.findAll();
        return ResponseEntity.ok(clientes); // 200 OK
    }

    // ── GET /clientes/{id} ──
    @GetMapping("/clientes/{id}")
    public ResponseEntity<?> buscarCliente(@PathVariable Long id) {
        Optional<Cliente> cliente = clienteService.findById(id);
        if (cliente.isPresent()) {
            return ResponseEntity.ok(cliente.get()); // 200 OK
        }
        return ResponseEntity.notFound().build(); // 404 Not Found
    }

    // ── POST /clientes ──
    @PostMapping("/clientes")
    public ResponseEntity<Cliente> crearCliente(@RequestBody Cliente cliente) {
        Cliente nuevo = clienteService.save(cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo); // 201 Created
    }

    // ── PUT /clientes/{id} ──
    @PutMapping("/clientes/{id}")
    public ResponseEntity<Cliente> actualizarCliente(
            @PathVariable Long id, @RequestBody Cliente cliente) {
        if (clienteService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build(); // 404
        }
        Cliente actualizado = clienteService.update(id, cliente);
        return ResponseEntity.ok(actualizado); // 200 OK
    }

    // ── DELETE /clientes ──
    @DeleteMapping("/clientes")
    public ResponseEntity<Void> eliminarCliente(@RequestBody Cliente cliente) {
        clienteService.delete(cliente);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // ── GET /clientes/regiones ──
    @GetMapping("/clientes/regiones")
    public ResponseEntity<List<Region>> listarRegiones() {
        List<Region> regiones = clienteService.findAllRegiones();
        return ResponseEntity.ok(regiones);
    }
}