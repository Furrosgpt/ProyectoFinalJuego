package com.atraparalagato.impl.strategy;

import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexPosition;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Implementación esqueleto de estrategia de movimiento usando algoritmo A*.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Algoritmos: A* pathfinding
 * - Programación Funcional: Function, Predicate
 * - Estructuras de Datos: PriorityQueue, Map, Set
 */
public class AStarCatMovement extends CatMovementStrategy<HexPosition> {
    
    public AStarCatMovement(GameBoard<HexPosition> board) {
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
        double bestCost = Double.POSITIVE_INFINITY;
    
        for (HexPosition move : possibleMoves) {
            double cost = aStarCost(move, targetPosition);
            if (cost < bestCost) {
                bestCost = cost;
                bestMove = move;
            }
        }
        return Optional.ofNullable(bestMove);
    }

    public Optional<HexPosition> getNextMove(HexPosition catPosition, HexPosition targetPosition) {
        List<HexPosition> possibleMoves = getPossibleMoves(catPosition);
        return selectBestMove(possibleMoves, catPosition, targetPosition);
    }
    
    
    // Implementación de A*
    private double aStarCost(HexPosition start, HexPosition goal) {
        // Heurística
        Function<HexPosition, Double> heuristic = getHeuristicFunction(goal);

        // Estructuras para A*
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<HexPosition, Double> gScore = new HashMap<>();
        Set<HexPosition> closedSet = new HashSet<>();

        gScore.put(start, 0.0);
        openSet.add(new AStarNode(start, 0.0, heuristic.apply(start), null));

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();

            if (current.position.equals(goal)) {
                // Llegaste al objetivo, retorna el costo total
                return current.gScore;
            }

            closedSet.add(current.position);

            for (HexPosition neighbor : board.getAdjacentPositions(current.position)) {
                if (closedSet.contains(neighbor) || board.isBlocked(neighbor)) continue;

                double tentativeG = current.gScore + getMoveCost(current.position, neighbor);
                if (tentativeG < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    gScore.put(neighbor, tentativeG);
                    double fScore = tentativeG + heuristic.apply(neighbor);
                    openSet.add(new AStarNode(neighbor, tentativeG, fScore, current));
                }
            }
        }
        return Double.POSITIVE_INFINITY; // No hay camino
    }
    
    @Override
    protected Function<HexPosition, Double> getHeuristicFunction(HexPosition targetPosition) {
        // Heurística simple: distancia al borde más cercano
        return position -> {
            int boardSize = board.getSize();
            
            // Calcular distancia mínima al borde
            double distanceToBorder = Math.min(
                Math.min(boardSize - Math.abs(position.getQ()),
                        boardSize - Math.abs(position.getR())),
                boardSize - Math.abs(position.getS())
            );
            
            // Invertir para que menor distancia al borde = mejor puntuación
            return boardSize - distanceToBorder;
        };
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
        Function<HexPosition, Double> heuristic = pos -> 0.0; // Para solo búsqueda, heurística 0
        Predicate<HexPosition> isGoal = getGoalPredicate();
    
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Set<HexPosition> closedSet = new HashSet<>();
        openSet.add(new AStarNode(currentPosition, 0.0, heuristic.apply(currentPosition), null));
    
        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
    
            if (isGoal.test(current.position)) {
                return true; // Encontró camino al objetivo
            }
    
            closedSet.add(current.position);
    
            for (HexPosition neighbor : board.getAdjacentPositions(current.position)) {
                if (closedSet.contains(neighbor) || board.isBlocked(neighbor)) continue;
    
                double tentativeG = current.gScore + getMoveCost(current.position, neighbor);
    
                // Solo agrega si no está ya en la cola con mejor score
                boolean alreadyInOpen = openSet.stream().anyMatch(n -> n.position.equals(neighbor) && n.gScore <= tentativeG);
                if (!alreadyInOpen) {
                    double fScore = tentativeG + heuristic.apply(neighbor);
                    openSet.add(new AStarNode(neighbor, tentativeG, fScore, current));
                }
            }
        }
        return false; // No hay camino al objetivo
    }
    
    
    @Override
    public List<HexPosition> getFullPath(HexPosition currentPosition, HexPosition targetPosition) {
        Function<HexPosition, Double> heuristic = getHeuristicFunction(targetPosition);
    
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<HexPosition, Double> gScore = new HashMap<>();
        Map<HexPosition, AStarNode> nodeMap = new HashMap<>();
        Set<HexPosition> closedSet = new HashSet<>();
    
        gScore.put(currentPosition, 0.0);
        AStarNode startNode = new AStarNode(currentPosition, 0.0, heuristic.apply(currentPosition), null);
        openSet.add(startNode);
        nodeMap.put(currentPosition, startNode);
    
        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
    
            if (current.position.equals(targetPosition)) {
                // ¡Objetivo encontrado! Reconstruye el camino y retorna
                return reconstructPath(current);
            }
    
            closedSet.add(current.position);
    
            for (HexPosition neighbor : board.getAdjacentPositions(current.position)) {
                if (closedSet.contains(neighbor) || board.isBlocked(neighbor)) continue;
    
                double tentativeG = current.gScore + getMoveCost(current.position, neighbor);
                if (tentativeG < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    gScore.put(neighbor, tentativeG);
                    double fScore = tentativeG + heuristic.apply(neighbor);
                    AStarNode neighborNode = new AStarNode(neighbor, tentativeG, fScore, current);
                    openSet.add(neighborNode);
                    nodeMap.put(neighbor, neighborNode);
                }
            }
        }
        // Si no se encontró camino
        return Collections.emptyList();
    }
    
    
    // Clase auxiliar para nodos del algoritmo A*
    private static class AStarNode {
        public final HexPosition position;
        public final double gScore; // Costo desde inicio
        public final double fScore; // gScore + heurística
        public final AStarNode parent;
        
        public AStarNode(HexPosition position, double gScore, double fScore, AStarNode parent) {
            this.position = position;
            this.gScore = gScore;
            this.fScore = fScore;
            this.parent = parent;
        }
    }
    
    private List<HexPosition> reconstructPath(AStarNode goalNode) {
        List<HexPosition> path = new LinkedList<>();
        AStarNode current = goalNode;
        while (current != null) {
            path.add(0, current.position); // Inserta al inicio para invertir el orden
            current = current.parent;
        }
        return path;
    }    
    
    // Hook methods - los estudiantes pueden override para debugging
    @Override
    protected void beforeMovementCalculation(HexPosition currentPosition) {
        // TODO: Opcional - logging, métricas, etc.
        super.beforeMovementCalculation(currentPosition);
    }
    
    @Override
    protected void afterMovementCalculation(Optional<HexPosition> selectedMove) {
        // TODO: Opcional - logging, métricas, etc.
        super.afterMovementCalculation(selectedMove);
    }
} 