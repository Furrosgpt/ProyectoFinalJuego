package com.atraparalagato.impl.repository;

import com.atraparalagato.base.repository.DataRepository;
import com.atraparalagato.impl.model.HexGameState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.Comparator;
import java.sql.Connection;
import java.sql.Statement;


/**
 * Implementación esqueleto de DataRepository usando base de datos H2.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Conexión a base de datos H2
 * - Operaciones CRUD con SQL
 * - Manejo de transacciones
 * - Mapeo objeto-relacional
 * - Consultas personalizadas
 * - Manejo de errores de BD
 */
public class H2GameRepository extends DataRepository<HexGameState, String> {
    
    // TODO: Los estudiantes deben definir la configuración de la base de datos
    // Ejemplos: DataSource, JdbcTemplate, EntityManager, etc.

    private static final String DB_URL = "jdbc:h2:mem:atraparalagato_db;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Connection connection;
    
    public H2GameRepository() {
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            createSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Error inicializando base de datos H2 en memoria", e);
        }
    }

    @Override
    public HexGameState save(HexGameState entity) {
        if (entity == null || entity.getGameId() == null) {
            throw new IllegalArgumentException("Entidad o ID no puede ser nulo");
        }

        // Llamar hook antes de guardar (si tienes definido)
        beforeSave(entity);

        String serializedData = serializeGameState(entity);

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            // Primero verificar si existe registro con ese ID
            String checkSql = "SELECT COUNT(*) FROM game_state WHERE id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, entity.getGameId());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    boolean exists = false;
                    if (rs.next()) {
                        exists = rs.getInt(1) > 0;
                    }

                    if (exists) {
                        // Hacer UPDATE
                        String updateSql = "UPDATE game_state SET state_data = ? WHERE id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, serializedData);
                            updateStmt.setString(2, entity.getGameId());
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Hacer INSERT
                        String insertSql = "INSERT INTO game_state (id, state_data) VALUES (?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, entity.getGameId());
                            insertStmt.setString(2, serializedData);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error guardando HexGameState en BD", e);
        }

        // Llamar hook después de guardar (si tienes definido)
        afterSave(entity);

        return entity;
    }

    @Override
    public Optional<HexGameState> findById(String id) {
        String sql = "SELECT state_data FROM game_state WHERE id = ?";
    
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
    
            ps.setString(1, id);
    
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String serializedData = rs.getString("state_data");
                    HexGameState gameState = deserializeGameState(serializedData, id);
                    return Optional.of(gameState);
                } else {
                    return Optional.empty();
                }
            }
    
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando HexGameState por ID", e);
        }
    }
    
    
    @Override
    public List<HexGameState> findAll() {
        String sql = "SELECT id, state_data FROM game_state";
        List<HexGameState> results = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String serializedData = rs.getString("state_data");
                HexGameState gameState = deserializeGameState(serializedData, id);
                results.add(gameState);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo todos los HexGameState", e);
        }

        return results;
    }

    
    @Override
    public List<HexGameState> findWhere(Predicate<HexGameState> condition) {
        List<HexGameState> allStates = findAll();
        List<HexGameState> filtered = new ArrayList<>();
    
        for (HexGameState state : allStates) {
            if (condition.test(state)) {
                filtered.add(state);
            }
        }
    
        return filtered;
    }    
    
    @Override
    public <R> List<R> findAndTransform(Predicate<HexGameState> condition, Function<HexGameState, R> transformer) {
        List<HexGameState> allStates = findAll();
        List<R> transformed = new ArrayList<>();
    
        for (HexGameState state : allStates) {
            if (condition.test(state)) {
                transformed.add(transformer.apply(state));
            }
        }
    
        return transformed;
    }    
    
    @Override
    public long countWhere(Predicate<HexGameState> condition) {
        List<HexGameState> allStates = findAll();
        return allStates.stream()
                        .filter(condition)
                        .count();
    }    
    
    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM game_state WHERE id = ?";
    
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
    
            ps.setString(1, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
    
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando HexGameState por ID", e);
        }
    }    
    
    @Override
    public long deleteWhere(Predicate<HexGameState> condition) {
        List<HexGameState> allStates = findAll();
        long deletedCount = 0;
    
        for (HexGameState state : allStates) {
            if (condition.test(state)) {
                boolean deleted = deleteById(state.getGameId()); // Usa getGameId() para obtener ID
                if (deleted) {
                    deletedCount++;
                }
            }
        }
        return deletedCount;
    }    
    
    @Override
    public boolean existsById(String id) {
        String sql = "SELECT COUNT(*) FROM game_state WHERE id = ?";
    
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
    
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Error verificando existencia de HexGameState por ID", e);
        }
    }    
    
    @Override
    public <R> R executeInTransaction(Function<DataRepository<HexGameState, String>, R> operation) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);
            try {
                // Ejecutar la operación
                R result = operation.apply(this);
    
                // Confirmar cambios
                conn.commit();
                return result;
            } catch (Exception ex) {
                // En caso de error, revertir
                conn.rollback();
                throw ex;
            } finally {
                // Restaurar auto commit por seguridad
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error ejecutando transacción en la base de datos", e);
        }
    }    
    
    @Override
    public List<HexGameState> findWithPagination(int page, int size) {
        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("Página y tamaño deben ser mayores que 0");
        }
    
        int offset = (page - 1) * size;
        String sql = "SELECT id, state_data FROM game_state ORDER BY id LIMIT ? OFFSET ?";
        List<HexGameState> results = new ArrayList<>();
    
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
    
            ps.setInt(1, size);
            ps.setInt(2, offset);
    
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String serializedData = rs.getString("state_data");
                    HexGameState gameState = deserializeGameState(serializedData, id);
                    results.add(gameState);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo HexGameState con paginación", e);
        }
    
        return results;
    }    
    
    @SuppressWarnings("unchecked")
    @Override
    public List<HexGameState> findAllSorted(Function<HexGameState, ? extends Comparable<?>> sortKeyExtractor, boolean ascending) {
        List<HexGameState> allStates = findAll();
    
        // Cast explícito para evitar error de tipos
        Function<HexGameState, Comparable> keyExtractor = (Function<HexGameState, Comparable>) (Function<?, ?>) sortKeyExtractor;
    
        Comparator<HexGameState> comparator = Comparator.comparing(keyExtractor);
    
        if (!ascending) {
            comparator = comparator.reversed();
        }
    
        allStates.sort(comparator);
        return allStates;
    }    
    
    @Override
    public <R> List<R> executeCustomQuery(String query, Function<Object, R> resultMapper) {
        List<R> results = new ArrayList<>();
    
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
    
            while (rs.next()) {
                // Adaptamos el ResultSet para pasarlo como Object al mapper
                R obj = resultMapper.apply((Object) rs);
                results.add(obj);
            }
    
        } catch (SQLException e) {
            throw new RuntimeException("Error ejecutando consulta personalizada", e);
        }
    
        return results;
    }    
    
    @Override
    protected void initialize() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
    
            // 1. Crear tabla game_state si no existe
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS game_state (
                    id VARCHAR(255) PRIMARY KEY,
                    state_data CLOB NOT NULL
                );
            """;
            stmt.execute(createTableSql);
    
            // 2. Crear índices si necesitas (opcional)
            // Ejemplo: índice sobre id para acelerar búsquedas (ya es PK, así que no es necesario)
    
            // 3. Insertar datos de prueba si quieres (opcional)
            // String insertTestSql = "INSERT INTO game_state (id, state_data) VALUES ('test', '{...}')";
            // stmt.execute(insertTestSql);
    
        } catch (SQLException e) {
            throw new RuntimeException("Error inicializando base de datos", e);
        }
    }    
    
    @Override
    protected void cleanup() {
        // Si usas conexión por campo, ciérrala aquí
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                // Puedes loguear el error o lanzar runtime
                System.err.println("Error cerrando conexión: " + e.getMessage());
            }
        }
    
        // Si tienes cachés o pools, límpialos aquí
    
        // Otros recursos que debas liberar
    }    
    
    // Métodos auxiliares que los estudiantes pueden implementar
    
    /**
     * TODO: Crear el esquema de la base de datos.
     * Definir tablas, columnas, tipos de datos, restricciones.
     */
    private void createSchema() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS game_state (
                id VARCHAR(255) PRIMARY KEY,
                state_data CLOB NOT NULL
            );
        """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
    

    private String serializeGameState(HexGameState gameState) {
        try {
            return objectMapper.writeValueAsString(gameState);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando HexGameState", e);
        }
    }

    private HexGameState deserializeGameState(String serializedData, String gameId) {
        try {
            return objectMapper.readValue(serializedData, HexGameState.class);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializando HexGameState", e);
        }
    }
    
    
    /**
     * TODO: Convertir Predicate a cláusula WHERE SQL.
     * Implementación avanzada opcional.
     */
    private String predicateToSql(Predicate<HexGameState> predicate) {
        throw new UnsupportedOperationException("Método auxiliar avanzado para implementar");
    }
} 