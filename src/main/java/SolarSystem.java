import processing.core.*;
import processing.event.MouseEvent;
import java.util.*;

public class SolarSystem extends PApplet {
    Camera cam;
    TimeSystem time;
    Planet[] planets = new Planet[8];
    ArrayList<Asteroid> belt = new ArrayList<>();
    float[][] stars = new float[600][4];
    boolean showGrid = true;
    
    // Seçim ve Görselleştirme
    Object selectedBody = null; 
    float sunSX, sunSY, sunSR = 60;
    boolean isDraggingSlider = false;

    // UI Renkleri
    int uiBG = color(10, 10, 25, 230);
    int uiBorder = color(50, 100, 200, 180);
    int uiText = color(220, 230, 255);
    int accent = color(0, 180, 255);

    public void settings() { size(1200, 800, P3D); smooth(8); }

    public void setup() {
        cam = new Camera();
        time = new TimeSystem();
        
        // Gezegen Verileri (isim, yörünge, boyut, renk, hız, eğim, uydusayısı, gerçekVeriler...)
        planets[0] = new Planet("Merkür", 100, 6, 0xFFA9A9A9, 0.0408f, 0.122f, 0, "57.9M", "4,879", "88");
        planets[1] = new Planet("Venüs",  150, 9, 0xFFFFF0B4, 0.0162f, 0.059f, 0, "108.2M", "12,104", "224.7");
        planets[2] = new Planet("Dünya",  210, 8, 0xFF4682B4, 0.0100f, 0.000f, 1, "149.6M", "12,742", "365.2");
        planets[3] = new Planet("Mars",   270, 5, 0xFFBC4A3C, 0.0053f, 0.032f, 2, "227.9M", "6,779", "687");
        planets[4] = new Planet("Jüpiter",390, 35, 0xFFC8A064, 0.0008f, 0.023f, 4, "778.6M", "139,820", "4,333");
        planets[5] = new Planet("Satürn", 490, 28, 0xFFD2B464, 0.0003f, 0.043f, 1, "1.4B", "116,460", "10,759");
        planets[6] = new Planet("Uranüs", 570, 16, 0xFF64C8D2, 0.0001f, 0.013f, 1, "2.9B", "50,724", "30,687");
        planets[7] = new Planet("Neptün", 640, 15, 0xFF3264C8, 0.00006f, 0.031f, 1, "4.5B", "49,244", "60,190");

        // Başlangıç Açıları
        for(int i=0; i<8; i++) planets[i].angle = i * 1.5f;

        // Uydular
        planets[2].moons[0] = new Moon("Ay", 22, 2, 0xFFC8C8C8, 0.037f);
        planets[3].moons[0] = new Moon("Phobos", 12, 1, 0xFF967850, 0.316f);
        planets[3].moons[1] = new Moon("Deimos", 18, 1, 0xFF8C7864, 0.080f);
        planets[4].moons[0] = new Moon("Io", 55, 2, 0xFFFFDC64, 0.056f);
        planets[4].moons[1] = new Moon("Europa", 70, 2, 0xFFD2B48C, 0.028f);
        planets[4].moons[2] = new Moon("Ganymede", 90, 3, 0xFFB4A082, 0.014f);
        planets[4].moons[3] = new Moon("Callisto", 110, 3, 0xFF78645A, 0.006f);
        planets[5].moons[0] = new Moon("Titan", 60, 3, 0xFFD2AA50, 0.006f);
        planets[6].moons[0] = new Moon("Titania", 40, 1, 0xFFB4B4BE, 0.011f);
        planets[7].moons[0] = new Moon("Triton", 28, 2, 0xFF96C8D2, -0.017f);

        for(int i=0; i<300; i++) belt.add(new Asteroid());
        for (int i = 0; i < 600; i++) {
            stars[i][0] = random(-10000, 10000); stars[i][1] = random(-10000, 10000);
            stars[i][2] = random(-10000, 10000); stars[i][3] = random(100, 255);
        }
    }

    public void draw() {
        background(3, 3, 12);
        time.update();
        cam.update();

        // 1. Sahne Hazırlığı
        pushMatrix();
        cam.apply();

        noLights();
        for (int i=0; i<600; i++) {
            stroke(stars[i][3]); strokeWeight(1); point(stars[i][0], stars[i][1], stars[i][2]);
        }

        if (showGrid) drawGrid();

        ambientLight(20, 20, 40);
        pointLight(255, 230, 180, 0, 0, 0);
        lightFalloff(1.0f, 0.0f, 0.000001f);

        // GÜNEŞ (Kendi Işığı)
        pushMatrix();
        noLights(); emissive(255, 200, 50); fill(255, 200, 50); noStroke();
        sphere(60);
        sunSX = screenX(0,0,0); sunSY = screenY(0,0,0);
        emissive(0); lights();
        popMatrix();

        // ASTEROIDLER
        noLights(); stroke(150, 80); 
        for (Asteroid a : belt) a.display(time.totalDays);
        lights();

        // GEZEGENLER
        for (Planet p : planets) {
            p.update(time.simSpeed);
            p.display();
        }

        popMatrix();

        // 2. 2D Overlay (Glow & UI)
        drawOverlays();
    }

