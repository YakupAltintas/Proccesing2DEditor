import processing.core.*;
import processing.event.MouseEvent;
import java.util.*;

public class SolarSystem extends PApplet {
    float camRotX = PI/4, camRotY = PI/4, camDist = 1200;
    float earthAngle = 0, moonAngle = 0, marsAngle = 0, jupiterAngle = 0, saturnAngle = 0;
    boolean paused = false;

    static class PlanetState {
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
    Map<String, PVector> screenPositions = new HashMap<>();
    float[][] stars = new float[500][3];

    public void settings() {
        size(1200, 800, P3D);
    }

    public void setup() {
        String[] names = {"SUN", "EARTH", "MARS", "JUPITER", "SATURN"};
        for (String n : names) {
            planetStates.put(n, new PlanetState());
            screenPositions.put(n, new PVector());
        }

        for (int i = 0; i < 500; i++) {
            float r = random(3000, 7000);
            float theta = random(TWO_PI);
            float phi = random(PI);
            stars[i][0] = r * sin(phi) * cos(theta);
            stars[i][1] = r * sin(phi) * sin(theta);
            stars[i][2] = r * cos(phi);
        }
    }

    public void draw() {
        background(5, 5, 20);

        // --- Camera Setup ---
        float eyeX = camDist * sin(camRotX) * cos(camRotY);
        float eyeY = camDist * cos(camRotX);
        float eyeZ = camDist * sin(camRotX) * sin(camRotY);
        camera(eyeX, eyeY, eyeZ, 0, 0, 0, 0, 1, 0);
        perspective(PI/3.0f, (float)width/height, 10, 15000);

        // --- Stars ---
        stroke(255, 200);
        for (float[] s : stars) point(s[0], s[1], s[2]);

        // --- Lights ---
        ambientLight(50, 50, 70);
        pointLight(255, 220, 150, 0, 0, 0);

        // --- Planets ---
        drawPlanet("SUN", 0, 0, 70, color(255, 200, 0));
        drawPlanet("EARTH", 280, earthAngle, 32, color(60, 140, 255));
        drawPlanet("MARS", 400, marsAngle, 24, color(240, 100, 50));
        drawPlanet("JUPITER", 550, jupiterAngle, 75, color(210, 180, 140));
        drawPlanet("SATURN", 750, saturnAngle, 60, color(230, 210, 170));

        if (!paused) {
            earthAngle += 0.01f; moonAngle += 0.05f; marsAngle += 0.008f;
            jupiterAngle += 0.004f; saturnAngle += 0.003f;
        }

        drawUI();
    }

    void drawPlanet(String name, float baseDist, float angle, float size, int col) {
        PlanetState s = planetStates.get(name);
        float r = baseDist + s.distOffset;
        float curAngle = angle + s.angleOffset;

        // Orbit Line
        if (baseDist > 0) {
            noFill(); stroke(255, 40); strokeWeight(1);
            beginShape();
            for (float a = 0; a < TWO_PI; a += 0.1) vertex(cos(a)*r, 0, sin(a)*r);
            endShape(CLOSE);
        }

        float px = cos(curAngle) * r;
        float pz = sin(curAngle) * r;

        screenPositions.get(name).set(screenX(px, 0, pz), screenY(px, 0, pz), screenZ(px, 0, pz));

        pushMatrix();
        translate(px, 0, pz);
        rotateY(s.rot);
        scale(s.scale);

        noStroke();
        fill(col);
        sphere(size/2);

        if (name.equals("SATURN")) {
            drawSaturnRings(size);
        } else if (name.equals("EARTH")) {
            drawMoon();
        }

        popMatrix();

        if (selectedPlanet.equals(name)) {
            noFill(); stroke(255, 255, 0); strokeWeight(2);
            pushMatrix(); translate(px, 0, pz); box(size + 15); popMatrix();
        }
    }

    void drawSaturnRings(float size) {
        noFill(); stroke(200, 180, 150, 100); strokeWeight(3);
        beginShape(TRIANGLE_STRIP);
        for (float a = 0; a <= TWO_PI + 0.1; a += 0.2) {
            vertex(cos(a)*size, 0, sin(a)*size);
            vertex(cos(a)*size*1.8f, 0, sin(a)*size*1.8f);
        }
        endShape();
    }

    void drawMoon() {
        pushMatrix();
        rotateY(moonAngle);
        translate(55, 0, 0);
        fill(180);
        sphere(6);
        popMatrix();
    }

    void drawUI() {
        hint(DISABLE_DEPTH_TEST);
        camera();
        noLights();
        fill(0, 150); rect(10, 10, 320, 180);
        fill(255); textSize(13);
        text("FPS: " + (int)frameRate + " | SELECTED: " + selectedPlanet, 20, 35);
        if (!selectedPlanet.equals("NONE")) {
            PlanetState s = planetStates.get(selectedPlanet);
            text(String.format("Scale: %.2f | Rot: %.2f | DistOff: %.0f", s.scale, s.rot, s.distOffset), 20, 60);
            text("3x3 Transform Projection: [XZ -> Screen]", 20, 80);
        }
        text("Controls:\nDrag: Orbit | Scroll: Zoom\nSPACE: Pause | U: Undo\nARROWS: Offset | A/D: Rot | W/S: Scale", 20, 110);
        hint(ENABLE_DEPTH_TEST);
    }

    public void mouseDragged() {
        camRotY += (mouseX - pmouseX) * 0.01f;
        camRotX = constrain(camRotX + (mouseY - pmouseY) * 0.01f, 0.1f, PI - 0.1f);
    }

    public void mouseWheel(MouseEvent event) {
        camDist = constrain(camDist + event.getCount() * 50, 300, 5000);
    }

    public void mousePressed() {
        selectedPlanet = "NONE";
        float bestZ = 1.0f;
        for (String name : planetStates.keySet()) {
            PVector p = screenPositions.get(name);
            if (dist(mouseX, mouseY, p.x, p.y) < 40 && p.z < bestZ) {
                selectedPlanet = name;
                bestZ = p.z;
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
        for (var e : planetStates.entrySet()) snap.put(e.getKey(), e.getValue().copy());
        undoStack.push(snap);
        if (undoStack.size() > 10) undoStack.removeLast();
    }

    void undo() { if (!undoStack.isEmpty()) planetStates = undoStack.pop(); }

    public static void main(String[] args) {
        PApplet.main("SolarSystem");
    }
}