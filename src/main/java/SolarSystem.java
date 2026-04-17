import processing.core.*;
import processing.event.MouseEvent;
import java.util.*;

public class SolarSystem extends PApplet {
    // --- Simulasyon Zaman Ayarı (1 sn = 30 gün) ---
    float timeStep = (30.0f / 365.0f) * TWO_PI / 60.0f;
    float masterTime = 0;
    boolean paused = false;

    // --- Kamera ve Görünüm ---
    float camRotX = PI/3, camRotY = PI/4, camDist = 1400;
    
    // --- Veri Yapıları ---
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
    Map<String, PVector> screenPosMap = new HashMap<>();
    float[][] stars = new float[500][3];

    // --- ASTRONOMIK SABITLER (Dunya=1 Ref) ---
    // Boyutlar (Yarıçap px)
    final float R_SUN = 60.0f, R_MERCURY = 3.0f, R_VENUS = 8.0f, R_EARTH = 8.0f, R_MARS = 4.0f;
    final float R_JUPITER = 40.0f, R_SATURN = 34.0f, R_URANUS = 16.0f, R_NEPTUNE = 15.0f;
    // Yörünge Uzaklıkları (px)
    final float D_MERCURY = 39, D_VENUS = 72, D_EARTH = 100, D_MARS = 152, D_JUPITER = 280, D_SATURN = 380, D_URANUS = 480, D_NEPTUNE = 580;
    // Yörünge Hız Çarpanları (1/Period)
    final float S_MERCURY = 4.15f, S_VENUS = 1.62f, S_EARTH = 1.0f, S_MARS = 0.53f, S_JUPITER = 0.084f, S_SATURN = 0.034f, S_URANUS = 0.012f, S_NEPTUNE = 0.006f;
    // Eksen Eğiklikleri (Derece -> Radyan)
    final float I_MERCURY = radians(7.0f), I_VENUS = radians(3.4f), I_EARTH = 0, I_MARS = radians(1.85f), I_JUPITER = radians(1.3f), I_SATURN = radians(2.49f), I_URANUS = radians(0.77f), I_NEPTUNE = radians(1.77f);

    public void settings() { size(1200, 800, P3D); }

    public void setup() {
        String[] names = {"SUN", "MERCURY", "VENUS", "EARTH", "MARS", "JUPITER", "SATURN", "URANUS", "NEPTUNE"};
        for (String n : names) {
            planetStates.put(n, new PlanetState());
            screenPosMap.put(n, new PVector());
        }
        for (int i = 0; i < 500; i++) {
            float r = random(4000, 9000); float theta = random(TWO_PI); float phi = random(PI);
            stars[i][0] = r * sin(phi) * cos(theta); stars[i][1] = r * sin(phi) * sin(theta); stars[i][2] = r * cos(phi);
        }
    }

    public void draw() {
        background(3, 3, 10);
        
        // Kamera Setup
        float eyeX = camDist * sin(camRotX) * cos(camRotY);
        float eyeY = camDist * cos(camRotX);
        float eyeZ = camDist * sin(camRotX) * sin(camRotY);
        camera(eyeX, eyeY, eyeZ, 0, 0, 0, 0, 1, 0);
        perspective(PI/3.0f, (float)width/height, 1, 20000);

        // Yıldızlar
        stroke(255, 180); strokeWeight(2);
        for (float[] s : stars) point(s[0], s[1], s[2]);

        // Işıklandırma
        ambientLight(40, 40, 50);
        pointLight(255, 210, 120, 0, 0, 0); // Güneş kaynağı

        // GÜNEŞ
        drawBody("SUN", 0, 0, 0, R_SUN, color(255, 200, 50), 0, 0);

        // GEZEGENLER
        drawPlanet("MERCURY", D_MERCURY, S_MERCURY, R_MERCURY, color(169), I_MERCURY, 58.6f);
        drawPlanet("VENUS", D_VENUS, S_VENUS, R_VENUS, color(255, 240, 180), I_VENUS, -243.0f);
        drawPlanet("EARTH", D_EARTH, S_EARTH, R_EARTH, color(70, 130, 180), I_EARTH, 1.0f);
        drawPlanet("MARS", D_MARS, S_MARS, R_MARS, color(188, 74, 60), I_MARS, 1.03f);
        drawPlanet("JUPITER", D_JUPITER, S_JUPITER, R_JUPITER, color(200, 160, 100), I_JUPITER, 0.41f);
        drawPlanet("SATURN", D_SATURN, S_SATURN, R_SATURN, color(210, 180, 100), I_SATURN, 0.44f);
        drawPlanet("URANUS", D_URANUS, S_URANUS, R_URANUS, color(100, 200, 210), I_URANUS, -0.72f);
        drawPlanet("NEPTUNE", D_NEPTUNE, S_NEPTUNE, R_NEPTUNE, color(50, 100, 200), I_NEPTUNE, 0.67f);

        if (!paused) masterTime += timeStep;
        drawUI();
    }