    void drawOverlays() {
        hint(DISABLE_DEPTH_TEST);
        camera(); // UI için 2D moda geç
        
        // Güneş Halo Efekti
        noFill();
        for(int i = 1; i <= 4; i++) {
            stroke(255, 200, 50, 60.0f/i);
            strokeWeight(i * 3);
            ellipse(sunSX, sunSY, 100*i*0.6f, 100*i*0.6f);
        }

        // Seçim Vurgusu (Sarı Halka)
        if (selectedBody != null) {
            stroke(255, 220, 0); strokeWeight(2); noFill();
            if (selectedBody instanceof Planet) {
                Planet p = (Planet) selectedBody;
                ellipse(p.sx, p.sy, p.size*5, p.size*5);
                drawInfoPanel(p);
            } else {
                ellipse(sunSX, sunSY, 150, 150);
            }
        }

        drawSpeedPanel();
        drawLegend();
        hint(ENABLE_DEPTH_TEST);
    }

    void drawInfoPanel(Planet p) {
        fill(uiBG); stroke(uiBorder); rect(20, 20, 260, 280, 8);
        fill(accent); textSize(16); text("● " + p.name.toUpperCase(), 40, 50);
        stroke(uiBorder); line(30, 60, 270, 60);
        fill(uiText); textSize(12);
        text("Yörünge: " + p.realOrbit, 40, 85);
        text("Çap: " + p.realDiam + " km", 40, 105);
        text("Periyot: " + p.realPeriod + " gün", 40, 125);
        text("Uydu Sayısı: " + p.moons.length, 40, 145);
        
        line(30, 165, 270, 165);
        text("Homojen Dönüşüm Matrisi (3x3):", 40, 185);
        float a = p.angle;
        text(String.format("[ %.2f  %.2f  %.0f ]", cos(a), -sin(a), p.sx), 45, 210);
        text(String.format("[ %.2f  %.2f  %.0f ]", sin(a),  cos(a), p.sy), 45, 230);
        text("[ 0.00  0.00  1.00 ]", 45, 250);
    }

    void drawSpeedPanel() {
        int sw = 280, sh = 10, sx = width/2 - sw/2, sy = height - 50;
        fill(uiBG); stroke(uiBorder); rect(width/2-200, height-80, 400, 60, 30);
        
        fill(50); noStroke(); rect(sx, sy, sw, sh, 5);
        float progress = map(log10(time.simSpeed / 0.1f), 0, log10(500), 0, sw);
        fill(accent); rect(sx, sy, progress, sh, 5);
        
        fill(uiText); textAlign(CENTER);
        text("SPEED: " + nf(time.simSpeed, 1, 1) + "x  |  " + time.getDateString(), width/2, sy - 10);
        textAlign(LEFT);
    }

    float log10(float x) { return log(x) / log(10); }

    void drawLegend() {
        fill(uiBG); rect(width-200, 20, 180, 140, 8);
        fill(uiText); textSize(11);
        text("SPACE : Pause", width-185, 45);
        text("F / T : Focus / Follow", width-185, 65);
        text("+/-   : Speed Adjust", width-185, 85);
        text("ESC   : Reset View", width-185, 105);
        text("G     : Grid Toggle", width-185, 125);
    }

    void drawGrid() {
        stroke(255, 20); strokeWeight(1);
        for(int i=-1000; i<=1000; i+=100) { line(i, 0, -1000, i, 0, 1000); line(-1000, 0, i, 1000, 0, i); }
        stroke(255, 0, 0, 100); line(0,0,0, 200,0,0); // X
        stroke(0, 255, 0, 100); line(0,0,0, 0,200,0); // Y
        stroke(0, 0, 255, 100); line(0,0,0, 0,0,200); // Z
    }

    public void mousePressed() {
        int sx = width/2 - 140, sy = height - 55, sw = 280;
        if (mouseX >= sx-20 && mouseX <= sx+sw+20 && mouseY >= sy-20 && mouseY <= sy+20) {
            isDraggingSlider = true;
            updateSpeed();
            return;
        }

        selectedBody = null;
        float minDist = 50;
        for (Planet p : planets) {
            float d = dist(mouseX, mouseY, p.sx, p.sy);
            if (d < max(p.size * 3, 30)) { selectedBody = p; minDist = d; }
        }
        if (dist(mouseX, mouseY, sunSX, sunSY) < 50) selectedBody = "SUN";
        
        if (mouseEvent.getCount() == 2 && selectedBody instanceof Planet) cam.focusOn((Planet)selectedBody);
    }

    public void mouseDragged() {
        if (isDraggingSlider) { updateSpeed(); return; }
        if (mouseButton == LEFT) {
            cam.targetRotY += (mouseX - pmouseX) * 0.01f;
            cam.targetRotX = constrain(cam.targetRotX + (mouseY - pmouseY) * 0.01f, -PI/2.1f, PI/2.1f);
        } else cam.pan(mouseX - pmouseX, mouseY - pmouseY);
    }

