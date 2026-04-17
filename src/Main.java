import processing.core.*;
import processing.opengl.*;
import java.util.*;

public class Main extends PApplet {
    // --- Camera & View State ---
    float camRotX = PI/4, camRotY = PI/4, camDist = 1200;
    float earthAngle = 0, moonAngle = 0, marsAngle = 0, jupiterAngle = 0, saturnAngle = 0;
    boolean paused = false;

    // --- Data Structures ---
    class PlanetState {
        float distOffset = 0, angleOffset = 0, rot = 0, scale = 1.0f;
        PlanetState copy() {
            PlanetState s = new PlanetState();
            s.distOffset = distOffset; s.angleOffset = angleOffset;
            s.rot = rot; s.scale = scale;
            return s;
        }
    }

    Map<String, PlanetState> planetStates = new HashMap<>();
    Deque<Map<String, PlanetState>> undoStack = new ArrayDeque<>();
    String selectedPlanet = "NONE";
    Map<String, PVector> screenPos = new HashMap<>();

    // --- Visuals ---
    float[][] stars = new float[500][3];

    public void settings() {
        size(1200, 800, P3D);
    }

    public void setup() {
        String[] names = {"SUN", "EARTH", "MARS", "JUPITER", "SATURN"};
        for (String n : names) {
            planetStates.put(n, new PlanetState());
            screenPos.put(n, new PVector());
        }

        // Distant 3D Stars
        for (int i = 0; i < 500; i++) {
            float r = random(4000, 8000);
            float theta = random(TWO_PI);
            float phi = random(PI);
            stars[i][0] = r * sin(phi) * cos(theta);
            stars[i][1] = r * sin(phi) * sin(theta);
            stars[i][2] = r * cos(phi);
        }
    }

    public void draw() {
        background(5, 5, 15);

        // 1. Setup 3D Scene
        pushMatrix();
        translate(width/2, height/2, 0);
        
        // Orbit Camera Controls
        if (mousePressed && mouseButton == LEFT) {
            camRotY += (mouseX - pmouseX) * 0.01f;
            camRotX -= (mouseY - pmouseY) * 0.01f;
        }
        rotateX(camRotX);
        rotateY(camRotY);
        translate(0, 0, -camDist + 1200);

        // Lights
        ambientLight(40, 40, 60);
        pointLight(255, 230, 180, 0, 0, 0); // Sun light source

        // 2. Draw Stars (Deep Space)
        stroke(255);
        strokeWeight(2);
        for (int i = 0; i < 500; i++) point(stars[i][0], stars[i][1], stars[i][2]);
        noStroke();

        // 3. Draw Solar System
        drawPlanet("SUN", 0, 0, 60, color(255, 220, 0));
        drawPlanet("EARTH", 250, earthAngle, 30, color(50, 120, 255));
        drawPlanet("MARS", 350, marsAngle, 22, color(230, 80, 40));
        drawPlanet("JUPITER", 500, jupiterAngle, 65, color(200, 170, 130));
        drawPlanet("SATURN", 700, saturnAngle, 55, color(220, 200, 150));

        popMatrix();

        // 4. UI Overlay
        drawUI();

        if (!paused) {
            earthAngle += 0.01f; moonAngle += 0.04f; marsAngle += 0.008f;
            jupiterAngle += 0.004f; saturnAngle += 0.003f;
        }
    }

