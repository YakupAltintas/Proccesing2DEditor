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
    
    boolean[] keys = new boolean[256];
    boolean shiftPressed = false;
    
    Object selectedBody = null; 
    float sunSX, sunSY;
    int lastClickTime = 0;
    String lastClickedName = "";
    boolean isDraggingSlider = false;

    int uiBG = color(8, 12, 30, 210);
    int uiBorder = color(40, 80, 180, 150);
    int uiText = color(200, 220, 255);
    int accent = color(0, 160, 255);

    public void settings() { size(1200, 800, P3D); smooth(8); }

    public void setup() {
        cam = new Camera();
        time = new TimeSystem();
        
        // Kamera Başlangıç (180 derece ters ve 45 derece üst açı)
        cam.camYaw = PI;
        cam.camPitch = PI/4;
        float startDist = 1000;
        cam.camPos.set(
            -startDist * sin(cam.camYaw) * cos(cam.camPitch),
            -startDist * sin(cam.camPitch),
            -startDist * cos(cam.camYaw) * cos(cam.camPitch)
        );

        planets[0] = new Planet("Merkür", 100, 6, 0xFFA9A9A9, 0.0408f, 0.122f, 0, "57.9M", "4,879", "88");
        planets[1] = new Planet("Venüs",  150, 9, 0xFFFFF0B4, 0.0162f, 0.059f, 0, "108.2M", "12,104", "224.7");
        planets[2] = new Planet("Dünya",  210, 8, 0xFF4682B4, 0.0100f, 0.000f, 1, "149.6M", "12,742", "365.2");
        planets[3] = new Planet("Mars",   270, 5, 0xFFBC4A3C, 0.0053f, 0.032f, 2, "227.9M", "6,779", "687");
        planets[4] = new Planet("Jüpiter",390, 35, 0xFFC8A064, 0.0008f, 0.023f, 4, "778.6M", "139,820", "4,333");
        planets[5] = new Planet("Satürn", 490, 28, 0xFFD2B464, 0.0003f, 0.043f, 1, "1.4B", "116,460", "10,759");
        planets[6] = new Planet("Uranüs", 570, 16, 0xFF64C8D2, 0.0001f, 0.013f, 1, "2.9B", "50,724", "30,687");
        planets[7] = new Planet("Neptün", 640, 15, 0xFF3264C8, 0.00006f, 0.031f, 1, "4.5B", "49,244", "60,190");

        for(int i=0; i<8; i++) planets[i].angle = i * 1.5f;
        
        planets[2].moons[0] = new Moon("Ay", 22, 2, 0xFFC8C8C8, 0.037f);
        planets[3].moons[0] = new Moon("Phobos", 12, 1, 0xFF967850, 0.316f);
        planets[3].moons[1] = new Moon("Deimos", 18, 1, 0xFF8C7864, 0.080f);
        planets[4].moons[0] = new Moon("Io", 55, 2, 0xFFFFDC64, 0.056f);
        planets[4].moons[1] = new Moon("Europa", 70, 2, 0xFFD2B48C, 0.028f);
        planets[4].moons[2] = new Moon("Ganymede", 74, 3, 0xFFB4A082, 0.014f);
        planets[4].moons[3] = new Moon("Callisto", 90, 3, 0xFF78645A, 0.006f);
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

        pushMatrix();
        cam.apply();

        ambientLight(20, 20, 40);
        pointLight(255, 230, 180, 0, 0, 0);
        lightFalloff(1.0f, 0.0f, 0.000001f);

        noLights();
        for (int i=0; i<600; i++) {
            stroke(stars[i][3]); strokeWeight(1); point(stars[i][0], stars[i][1], stars[i][2]);
        }
        if (showGrid) drawGrid();

        noLights(); emissive(255, 200, 50); fill(255, 200, 50); noStroke();
        sphere(60); sunSX = screenX(0,0,0); sunSY = screenY(0,0,0);
        emissive(0); lights();

        for (Asteroid a : belt) a.display(time.totalDays);
        for (Planet p : planets) {
            p.update(time.simSpeed);
            p.display();
        }
        popMatrix();

        drawOverlays();
    }

    void drawOverlays() {
        hint(DISABLE_DEPTH_TEST);
        camera();
        
        noFill();
        for(int i = 1; i <= 4; i++) {
            stroke(255, 200, 50, 60.0f/i); strokeWeight(i * 3);
            ellipse(sunSX, sunSY, 100*i*0.6f, 100*i*0.6f);
        }

        if (selectedBody != null) {
            stroke(255, 220, 0); strokeWeight(2); noFill();
            if (selectedBody instanceof Planet) {
                Planet p = (Planet) selectedBody;
                ellipse(p.sx, p.sy, p.size*5, p.size*5);
                drawInfoPanel(p);
            } else if (selectedBody.equals("SUN")) {
                ellipse(sunSX, sunSY, 150, 150);
            }
        }

        if (cam.leftMouseHeld) {
            fill(0, 20, 0, 180); noStroke();
            rect(width/2 - 150, 8, 300, 26, 6);
            fill(0, 255, 100); textSize(11); textAlign(CENTER);
            text("🎮 SOL TIK BASILI  |  WASD: Uç  |  SHIFT: Hızlı  |  Q/E: Dikey", width/2, 26);
            textAlign(LEFT);
        }

        drawSpeedPanel();
        drawControlsHelp();
        drawRightList();
        hint(ENABLE_DEPTH_TEST);
    }

    void drawInfoPanel(Planet p) {
        fill(uiBG); stroke(uiBorder); rect(20, 20, 260, 250, 8);
        fill(accent); textSize(16); text("● " + p.name.toUpperCase(), 40, 45);
        stroke(uiBorder); line(30, 55, 270, 55);
        fill(uiText); textSize(11);
        text("Yörünge: " + p.realOrbit + " km", 40, 75);
        text("Çap: " + p.realDiam + " km", 40, 90);
        text("Periyot: " + p.realPeriod + " gün", 40, 105);
        text("Uydu: " + p.moons.length, 40, 120);
        line(30, 135, 270, 135);
        text("Dönüşüm Matrisi (3x3):", 40, 150);
        text(String.format("[ %.2f  %.2f  %.0f ]", cos(p.angle), -sin(p.angle), p.sx), 45, 170);
        text(String.format("[ %.2f  %.2f  %.0f ]", sin(p.angle),  cos(p.angle), p.sy), 45, 185);
        text("[ 0.00  0.00  1.00 ]", 45, 200);
    }

    void drawSpeedPanel() {
        int sw = 280, sx = width/2 - sw/2, sy = height - 50;
        fill(uiBG); stroke(uiBorder); rect(width/2-200, height-80, 400, 60, 30);
        fill(50); noStroke(); rect(sx, sy, sw, 10, 5);
        fill(accent); rect(sx, sy, map(time.simSpeed, 0.1f, 50, 0, sw), 10, 5);
        fill(uiText); textAlign(CENTER);
        text("HIZ: " + nf(time.simSpeed, 1, 1) + "x  |  " + time.getDateString(), width/2, sy - 10);
        textAlign(LEFT);
    }

    void drawControlsHelp() {
        int hX = width - 220, hY = height - 190, hW = 210, hH = 170;
        fill(uiBG); stroke(uiBorder); rect(hX, hY, hW, hH, 6);
        fill(accent); textSize(11); text("⌨ KONTROLLER", hX + 10, hY + 18);
        stroke(uiBorder); line(hX + 8, hY + 24, hX + hW - 8, hY + 24);
        String[][] c = { 
            {"SOL TIK + WASD", "Serbest uç"}, {"SOL TIK + fare", "Bak"}, 
            {"SHIFT", "Hızlı hareket"}, {"Q / E", "Aşağı / Yukarı"}, {"SCROLL", "Zoom"}, 
            {"F", "Odaklan"}, {"ÇİFT TIK", "Gezegene fokus"}, {"SPACE", "Duraklat"}, 
            {"G", "Izgara"}, {"U", "Geri Al"}, {"+/-", "Sim hızı"}
        };
        for(int i=0; i<c.length; i++) {
            fill(255, 220, 80); text(c[i][0], hX + 10, hY + 38 + i*12);
            fill(uiText); text(c[i][1], hX + 105, hY + 38 + i*12);
        }
    }

    void drawRightList() {
        int pW = 180, pX = width - pW - 10, pY = 10, pH = height - 210, iH = 28;
        fill(uiBG); stroke(uiBorder); rect(pX, pY, pW, pH, 8);
        fill(accent); textAlign(CENTER); text("◈ GEZEGENLER", pX + pW/2, pY + 20);
        line(pX + 10, pY + 28, pX + pW - 10, pY + 28);
        textAlign(LEFT);
        float sY = pY + 45; fill(255, 200, 50); ellipse(pX + 18, sY, 12, 12);
        fill(selectedBody != null && selectedBody.equals("SUN") ? color(255,220,0) : uiText);
        text("☀ Güneş", pX + 32, sY + 4);
        for (int i=0; i<8; i++) {
            float iY = pY + 45 + (i+1) * iH;
            fill(planets[i].col); ellipse(pX + 18, iY, 8, 8);
            fill(selectedBody == planets[i] ? color(255,220,0) : uiText); text(planets[i].name, pX + 32, iY + 4);
        }
    }

    void drawGrid() {
        stroke(255, 20); for(int i=-1000; i<=1000; i+=100) { line(i, 0, -1000, i, 0, 1000); line(-1000, 0, i, 1000, 0, i); }
        stroke(255, 0, 0, 60); line(0,0,0, 200,0,0); stroke(0, 255, 0, 60); line(0,0,0, 0,200,0); stroke(0, 0, 255, 60); line(0,0,0, 0,0,200);
    }

    public void mousePressed() {
        int sx = width/2 - 140, sy = height - 55, sw = 280;
        if (mouseX >= sx && mouseX <= sx+sw && mouseY >= sy-15 && mouseY <= sy+15) { isDraggingSlider = true; updateSpeed(); return; }
        
        int pW = 180, pX = width - pW - 10, pY = 10, iH = 28;
        if (mouseX > pX && mouseX < pX + pW) {
            if (mouseY > pY + 30 && mouseY < pY + 60) { selectedBody = "SUN"; return; }
            for (int i = 0; i < planets.length; i++) {
                float iY = pY + 45 + (i + 1) * iH;
                if (mouseY > iY - iH/2 && mouseY < iY + iH/2) { 
                    selectedBody = planets[i]; 
                    cam.focusOnPlanet(planets[i]); 
                    return; 
                }
            }
            return;
        }

        if (mouseButton == LEFT) {
            int now = millis();
            Planet clicked = null;
            for (Planet p : planets) {
                if (dist(mouseX, mouseY, p.sx, p.sy) < max(p.size * 2, 20)) { clicked = p; break; }
            }
            Object clickedFinal = clicked;
            if (clickedFinal == null && dist(mouseX, mouseY, sunSX, sunSY) < 60) clickedFinal = "SUN";

            if (clickedFinal != null) {
                String cName = (clickedFinal instanceof Planet) ? ((Planet)clickedFinal).name : "SUN";
                if (now - lastClickTime < 400 && lastClickedName.equals(cName)) {
                    if (clickedFinal instanceof Planet) cam.focusOnPlanet((Planet)clickedFinal);
                }
                selectedBody = clickedFinal;
                lastClickTime = now;
                lastClickedName = cName;
            } else {
                if (mouseX < width - 200) cam.leftMouseHeld = true;
            }
        }
    }

    public void mouseReleased() {
        if (mouseButton == LEFT) cam.leftMouseHeld = false;
        isDraggingSlider = false;
    }

    public void mouseDragged() {
        if (isDraggingSlider) { updateSpeed(); return; }
        if (cam.leftMouseHeld && mouseX < width - 200) {
            cam.isFocusing = false; 
            cam.camYaw -= (mouseX - pmouseX) * 0.005f;
            cam.camPitch -= (mouseY - pmouseY) * 0.005f;
            cam.camPitch = constrain(cam.camPitch, -PI/2 + 0.05f, PI/2 - 0.05f);
        }
    }
    
    public void mouseWheel(MouseEvent e) {
        float fx = -sin(cam.camYaw) * cos(cam.camPitch);
        float fy = -sin(cam.camPitch);
        float fz = -cos(cam.camYaw) * cos(cam.camPitch);
        float spd = shiftPressed ? 80 : 25;
        cam.camPos.x -= fx * e.getCount() * spd;
        cam.camPos.y -= fy * e.getCount() * spd;
        cam.camPos.z -= fz * e.getCount() * spd;
    }

    public void keyPressed() {
        if (keyCode < 256) keys[keyCode] = true;
        if (keyCode == SHIFT) shiftPressed = true;
        if (key == ' ') time.paused = !time.paused;
        if (key == 'g' || key == 'G') showGrid = !showGrid;
        if (key == '+' || key == '=') time.simSpeed = min(100, time.simSpeed + 0.5f);
        if (key == '-' || key == '_') time.simSpeed = max(0.1f, time.simSpeed - 0.5f);
        if (key == ESC) { selectedBody = null; key = 0; }
        if ((key == 'f' || key == 'F') && selectedBody instanceof Planet) cam.focusOnPlanet((Planet)selectedBody);
    }

    public void keyReleased() {
        if (keyCode < 256) keys[keyCode] = false;
        if (keyCode == SHIFT) shiftPressed = false;
    }

    void updateSpeed() { float t = constrain((mouseX - (width/2 - 140)) / 280.0f, 0, 1); time.simSpeed = 0.1f + t * 49.9f; }

    class Planet {
        String name, realOrbit, realDiam, realPeriod; 
        float orbitRadius, size, angle, speed, inclination; int col;
        Moon[] moons; float wx, wy, wz, sx, sy;
        Planet(String n, float orb, float s, int c, float spd, float inc, int mCount, String ro, String rd, String rp) {
            name = n; orbitRadius = orb; size = s; col = c; speed = spd; inclination = inc;
            moons = new Moon[mCount]; realOrbit = ro; realDiam = rd; realPeriod = rp;
        }
        void update(float simSpeed) {
            if (!time.paused) angle += speed * simSpeed * 0.1f;
            wx = cos(angle) * orbitRadius; wy = sin(inclination) * sin(angle) * orbitRadius; wz = sin(angle) * orbitRadius;
            sx = screenX(wx, wy, wz); sy = screenY(wx, wy, wz);
        }
        void display() {
            noFill(); stroke(255, 30); beginShape();
            for(int i=0; i<=360; i+=10) vertex(cos(radians(i))*orbitRadius, sin(inclination)*sin(radians(i))*orbitRadius, sin(radians(i))*orbitRadius);
            endShape();
            pushMatrix(); translate(wx, wy, wz); specular(50); shininess(10); fill(col); noStroke(); sphere(size);
            for (Moon m : moons) m.display(time.paused, time.simSpeed); popMatrix();
        }
    }

    class Moon {
        String name; float orbitRadius, size, angle, speed; int col;
        Moon(String n, float orb, float s, int c, float spd) {
            name = n; orbitRadius = orb; size = s; col = c; speed = spd; angle = random(TWO_PI);
        }
        void display(boolean p, float s) {
            if(!p) angle += speed * s * 0.2f;
            pushMatrix(); translate(cos(angle)*orbitRadius, 0, sin(angle)*orbitRadius); fill(col); sphere(size); popMatrix();
        }
    }

    class Asteroid {
        float orbit = random(310, 350), angle = random(TWO_PI), speed = random(0.001f, 0.004f), inc = random(-0.08f, 0.08f);
        void display(float t) { float a = angle + t * speed; point(cos(a)*orbit, sin(inc)*sin(a)*orbit, sin(a)*orbit); }
    }

    class Camera {
        PVector camPos = new PVector(0, -400, 800);
        float camYaw = 0, camPitch = 0.4f, moveSpeed = 5f;
        boolean leftMouseHeld = false;
        boolean isFocusing = false;
        float focusProgress = 0;
        PVector focusStartPos = new PVector(), focusTargetPos = new PVector();
        float focusStartYaw, focusStartPitch, focusTargetYaw, focusTargetPitch;

        void update() {
            if(isFocusing) {
                focusProgress += 0.025f;
                if(focusProgress >= 1.0f) { focusProgress = 1.0f; isFocusing = false; }
                float t = focusProgress < 0.5f ? 2 * focusProgress * focusProgress : 1 - pow(-2 * focusProgress + 2, 2) / 2;
                camPos.x = lerp(focusStartPos.x, focusTargetPos.x, t);
                camPos.y = lerp(focusStartPos.y, focusTargetPos.y, t);
                camPos.z = lerp(focusStartPos.z, focusTargetPos.z, t);
                camYaw   = lerp(focusStartYaw,   focusTargetYaw,   t);
                camPitch = lerp(focusStartPitch, focusTargetPitch, t);
            }
            if (!isFocusing && leftMouseHeld) {
                float spd = shiftPressed ? moveSpeed * 4 : moveSpeed;
                float cfx = -sin(camYaw) * cos(camPitch), cfy = -sin(camPitch), cfz = -cos(camYaw) * cos(camPitch);
                float crx = cos(camYaw), crz = -sin(camYaw);
                if(keys[87]) { camPos.x += cfx*spd; camPos.y += cfy*spd; camPos.z += cfz*spd; }
                if(keys[83]) { camPos.x -= cfx*spd; camPos.y -= cfy*spd; camPos.z -= cfz*spd; }
                if(keys[65]) { camPos.x -= crx*spd; camPos.y -= 0; camPos.z -= crz*spd; }
                if(keys[68]) { camPos.x += crx*spd; camPos.y += 0; camPos.z += crz*spd; }
                if(keys[69]) camPos.y -= spd; if(keys[81]) camPos.y += spd;
            }
        }

        void apply() {
            float fx = -sin(camYaw) * cos(camPitch);
            float fy = -sin(camPitch);
            float fz = -cos(camYaw) * cos(camPitch);
            camera(camPos.x, camPos.y, camPos.z, camPos.x + fx, camPos.y + fy, camPos.z + fz, 0, 1, 0);
        }

        void focusOnPlanet(Planet p) {
            float approachDist = p.size * 8 + 80;
            float targetYaw   = atan2(-p.wx, -p.wz);
            float targetPitch = PI / 6;
            float tx = p.wx - approachDist * (-sin(targetYaw)) * cos(targetPitch);
            float ty = p.wy - approachDist * (-sin(targetPitch));
            float tz = p.wz - approachDist * (-cos(targetYaw)) * cos(targetPitch);
            focusStartPos.set(camPos); focusStartYaw = camYaw; focusStartPitch = camPitch;
            focusTargetPos.set(tx, ty, tz); focusTargetYaw = targetYaw; focusTargetPitch = targetPitch;
            isFocusing = true; focusProgress = 0;
        }
    }

    class TimeSystem {
        float simSpeed = 1.0f, totalDays = 0; boolean paused = false;
        void update() { if(!paused) totalDays += simSpeed * 0.1f; }
        String getDateString() {
            int y = 2025 + (int)(totalDays/365);
            return y + "/" + nf((int)((totalDays%365)/30)+1, 2) + "/" + nf((int)(totalDays%30)+1, 2);
        }
    }

    public static void main(String[] args) { PApplet.main("SolarSystem"); }
}