    public void mouseReleased() { isDraggingSlider = false; }
    
    void updateSpeed() {
        float t = constrain((mouseX - (width/2 - 140)) / 280.0f, 0, 1);
        time.simSpeed = 0.1f + t * 49.9f;
    }

    public void mouseWheel(MouseEvent e) { cam.zoom(e.getCount()); }

    public void keyPressed() {
        if (key == ' ') time.paused = !time.paused;
        if (key == 'g' || key == 'G') showGrid = !showGrid;
        if (key == '+' || key == '=') time.simSpeed = min(50, time.simSpeed + 0.5f);
        if (key == '-' || key == '_') time.simSpeed = max(0.1f, time.simSpeed - 0.5f);
        if (key == ESC) { selectedBody = null; cam.reset(); key = 0; }
        if (key == 'f' && selectedBody instanceof Planet) cam.focusOn((Planet)selectedBody);
    }

    class Planet {
        String name, realOrbit, realDiam, realPeriod; 
        float orbitRadius, size, angle, speed, inclination; int col;
        Moon[] moons; float wx, wy, wz, sx, sy, localScale = 1.0f;

        Planet(String n, float orb, float s, int c, float spd, float inc, int mCount, String ro, String rd, String rp) {
            name = n; orbitRadius = orb; size = s; col = c; speed = spd; inclination = inc;
            moons = new Moon[mCount]; realOrbit = ro; realDiam = rd; realPeriod = rp;
        }

        void update(float simSpeed) {
            if (!time.paused) angle += speed * simSpeed * 0.1f;
            float r = orbitRadius;
            wx = cos(angle) * r;
            wy = sin(inclination) * sin(angle) * r;
            wz = sin(angle) * r;
            sx = screenX(wx, wy, wz); sy = screenY(wx, wy, wz);
        }

        void display() {
            noFill(); stroke(255, 30);
            beginShape();
            for(int i=0; i<=360; i+=5) {
                float a = radians(i);
                vertex(cos(a)*orbitRadius, sin(inclination)*sin(a)*orbitRadius, sin(a)*orbitRadius);
            }
            endShape();
            pushMatrix();
            translate(wx, wy, wz); specular(50); shininess(10); fill(col); noStroke();
            scale(localScale); sphere(size);
            for (Moon m : moons) m.display(time.paused, time.simSpeed);
            popMatrix();
        }
    }

    class Moon {
        String name; float orbitRadius, size, angle, speed; int col;
        Moon(String n, float orb, float s, int c, float spd) {
            name = n; orbitRadius = orb; size = s; col = c; speed = spd; angle = random(TWO_PI);
        }
        void display(boolean p, float s) {
            if(!p) angle += speed * s * 0.2f;
            pushMatrix();
            translate(cos(angle)*orbitRadius, 0, sin(angle)*orbitRadius);
            fill(col); sphere(size);
            popMatrix();
        }
    }

    class Asteroid {
        float orbit = random(300, 360), angle = random(TWO_PI), speed = random(0.001f, 0.004f), inc = random(-0.1f, 0.1f);
        void display(float t) {
            float a = angle + t * speed; 
            point(cos(a)*orbit, sin(inc)*sin(a)*orbit, sin(a)*orbit);
        }
    }

    class Camera {
        float rotX = 0.4f, rotY = 0, zoom = 1.0f, targetRotX = 0.4f, targetRotY = 0, targetZoom = 1.0f;
        PVector focus = new PVector(0,0,0), targetFocus = new PVector(0,0,0);
        void update() {
            rotX = lerp(rotX, targetRotX, 0.1f); rotY = lerp(rotY, targetRotY, 0.1f);
            zoom = lerp(zoom, targetZoom, 0.1f); focus.lerp(targetFocus, 0.1f);
        }
        void apply() {
            translate(width/2, height/2, 0); scale(zoom); rotateX(rotX); rotateY(rotY);
            translate(-focus.x, -focus.y, -focus.z);
        }
        void focusOn(Planet p) { targetFocus.set(p.wx, p.wy, p.wz); targetZoom = 2.5f; }
        void reset() { targetFocus.set(0,0,0); targetZoom = 1.0f; targetRotX = 0.4f; }
        void zoom(float d) { targetZoom = constrain(targetZoom - d*0.05f, 0.2f, 10); }
        void pan(float dx, float dy) { targetFocus.add(dx, dy, 0); }
    }

    class TimeSystem {
        float simSpeed = 1.0f, totalDays = 0; boolean paused = false;
        void update() { if(!paused) totalDays += simSpeed * 0.1f; }
        String getDateString() {
            int y = 2025 + (int)(totalDays/365);
            return "DATE: " + y + "/" + nf((int)((totalDays%365)/30)+1, 2) + "/" + nf((int)(totalDays%30)+1, 2);
        }
    }

    public static void main(String[] args) { PApplet.main("SolarSystem"); }
}