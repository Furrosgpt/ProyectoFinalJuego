package com.atraparalagato.impl.strategy;

import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexPosition;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Implementación esqueleto de estrategia BFS (Breadth-First Search) para el gato.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Algoritmo BFS para pathfinding
 * - Exploración exhaustiva de caminos
 * - Garantía de encontrar el camino más corto
 * - Uso de colas para exploración por niveles
 */
public class BFSCatMovement extends CatMovementStrategy<HexPosition> {
    
    public BFSCatMovement(GameBoard<HexPosition> board) {
        super(board);
    }
    
    @Override
    public List<HexPosition> getPossibleMoves(HexPosition currentPosition) {
        // Obtener posiciones adyacentes que no estén bloqueadas
        return board.getAdjacentPositions(currentPosition).stream()
                .filter(pos -> !board.isBlocked(pos))
                .toList();
    }
    
    @Override
    protected Optional<HexPosition> selectBestMove(List<HexPosition> possibleMoves, 
                                                   HexPosition currentPosition, 
                                                   HexPosition targetPosition) {
        HexPosition bestMove = null;
        int bestPathLength = Integer.MAX_VALUE;
    
        for (HexPosition move : possibleMoves) {
            Optional<List<HexPosition>> pathOpt = bfsToGoal(move);
            if (pathOpt.isPresent()) {
                List<HexPosition> path = pathOpt.get();
                if (path.size() < bestPathLength) {
                    bestPathLength = path.size();
                    bestMove = move;
                }
            }
        }
        return Optional.ofNullable(bestMove);
    }    
    
    @Override
    protected Function<HexPosition, Double> getHeuristicFunction(HexPosition targetPosition) {
        // BFS no necesita heurística; retorna siempre 0.
        return pos -> 0.0;
    }    
    
    public Optional<HexPosition> getNextMove(HexPosition catPosition, HexPosition targetPosition) {
        List<HexPosition> possibleMoves = getPossibleMoves(catPosition);
        return selectBestMove(possibleMoves, catPosition, targetPosition);
    }
    
    @Override
    protected Predicate<HexPosition> getGoalPredicate() {
        // El objetivo es llegar al borde del tablero
        return position -> {
            int boardSize = board.getSize();
            return Math.abs(position.getQ()) >= boardSize ||
                   Math.abs(position.getR()) >= boardSize ||
                   Math.abs(position.getS()) >= boardSize;
        };
    }
    
    @Override
    protected double getMoveCost(HexPosition from, HexPosition to) {
        // Costo uniforme para movimientos adyacentes
        return 1.0;
    }
    
    @Override
    public boolean hasPathToGoal(HexPosition currentPosition) {
        Predicate<HexPosition> isGoal = getGoalPredicate();
        Queue<HexPosition> queue = new LinkedList<>();
        Set<HexPosition> visited = new HashSet<>();
    
        queue.add(currentPosition);
        visited.add(currentPosition);
    
        while (!queue.isEmpty()) {
            HexPosition current = queue.poll();
    
            if (isGoal.test(current)) {
                return true; // ¡Camino encontrado!
            }
    
            for (HexPosition neighbor : board.getAdjacentPositions(current)) {
                if (!board.isBlocked(neighbor) && !visited.contains(neighbor)) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                }
            }
        }
        // No se encontró camino al objetivo
        return false;
    }    
    
    @Override
    public List<HexPosition> getFullPath(HexPosition currentPosition, HexPosition targetPosition) {
        Queue<HexPosition> queue = new LinkedList<>();
        Map<HexPosition, HexPosition> parent = new HashMap<>();
        Set<HexPosition> visited = new HashSet<>();
    
        queue.add(currentPosition);
        visited.add(currentPosition);
        parent.put(currentPosition, null);
    
        while (!queue.isEmpty()) {
            HexPosition current = queue.poll();
    
            if (current.equals(targetPosition)) {
                // Reconstruir camino desde el objetivo hasta el inicio
                List<HexPosition> path = new LinkedList<>();
                HexPosition step = current;
                while (step != null) {
                    path.add(0, step); // Insertar al inicio (así queda ordenado)
                    step = parent.get(step);
                }
                return path;
            }
    
            for (HexPosition neighbor : board.getAdjacentPositions(current)) {
                if (!board.isBlocked(neighbor) && !visited.contains(neighbor)) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                }
            }
        }
        // Si no se encontró camino
        return Collections.emptyList();
    }    
    
    // Métodos auxiliares que los estudiantes pueden implementar
    
    /**
     * TODO: Ejecutar BFS desde una posición hasta encontrar objetivo.
     */
    private Optional<List<HexPosition>> bfsToGoal(HexPosition start) {
        Predicate<HexPosition> isGoal = getGoalPredicate();
        Queue<HexPosition> queue = new LinkedList<>();
        Map<HexPosition, HexPosition> parent = new HashMap<>();
        Set<HexPosition> visited = new HashSet<>();
        
        queue.add(start);
        visited.add(start);
        parent.put(start, null);
    
        while (!queue.isEmpty()) {
            HexPosition current = queue.poll();
    
            if (isGoal.test(current)) {
                // Reconstruye el camino desde start hasta current
                List<HexPosition> path = new LinkedList<>();
                HexPosition step = current;
                while (step != null) {
                    path.add(0, step);
                    step = parent.get(step);
                }
                return Optional.of(path);
            }
    
            for (HexPosition neighbor : board.getAdjacentPositions(current)) {
                if (!board.isBlocked(neighbor) && !visited.contains(neighbor)) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                }
            }
        }
        // No se encontró camino al objetivo
        return Optional.empty();
    }
    
    
    /**
     * TODO: Reconstruir camino desde mapa de padres.
     */
    private List<HexPosition> reconstructPath(Map<HexPosition, HexPosition> parentMap, 
                                            HexPosition start, HexPosition goal) {
        throw new UnsupportedOperationException("Método auxiliar para implementar");
    }
    
    /**
     * TODO: Evaluar calidad de un camino encontrado.
     */
    private double evaluatePathQuality(List<HexPosition> path) {
        throw new UnsupportedOperationException("Método auxiliar para implementar");
    }
} 