    void drawPlanet(String name, float baseR, float angle, float size, int col) {
        PlanetState s = planetStates.get(name);
        float r = baseR + s.distOffset;
        float curAngle = angle + s.angleOffset;

        // Draw Orbit Line (XZ Plane)
        if (baseR > 0) {
            noFill(); stroke(255, 50); strokeWeight(1);
            beginShape();
            for (float a = 0; a < TWO_PI; a += 0.1) vertex(cos(a)*r, 0, sin(a)*r);
            endShape(CLOSE);
        }

        // Trig Position (XZ Plane)
        float x = cos(curAngle) * r;
        float z = sin(curAngle) * r;
        float y = 0;

        // Capture Screen Pos for Picking
        screenPos.get(name).set(screenX(x, y, z), screenY(x, y, z), screenZ(x, y, z));

        pushMatrix();
        translate(x, y, z);
        
        // Local Rotation & Scale
        rotateY(s.rot);
        scale(s.scale);

        // Body
        fill(col);
        noStroke();
        sphere(size/2);

        // Saturn Rings
        if (name.equals("SATURN")) {
            noFill(); stroke(200, 150, 100, 150); strokeWeight(4);
            rotateX(PI/2.5f);
            ellipse(0, 0, size * 2.5f, size * 2.2f);
        } 
        // Earth Moon
        else if (name.equals("EARTH")) {
            pushMatrix();
            rotateY(moonAngle);
            translate(50, 0, 0);
            fill(180);
            sphere(6);
            popMatrix();
        }

        popMatrix();

        // Selection Highlight
        if (selectedPlanet.equals(name)) {
            noFill(); stroke(255, 255, 0); strokeWeight(2);
            pushMatrix();
            translate(x, y, z);
            box(size + 10);
            popMatrix();
        }
    }

    void drawUI() {
        hint(DISABLE_DEPTH_TEST);
        camera(); // Reset matrix to 2D screen space
        noLights();
        
        fill(0, 180);
        rect(10, 10, 300, 150);
        fill(255);
        textSize(14);
        text("FPS: " + (int)frameRate, 20, 35);
        text("SELECTED: " + selectedPlanet, 20, 55);
        
        if (!selectedPlanet.equals("NONE")) {
            PlanetState s = planetStates.get(selectedPlanet);
            text(String.format("Scale: %.2f | Rot: %.2f", s.scale, s.rot), 20, 75);
            text("3D Homogeneous Matrix Active", 20, 95);
        }
        
        text("Drag: Orbit | Scroll: Zoom", 20, 125);
        text("SPACE: Pause | U: Undo | Arrows: Offset", 20, 145);

        hint(ENABLE_DEPTH_TEST);
    }

    public void mouseWheel(MouseEvent event) {
        camDist += event.getCount() * 30;
        camDist = constrain(camDist, 200, 4000);
    }

    public void mousePressed() {
        selectedPlanet = "NONE";
        float closestZ = Float.MAX_VALUE;
        for (String name : planetNames) {
            PVector p = screenPos.get(name);
            float d = dist(mouseX, mouseY, p.x, p.y);
            if (d < 40 && p.z < closestZ) {
                selectedPlanet = name;
                closestZ = p.z;
            }
        }
    }

    public void keyPressed() {
        if (key == ' ') paused = !paused;
        if (key == 'u' || key == 'U') undo();
        
        if (selectedPlanet.equals("NONE")) return;
        saveState();
        PlanetState s = planetStates.get(selectedPlanet);
        if (keyCode == UP) s.distOffset -= 10;
        if (keyCode == DOWN) s.distOffset += 10;
        if (keyCode == LEFT) s.angleOffset -= 0.1f;
        if (keyCode == RIGHT) s.angleOffset += 0.1f;
        if (key == 'a' || key == 'A') s.rot -= 0.1f;
        if (key == 'd' || key == 'D') s.rot += 0.1f;
        if (key == 'w' || key == 'W') s.scale += 0.1f;
        if (key == 's' || key == 'S') s.scale = max(0.1f, s.scale - 0.1f);
    }

    void saveState() {
        Map<String, PlanetState> snap = new HashMap<>();
        for (String k : planetStates.keySet()) snap.put(k, planetStates.get(k).copy());
        undoStack.push(snap);
        if (undoStack.size() > 10) undoStack.removeLast();
    }

    void undo() {
        if (!undoStack.isEmpty()) planetStates = undoStack.pop();
    }

    private final String[] planetNames = {"SUN", "EARTH", "MARS", "JUPITER", "SATURN"};

    public static void main(String[] args) {
        PApplet.main("Main");
    }
}