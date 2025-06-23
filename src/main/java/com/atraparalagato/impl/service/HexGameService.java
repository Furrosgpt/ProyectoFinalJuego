package com.atraparalagato.impl.service;

import com.atraparalagato.base.service.GameService;
import com.atraparalagato.base.model.GameState;
import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexPosition;
import com.atraparalagato.impl.strategy.AStarCatMovement;
import com.atraparalagato.impl.strategy.BFSCatMovement;
import com.atraparalagato.impl.model.HexGameState;
import com.atraparalagato.impl.model.HexGameBoard;
import com.atraparalagato.impl.repository.H2GameRepository;

import java.util.*;

/**
 * Implementación esqueleto de GameService para el juego hexagonal.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Orquestación de todos los componentes del juego
 * - Lógica de negocio compleja
 * - Manejo de eventos y callbacks
 * - Validaciones avanzadas
 * - Integración con repositorio y estrategias
 */
public class HexGameService extends GameService<HexPosition> {
    
    // Dependencias básicas para la implementación simplificada
    private final H2GameRepository repository;

    public HexGameService() {
        this(new H2GameRepository());
    }

    private HexGameService(H2GameRepository repo) {
        super(
            new HexGameBoard(5),
            new BFSCatMovement(new HexGameBoard(5)),
            (com.atraparalagato.base.repository.DataRepository<com.atraparalagato.base.model.GameState<HexPosition>, String>) (com.atraparalagato.base.repository.DataRepository<?>) repo,
            () -> UUID.randomUUID().toString(),
            HexGameBoard::new,
            (String id) -> new HexGameState(id, 5)
        );
        this.repository = repo;
    }
    
    /**
     * TODO: Crear un nuevo juego con configuración personalizada.
     * Debe ser más sofisticado que ExampleGameService.
     */
    public HexGameState createGame(int boardSize, String difficulty, Map<String, Object> options) {
        if (boardSize < 3 || boardSize > 10) {
            throw new IllegalArgumentException("El tamaño del tablero debe estar entre 3 y 10.");
        }
        if (difficulty == null ||
            !("easy".equalsIgnoreCase(difficulty) || "hard".equalsIgnoreCase(difficulty))) {
            throw new IllegalArgumentException("La dificultad debe ser 'easy' o 'hard'.");
        }
        if (options == null) {
            throw new IllegalArgumentException("Las opciones no pueden ser nulas.");
        }

        HexGameState gameState = new HexGameState(UUID.randomUUID().toString(), boardSize);
        // El constructor ya crea el tablero y coloca al gato en el centro

        repository.save(gameState);
        return gameState;
    }
    
    /**
     * TODO: Ejecutar movimiento del jugador con validaciones avanzadas.
     */
    public Optional<HexGameState> executePlayerMove(String gameId, HexPosition position, String playerId) {
        Optional<HexGameState> stateOpt = repository.findById(gameId);
        if (stateOpt.isEmpty()) {
            return Optional.empty();
        }

        HexGameState state = stateOpt.get();

        if (state.isGameFinished()) {
            return Optional.of(state);
        }

        if (!state.executeMove(position)) {
            return Optional.of(state);
        }

        // Mover el gato de forma simple utilizando BFS
        CatMovementStrategy<HexPosition> strategy = new BFSCatMovement(state.getGameBoard());
        strategy.findBestMove(state.getCatPosition(), getTargetPosition(state))
                .ifPresent(state::setCatPosition);

        repository.save(state);
        return Optional.of(state);
    }
    
    /**
     * TODO: Obtener estado del juego con información enriquecida.
     */
    public Optional<Map<String, Object>> getEnrichedGameState(String gameId) {
        Optional<HexGameState> stateOpt = repository.findById(gameId);
        if (stateOpt.isEmpty()) {
            return Optional.empty();
        }

        HexGameState state = stateOpt.get();
        Map<String, Object> map = new HashMap<>();
        map.put("gameId", state.getGameId());
        map.put("status", state.getStatus().toString());
        map.put("catPosition", Map.of("q", state.getCatPosition().getQ(), "r", state.getCatPosition().getR()));
        map.put("blockedCells", state.getGameBoard().getBlockedPositions());
        map.put("movesCount", state.getMoveCount());
        map.put("boardSize", state.getBoardSize());
        return Optional.of(map);
    }
    
    /**
     * TODO: Obtener sugerencia inteligente de movimiento.
     */
    public Optional<HexPosition> getIntelligentSuggestion(String gameId, String difficulty) {
        // Estrategia simple: usar la estrategia de movimiento configurada para
        // calcular la posición más cercana al gato que no esté bloqueada.
        return getSuggestedMove(gameId);
    }
    
    /**
     * TODO: Analizar la partida y generar reporte.
     */
    public Map<String, Object> analyzeGame(String gameId) {
        return repository.findById(gameId)
                .map(gs -> {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("gameId", gs.getGameId());
                    stats.put("status", gs.getStatus().toString());
                    stats.put("moveCount", gs.getMoveCount());
                    stats.put("score", gs.calculateScore());
                    stats.put("boardSize", gs.getBoardSize());
                    stats.put("catPosition", Map.of("q", gs.getCatPosition().getQ(), "r", gs.getCatPosition().getR()));
                    return stats;
                })
                .orElse(Map.of("error", "Game not found"));
    }
    