    void drawPlanet(String name, float dist, float speedMult, float size, int col, float incl, float selfRotDays) {
        PlanetState s = planetStates.get(name);
        float d = dist + s.distOffset;
        float angle = masterTime * speedMult + s.angleOffset;

        // Yörünge Çizimi
        pushMatrix();
        rotateZ(incl); noFill(); stroke(255, 30); strokeWeight(1);
        beginShape();
        for (float a=0; a<TWO_PI; a+=0.05) vertex(cos(a)*d, 0, sin(a)*d);
        endShape(CLOSE);
        popMatrix();

        // Pozisyon Hesaplama
        float px = cos(incl) * cos(angle) * d;
        float py = sin(incl) * cos(angle) * d;
        float pz = sin(angle) * d;

        drawBody(name, px, py, pz, size, col, selfRotDays, s.scale);

        // Uydular ve Özel Yapılar
        pushMatrix();
        translate(px, py, pz);
        if (name.equals("EARTH")) drawSatellite(30, 27.3f, 2, color(180), masterTime);
        if (name.equals("MARS")) {
            drawSatellite(10, 0.32f, 1, color(150), masterTime);
            drawSatellite(15, 1.26f, 1, color(140), masterTime * 0.8f);
        }
        if (name.equals("JUPITER")) {
            drawSatellite(55, 1.77f, 2, color(255, 255, 200), masterTime);
            drawSatellite(70, 3.55f, 2, color(200, 255, 255), masterTime * 0.5f);
            drawSatellite(90, 7.15f, 3, color(200, 200, 180), masterTime * 0.3f);
        }
        if (name.equals("SATURN")) {
            drawSaturnRings(size);
            drawSatellite(60, 15.9f, 3, color(230, 200, 150), masterTime * 0.1f);
        }
        if (name.equals("NEPTUNE")) drawSatellite(35, -5.88f, 2, color(200), masterTime); // Retrograd Triton
        popMatrix();
    }

    void drawBody(String name, float x, float y, float z, float size, int col, float rotPeriod, float localScale) {
        screenPosMap.get(name).set(screenX(x, y, z), screenY(x, y, z), screenZ(x, y, z));
        pushMatrix();
        translate(x, y, z);
        if (rotPeriod != 0) rotateY(masterTime * (365.0f / rotPeriod));
        scale(localScale == 0 ? 1.0f : localScale);
        
        noStroke(); fill(col);
        sphere(size);
        
        if (selectedPlanet.equals(name)) {
            noFill(); stroke(255, 255, 0); strokeWeight(2); box(size * 2.5f);
        }
        popMatrix();
    }

    void drawSatellite(float d, float p, float sz, int c, float t) {
        float ang = t * (365.0f / p);
        pushMatrix();
        translate(cos(ang)*d, 0, sin(ang)*d);
        fill(c); noStroke(); sphere(sz);
        popMatrix();
    }

    void drawSaturnRings(float r) {
        noFill(); stroke(150, 130, 100, 80); strokeWeight(2);
        rotateX(radians(26.7f));
        for (float i = r * 1.2f; i < r * 2.3f; i += 2) {
            beginShape();
            for (float a = 0; a < TWO_PI; a += 0.2) vertex(cos(a)*i, 0, sin(a)*i);
            endShape(CLOSE);
        }
    }

    void drawUI() {
        hint(DISABLE_DEPTH_TEST); camera(); noLights();
        fill(0, 180); rect(10, 10, 320, 190);
        fill(255); textSize(12);
        text("ASTRONOMIC SIMULATION v3.0", 20, 30);
        text("FPS: " + (int)frameRate + " | SELECTED: " + selectedPlanet, 20, 50);
        if (!selectedPlanet.equals("NONE")) {
            PlanetState s = planetStates.get(selectedPlanet);
            text(String.format("Dist Offset: %.0f | Scale: %.2f", s.distOffset, s.scale), 20, 75);
            text("Rel. Velocity: " + (paused ? "0 (PAUSED)" : "30 Days / Sec"), 20, 95);
        }
        text("Drag L-Mouse: Rotate View | Scroll: Zoom", 20, 125);
        text("SPACE: Play/Pause | U: Undo (10 Steps)", 20, 145);
        text("ARROWS: Dist Offset | W/S: Local Scale", 20, 165);
        hint(ENABLE_DEPTH_TEST);
    }

    public void mouseDragged() {
        camRotY += (mouseX - pmouseX) * 0.01f;
        camRotX = constrain(camRotX + (mouseY - pmouseY) * 0.01f, 0.1f, PI - 0.1f);
    }

    public void mouseWheel(MouseEvent event) { camDist = constrain(camDist + event.getCount() * 50, 200, 6000); }

    public void mousePressed() {
        selectedPlanet = "NONE"; float bestZ = 1.0f;
        for (String name : planetStates.keySet()) {
            PVector p = screenPosMap.get(name);
            if (dist(mouseX, mouseY, p.x, p.y) < 40 && p.z < bestZ) {
                selectedPlanet = name; bestZ = p.z;
            }
        }
    }

    public void keyPressed() {
        if (key == ' ') paused = !paused;
        if (key == 'u' || key == 'U') undo();
        if (selectedPlanet.equals("NONE")) return;
        saveState();
        PlanetState s = planetStates.get(selectedPlanet);
        if (keyCode == UP) s.distOffset += 10; if (keyCode == DOWN) s.distOffset -= 10;
        if (key == 'w' || key == 'W') s.scale += 0.1f; if (key == 's' || key == 'S') s.scale = max(0.1f, s.scale - 0.1f);
    }

    void saveState() {
        Map<String, PlanetState> snap = new HashMap<>();
        for (var e : planetStates.entrySet()) snap.put(e.getKey(), e.getValue().copy());
        undoStack.push(snap); if (undoStack.size() > 10) undoStack.removeLast();
    }

    void undo() { if (!undoStack.isEmpty()) planetStates = undoStack.pop(); }

    public static void main(String[] args) { PApplet.main("SolarSystem"); }
}