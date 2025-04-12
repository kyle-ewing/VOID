package map;

import bwapi.Game;
import bwapi.Position;
import bwapi.TilePosition;
import bwem.BWEM;
import map.TilePositionValidator;

import java.util.*;

public class PathFinding {
    private BWEM bwem;
    private Game game;
    private TilePositionValidator tilePositionValidator;

    public PathFinding(BWEM bwem, Game game) {
        this.bwem = bwem;
        this.game = game;

        tilePositionValidator = new TilePositionValidator(game);
    }

    public List<Position> findPath(Position start, Position goal) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::getF));
        Map<Position, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start, null, 0, heuristic(start, goal));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.getPosition().equals(goal)) {
                return reconstructPath(current);
            }

            for (Position neighbor : getNeighbors(current.getPosition())) {
                double tentativeG = current.getG() + current.getPosition().getApproxDistance(neighbor);

                Node neighborNode = allNodes.getOrDefault(neighbor, new Node(neighbor));
                if (tentativeG < neighborNode.getG()) {
                    neighborNode.setCameFrom(current);
                    neighborNode.setG(tentativeG);
                    neighborNode.setF(tentativeG + heuristic(neighbor, goal));

                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode);
                    }
                    allNodes.put(neighbor, neighborNode);
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    private List<Position> reconstructPath(Node node) {
        List<Position> path = new ArrayList<>();
        while (node != null) {
            path.add(node.getPosition());
            node = node.getCameFrom();
        }
        Collections.reverse(path);
        return path;
    }

    private double heuristic(Position a, Position b) {
        return a.getApproxDistance(b);
    }

    private List<Position> getNeighbors(Position position) {
        List<Position> neighbors = new ArrayList<>();
        TilePosition tilePosition = position.toTilePosition();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                TilePosition neighborTile = new TilePosition(tilePosition.getX() + dx, tilePosition.getY() + dy);

                if(tilePositionValidator.isValid(neighborTile)) {
                    if (bwem.getMap().getTile(neighborTile).isWalkable()) {
                        neighbors.add(neighborTile.toPosition());
                    }
                }
            }
        }

        return neighbors;
    }

    private static class Node {
        private Position position;
        private Node cameFrom;
        private double g;
        private double f;

        public Node(Position position) {
            this.position = position;
            this.g = Double.POSITIVE_INFINITY;
            this.f = Double.POSITIVE_INFINITY;
        }

        public Node(Position position, Node cameFrom, double g, double f) {
            this.position = position;
            this.cameFrom = cameFrom;
            this.g = g;
            this.f = f;
        }

        public Position getPosition() {
            return position;
        }

        public Node getCameFrom() {
            return cameFrom;
        }

        public void setCameFrom(Node cameFrom) {
            this.cameFrom = cameFrom;
        }

        public double getG() {
            return g;
        }

        public void setG(double g) {
            this.g = g;
        }

        public double getF() {
            return f;
        }

        public void setF(double f) {
            this.f = f;
        }
    }
}