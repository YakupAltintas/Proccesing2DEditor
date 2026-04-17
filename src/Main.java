import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class Main extends JPanel {
    private float earthAngle = 0, moonAngle = 0, marsAngle = 0, jupiterAngle = 0, saturnAngle = 0, rayAngle = 0;
    private boolean paused = false;
    private long lastTime = System.nanoTime();
    private int fps = 0, frameCount = 0;

    private final int[] starX = new int[200], starY = new int[200];
    private final Map<String, LinkedList<Point>> trails = new HashMap<>();
    private final Map<String, Point2D> positions = new HashMap<>();
    private final Map<String, Integer> sizes = new HashMap<>();
    private final String[] planetNames = {"SUN", "EARTH", "MARS", "JUPITER", "SATURN"};
    private String selectedPlanet = "NONE";

    private static class PlanetState {
        double distOffset = 0, angleOffset = 0, rot = 0, scale = 1.0;
        PlanetState copy() {
            PlanetState s = new PlanetState();
            s.distOffset = distOffset; s.angleOffset = angleOffset; s.rot = rot; s.scale = scale;
            return s;
        }
    }
    private Map<String, PlanetState> planetStates = new HashMap<>();
    private Deque<Map<String, PlanetState>> undoStack = new ArrayDeque<>();

    public Main() {
        setBackground(Color.BLACK);
        setFocusable(true);
        Random rand = new Random();

        for (int i = 0; i < 200; i++) { starX[i] = rand.nextInt(1200); starY[i] = rand.nextInt(1200); }
        for (String n : planetNames) {
            planetStates.put(n, new PlanetState());
            trails.put(n, new LinkedList<>());
            positions.put(n, new Point2D.Double(0,0));
        }
        sizes.put("SUN", 60); sizes.put("EARTH", 30); sizes.put("MARS", 20); sizes.put("JUPITER", 55); sizes.put("SATURN", 45);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                selectedPlanet = "NONE";
                double closest = Double.MAX_VALUE;
                for (String name : planetNames) {
                    double d = e.getPoint().distance(positions.get(name));
                    double r = (sizes.get(name) * planetStates.get(name).scale / 2.0) + 15;
                    if (d < r && d < closest) { selectedPlanet = name; closest = d; }
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) paused = !paused;
                if (e.getKeyCode() == KeyEvent.VK_U) undo();
                if (selectedPlanet.equals("NONE")) return;
                saveState();
                PlanetState s = planetStates.get(selectedPlanet);
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> s.distOffset -= 5; case KeyEvent.VK_DOWN -> s.distOffset += 5;
                    case KeyEvent.VK_LEFT -> s.angleOffset -= 0.05; case KeyEvent.VK_RIGHT -> s.angleOffset += 0.05;
                    case KeyEvent.VK_A -> s.rot -= 0.1; case KeyEvent.VK_D -> s.rot += 0.1;
                    case KeyEvent.VK_W -> s.scale += 0.05; case KeyEvent.VK_S -> s.scale = Math.max(0.1, s.scale - 0.05);
                }
            }
        });

        new javax.swing.Timer(16, e -> {
            if (!paused) {
                earthAngle += 0.015f; moonAngle += 0.06f; marsAngle += 0.012f;
                jupiterAngle += 0.006f; saturnAngle += 0.004f; rayAngle += 0.01f;
                updateTrails();
            }
            updateFPS(); repaint();
        }).start();
    }

    private void updateTrails() {
        positions.forEach((name, p) -> {
            if (!name.equals("SUN")) {
                LinkedList<Point> t = trails.get(name);
                t.addFirst(new Point((int)p.getX(), (int)p.getY()));
                if (t.size() > 40) t.removeLast();
            }
        });
    }

    private void updateFPS() {
        frameCount++; long now = System.nanoTime();
        if (now - lastTime >= 1_000_000_000L) { fps = frameCount; frameCount = 0; lastTime = now; }
    }

    private void saveState() {
        Map<String, PlanetState> snap = new HashMap<>();
        planetStates.forEach((k, v) -> snap.put(k, v.copy()));
        undoStack.push(snap);
        if (undoStack.size() > 10) undoStack.removeLast();
    }

    private void undo() { if (!undoStack.isEmpty()) { planetStates = undoStack.pop(); repaint(); } }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float cx = getWidth() / 2.0f;
        float cy = getHeight() / 2.0f;

        drawNebula(g2d);
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < 200; i++) g2d.fillOval(starX[i], starY[i], 2, 2);

        drawAllTrails(g2d);
        drawSolarRays(g2d, cx, cy);

        drawPlanet(g2d, cx, cy, 0, 0, "SUN", Color.YELLOW);
        drawPlanet(g2d, cx, cy, 180, earthAngle, "EARTH", new Color(50, 150, 255));
        drawPlanet(g2d, cx, cy, 280, marsAngle, "MARS", new Color(255, 80, 30));
        drawPlanet(g2d, cx, cy, 380, jupiterAngle, "JUPITER", new Color(200, 160, 120));
        drawPlanet(g2d, cx, cy, 480, saturnAngle, "SATURN", new Color(230, 200, 150));

        drawPanels(g2d);
    }

    private void drawPlanet(Graphics2D g2d, float cx, float cy, float baseDist, float orbitAngle, String name, Color color) {
        PlanetState s = planetStates.get(name);
        float r = (float)(baseDist + s.distOffset);
        float angle = (float)(orbitAngle + s.angleOffset);

        if (baseDist > 0) {
            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            g2d.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        }

        float px = cx + (float)Math.cos(angle) * r;
        float py = cy + (float)Math.sin(angle) * r;
        positions.put(name, new Point2D.Float(px, py));

        AffineTransform old = g2d.getTransform();
        g2d.translate(px, py);
        g2d.rotate(s.rot);
        g2d.scale(s.scale, s.scale);

        int size = sizes.get(name);
        for (int i = 0; i < 3; i++) {
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40 - i*10));
            float gs = size + 10 + i*15;
            g2d.fill(new Ellipse2D.Float(-gs/2, -gs/2, gs, gs));
        }
        g2d.setColor(color);
        g2d.fill(new Ellipse2D.Float(-size/2.0f, -size/2.0f, size, size));

        if (name.equals("SATURN")) {
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.draw(new Ellipse2D.Float(-size, -size/4.0f, size*2, size/2.0f));
        } else if (name.equals("EARTH")) {
            float mx = (float)Math.cos(moonAngle) * 45;
            float my = (float)Math.sin(moonAngle) * 45;
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fill(new Ellipse2D.Float(mx-6, my-6, 12, 12));
        }

        g2d.setTransform(old);

        if (selectedPlanet.equals(name)) {
            g2d.setColor(Color.YELLOW); g2d.setStroke(new BasicStroke(2));
            float ss = (float)(size * s.scale + 20);
            g2d.draw(new Ellipse2D.Float(px - ss/2, py - ss/2, ss, ss));
        }
    }

    private void drawNebula(Graphics2D g2d) {
        Color[] c = {new Color(80, 0, 120, 15), new Color(0, 40, 100, 10), new Color(100, 40, 0, 10)};
        g2d.setColor(c[0]); g2d.fillOval(50, 50, 600, 400);
        g2d.setColor(c[1]); g2d.fillOval(getWidth()-500, getHeight()-400, 500, 400);
    }

    private void drawSolarRays(Graphics2D g2d, float x, float y) {
        g2d.setColor(new Color(255, 255, 200, 20));
        AffineTransform old = g2d.getTransform();
        g2d.translate(x, y); g2d.rotate(rayAngle);
        for (int i = 0; i < 16; i++) { g2d.rotate(Math.PI/8); g2d.drawLine(0, 0, 0, 1500); }
        g2d.setTransform(old);
    }

    private void drawAllTrails(Graphics2D g2d) {
        trails.forEach((name, list) -> {
            int a = 140; for (Point p : list) {
                g2d.setColor(new Color(255, 255, 255, a));
                g2d.fillOval(p.x, p.y, 2, 2); a = Math.max(0, a - 4);
            }
        });
    }

    private void drawPanels(Graphics2D g2d) {
        g2d.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2d.setColor(new Color(0, 0, 0, 210)); g2d.fillRect(10, 10, 280, 120);
        g2d.setColor(Color.WHITE); g2d.drawRect(10, 10, 280, 120);
        g2d.drawString("FPS: " + fps + " | SELECTED: " + selectedPlanet, 20, 30);
        if (!selectedPlanet.equals("NONE")) {
            Point2D p = positions.get(selectedPlanet); PlanetState s = planetStates.get(selectedPlanet);
            g2d.drawString(String.format("POS: [%.0f, %.0f]", p.getX(), p.getY()), 20, 55);
            g2d.drawString(String.format("DIST_OFF: %.1f | ANG_OFF: %.2f", s.distOffset, s.angleOffset), 20, 75);
            g2d.drawString(String.format("ROT: %.2f | SCALE: %.2f", s.rot, s.scale), 20, 95);
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("SymDev - Solar System Fixed v2.2");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1000, 1000); f.add(new Main());
        f.setLocationRelativeTo(null); f.setVisible(true);
    }
}