    /**
     * TODO: Obtener estadísticas globales del jugador.
     */
    public Map<String, Object> getPlayerStatistics(String playerId) {
        List<HexGameState> all = repository.findAll();
        int total = all.size();
        long won = all.stream().filter(HexGameState::hasPlayerWon).count();
        double avgScore = all.stream().mapToInt(HexGameState::calculateScore).average().orElse(0);

        Map<String, Object> map = new HashMap<>();
        map.put("playerId", playerId);
        map.put("gamesPlayed", total);
        map.put("winRate", total == 0 ? 0 : (double) won / total);
        map.put("averageScore", avgScore);
        return map;
    }
    
    /**
     * TODO: Configurar dificultad del juego.
     */
    public void setGameDifficulty(String gameId, String difficulty) {
        // Implementación simplificada: solo imprime el cambio solicitado
        System.out.println("Cambio de dificultad para " + gameId + " a " + difficulty);
    }
    
    /**
     * TODO: Pausar/reanudar juego.
     */
    public boolean toggleGamePause(String gameId) {
        // No se mantiene estado de pausa. Retorna false indicando que no se soporta.
        return false;
    }
    
    /**
     * TODO: Deshacer último movimiento.
     */
    public Optional<HexGameState> undoLastMove(String gameId) {
        // Funcionalidad no soportada en esta implementación
        return Optional.empty();
    }
    
    /**
     * TODO: Obtener ranking de mejores puntuaciones.
     */
    public List<Map<String, Object>> getLeaderboard(int limit) {
        return repository.findAll().stream()
                .sorted(Comparator.comparingInt(HexGameState::calculateScore).reversed())
                .limit(limit)
                .map(gs -> Map.of(
                        "gameId", gs.getGameId(),
                        "score", gs.calculateScore(),
                        "moves", gs.getMoveCount(),
                        "boardSize", gs.getBoardSize()))
                .toList();
    }
    
    // Métodos auxiliares que los estudiantes pueden implementar
    
    /**
     * TODO: Validar movimiento según reglas avanzadas.
     */
    private boolean isValidAdvancedMove(HexGameState gameState, HexPosition position, String playerId) {
        return gameState.getGameBoard().isValidMove(position);
    }
    
    /**
     * TODO: Ejecutar movimiento del gato usando estrategia apropiada.
     */
    private void executeCatMove(HexGameState gameState, String difficulty) {
        CatMovementStrategy<HexPosition> strategy = createMovementStrategy(difficulty, gameState.getGameBoard());
        strategy.findBestMove(gameState.getCatPosition(), getTargetPosition(gameState))
                .ifPresent(gameState::setCatPosition);
    }
    
    /**
     * TODO: Calcular puntuación avanzada.
     */
    private int calculateAdvancedScore(HexGameState gameState, Map<String, Object> factors) {
        return gameState.calculateScore();
    }
    
    /**
     * TODO: Notificar eventos del juego.
     */
    private void notifyGameEvent(String gameId, String eventType, Map<String, Object> eventData) {
        System.out.println("[EVENT] " + eventType + " for " + gameId + " -> " + eventData);
    }
    
    /**
     * TODO: Crear factory de estrategias según dificultad.
     */
    private CatMovementStrategy createMovementStrategy(String difficulty, HexGameBoard board) {
        if ("hard".equalsIgnoreCase(difficulty)) {
            return new AStarCatMovement(board);
        }
        return new BFSCatMovement(board);
    }

    // Métodos abstractos requeridos por GameService

    @Override
    protected void initializeGame(GameState<HexPosition> gameState, GameBoard<HexPosition> gameBoard) {
        if (gameState instanceof HexGameState hexState) {
            hexState.setCatPosition(new HexPosition(0, 0));
        }
    }

    @Override
    public boolean isValidMove(String gameId, HexPosition position) {
        return repository.findById(gameId)
                .map(gs -> gs.getGameBoard().isValidMove(position))
                .orElse(false);
    }

    @Override
    public Optional<HexPosition> getSuggestedMove(String gameId) {
        return repository.findById(gameId)
                .flatMap(gs -> gs.getGameBoard()
                        .getAdjacentPositions(gs.getCatPosition())
                        .stream()
                        .filter(pos -> !gs.getGameBoard().isBlocked(pos))
                        .findFirst());
    }

    @Override
    protected HexPosition getTargetPosition(GameState<HexPosition> gameState) {
        if (gameState instanceof HexGameState hexState) {
            int size = hexState.getBoardSize();
            return new HexPosition(size, 0);
        }
        return new HexPosition(0, 0);
    }

    @Override
    public Object getGameStatistics(String gameId) {
        return repository.findById(gameId)
                .map(gs -> Map.of(
                        "gameId", gs.getGameId(),
                        "status", gs.getStatus().toString(),
                        "moveCount", gs.getMoveCount(),
                        "boardSize", gs.getBoardSize()))
                .orElse(Map.of("error", "Game not found"));
    }
}
