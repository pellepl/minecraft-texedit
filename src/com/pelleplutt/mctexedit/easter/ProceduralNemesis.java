package com.pelleplutt.mctexedit.easter;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.pelleplutt.util.Log;

public class ProceduralNemesis extends JPanel {
  static boolean DBGPHYS = false;
  static final int LOOP_DELAY_MS = 20;
  static final float COLLISION_MIN_PENE = 0.1f;
  static final int WIDTH = 600;
  static final int HEIGHT = 400;
  
  static final int KEY_UP = 0;
  static final int KEY_DOWN = 1;
  static final int KEY_LEFT = 2;
  static final int KEY_RIGHT = 3;
  static final int KEY_SPACE = 4;
  static final int KEY_ESC = 5;
  
  boolean keyStatus[] = new boolean[8];
  Player player;
  Thing thing; // TODO remove
  java.util.List<Sprite> sprites = new ArrayList<Sprite>();
  java.util.List<Sprite> deadSprites = new ArrayList<Sprite>();
  BufferedImage screen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB); 
  
  public ProceduralNemesis() {
    player = new Player();
    player.move(32, HEIGHT/2);
    sprites.add(player);
    thing = new Thing();
    thing.move(200, 100);
    sprites.add(thing);
    Sprite enemy = new EnemyFighter();
    enemy.move(400,300);
    sprites.add(enemy);
    
    registerKey("W", KEY_UP);
    registerKey("S", KEY_DOWN);
    registerKey("A", KEY_LEFT);
    registerKey("D", KEY_RIGHT);
    registerKey("SPACE", KEY_SPACE);
    registerKey("ESCAPE", KEY_ESC);
    setFocusable(true);
    requestFocus();
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // User interaction, loops, events and stuff
  //
  
  void registerKey(String keyName, final int keyIndex) {
    getInputMap().put(KeyStroke.getKeyStroke("pressed " + keyName), "p"+keyName);
    getActionMap().put("p"+keyName, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent a) {
        keyStatus[keyIndex] = true;
      }});
    getInputMap().put(KeyStroke.getKeyStroke("released " + keyName), "r"+keyName);
    getActionMap().put("r"+keyName, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent a) {
        keyStatus[keyIndex] = false;
      }});
  }
  
  static final float PLAYER_STEP = 4;
  int fireCoolOff = 0;
  void updateInteractions() {
    float dx = player.shape.vstick.v2.x -player.shape.vstick.v1.x; 
    float dy = player.shape.vstick.v2.y -player.shape.vstick.v1.y;
    dx *= PLAYER_STEP * player.shape.vstick.idist;
    dy *= PLAYER_STEP * player.shape.vstick.idist;
    
    float pdx = 0;
    float pdy = 0;

    boolean diag = false;
    if (keyStatus[KEY_UP]) {
      pdx += dy; pdy += -dx;
    }
    if (keyStatus[KEY_DOWN]) {
      pdx += -dy; pdy += dx;
    }
    if (keyStatus[KEY_LEFT]) {
      pdx += -dx; pdy += -dy;
      diag |= (keyStatus[KEY_UP] || keyStatus[KEY_DOWN]); 
    }
    if (keyStatus[KEY_RIGHT]) {
      pdx += dx; pdy += dy;
      diag |= (keyStatus[KEY_UP] || keyStatus[KEY_DOWN]); 
    }
    if (keyStatus[KEY_ESC]) {
      Log.println("---------------------------");
      thing.life = 1;
      thing = new Thing();
      thing.move(200, 100);
      sprites.add(thing);
      keyStatus[KEY_ESC] = false;
    }
    if (diag) {
      pdx *= 0.7071f; // 1/sqrt(2);
      pdy *= 0.7071f;
    }
    player.move(pdx, pdy);
    if (keyStatus[KEY_SPACE]) {
      if (fireCoolOff == 0) {
        playerFire(10,0);
        fireCoolOff = 10;
      } else {
        fireCoolOff--;
      }
    } else {
      fireCoolOff = 0;
    }
  }
  
  void playerFire(int dx, int dy) {
    Shot shot = new Shot();
    float sdx = (player.shape.vstick.v2.x - player.shape.vstick.v1.x)*player.shape.vstick.idist;
    float sdy = (player.shape.vstick.v2.y - player.shape.vstick.v1.y)*player.shape.vstick.idist;
    shot.move(
        player.shape.vstick.v1.x + sdx*(player.shape.vstick.dist + shot.shape.vstick.dist),
        player.shape.vstick.v1.y + sdy*(player.shape.vstick.dist + shot.shape.vstick.dist));
    shot.push(sdx*16f, sdy*16f);
    shot.life = 40;
    shot.shape.vstick.setInverseFriction(0.975f);
    sprites.add(shot);
  }

  //////////////
  // Main game loop
  Vec2f phyMTVref = new Vec2f();
  Vec2f phyMTVtest = new Vec2f();
  Vec2f phyMTV = new Vec2f();
  Vec2f phyColl = new Vec2f();
  public void loop() {
    updateInteractions();
    for (Sprite sprite : sprites) {
      sprite.update();
      if (sprite.gone) deadSprites.add(sprite);
    }
    while (!deadSprites.isEmpty()) {
      sprites.remove(deadSprites.remove(0));
    }

    final int spriteCount = sprites.size();
    int maxIterations = 6;
    while (maxIterations-- > 0) {
      boolean haveCollisions = false;
      for (int i = 0; i < spriteCount; i++) {
        Sprite sref = sprites.get(i);
        if (sref.shape == null) continue;
        for (int j = i + 1; j < spriteCount; j++) {
          Sprite stest = sprites.get(j);
          if (stest.shape == null) continue;
          if ((sref.shape.collisionGroups & stest.shape.collisionFilters) +
              (sref.shape.collisionFilters & stest.shape.collisionGroups) != 0) continue;
          float pene = Collider.collides(sref, stest, phyMTVref, phyMTVtest, phyMTV, phyColl);
          if (pene > 0) {
            haveCollisions = true;
            if (DBGPHYS) sprites.add(new DbgMark(Color.white, phyColl));

            sref.shape.impulse(phyColl, phyMTV, -1f);
            stest.shape.impulse(phyColl, phyMTV, 1f);
            stickConstraint(sref.shape.vstick, 1f);
            stickConstraint(stest.shape.vstick, 1f);
            sref.shape.shapify();
            stest.shape.shapify();
            phyMTV.neg();
            
//            if (DBGPHYS) Log.println(maxIterations + " coll [" + i + "," + j + "] "
//                + "MTV1:" + phyMTVref.len() + "/" + sref.shape.vstick.dist + ",a:" + (int)Math.toDegrees(phyMTVref.angle()) + 
//                "  MTV2:" + phyMTVtest.len() + "/" + stest.shape.vstick.dist  + ",a:" + (int)Math.toDegrees(phyMTVtest.angle()) +
//                "  MTV:" + phyMTV.len() + ",a:" + (int)Math.toDegrees(phyMTV.angle()));

            if (DBGPHYS) sprites.add(new DbgLine(Color.blue, stest.shape.vstick,
              phyMTV.assign(new Vec2f()).mul(5f).add(stest.shape.vstick)));
            if (DBGPHYS) sprites.add(new DbgMark(Color.blue, stest.shape.vstick));
            if (DBGPHYS) sprites.add(new DbgLine(Color.red, sref.shape.vstick,
              phyMTV.assign(new Vec2f()).neg().mul(5f).add(sref.shape.vstick)));
            if (DBGPHYS) sprites.add(new DbgMark(Color.red, sref.shape.vstick));
          }
        }
      }
      if (!haveCollisions) {
        break;
      }
    } // while maxIterations
    angleConstraint(player.shape.vstick, 0f, 0.02f);
    for (int i = 0; i < spriteCount; i++) {
      Sprite spr = sprites.get(i);
      if (spr.shape == null) continue;
      stickConstraint(spr.shape.vstick, 1f);
      spr.shape.vstick.update(1f);
      spr.shape.shapify();
      Collider.precalcVelocityOOB(spr);
    }
  }
  
  @Override
  public void paint(Graphics g) {
    g.drawImage(screen, 0, 0, this);
  }

  Stroke stroke = new BasicStroke(2);
  
  public void draw(Graphics2D g) {
    g.setColor(Color.black);
    g.fillRect(0, 0, WIDTH, HEIGHT);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setStroke(stroke);
    for (Sprite sprite : sprites) {
      sprite.paint(g);
    }
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // Graphics and such
  //

  class Player extends Sprite {
    public Player() {
      shape = new Shape();
      shape.addVertex(new Vec2f( 36,  0));
      shape.addVertex(new Vec2f(  4, -8));
      shape.addVertex(new Vec2f(  0,  0));
      shape.addVertex(new Vec2f(  4,  8));
      shape.solidify(36);
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(Color.cyan);
      super.paint(g);
    }
  }
  class Thing extends Sprite {
    public Thing() {
      shape = new Shape();
      shape.addVertex(new Vec2f( 80,  40));
      shape.addVertex(new Vec2f( 80, -40));
      shape.addVertex(new Vec2f(  0, -40));
      shape.addVertex(new Vec2f(  0,  40));
      shape.solidify(80);
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(Color.green);
      super.paint(g);
    }
  }
  class EnemyFighter extends Sprite {
    public EnemyFighter() {
      shape = new Shape();
      shape.addVertex(new Vec2f( 40,   0));
      shape.addVertex(new Vec2f( 32, -10));
      shape.addVertex(new Vec2f(  0,   0));
      shape.addVertex(new Vec2f( 32,  10));
      shape.solidify(40);
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(Color.pink);
      super.paint(g);
    }
  }
  class Shot extends Sprite {
    public Shot() {
      shape = new Shape();
      shape.addVertex(new Vec2f( 10, 0));
      shape.addVertex(new Vec2f( 5,-3));
      shape.addVertex(new Vec2f( 0, 0));
      shape.addVertex(new Vec2f( 5, 3));
      shape.solidify(10);
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(Color.yellow);
      super.paint(g);
    }
  }
  
  class DbgLine extends Sprite {
    float x,y,tx, ty; Color c;
    public DbgLine(Color c, Vec2f from, Vec2f to) {
      this.c = c; x = from.x; y = from.y; tx = to.x; ty = to.y; life = 50;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(colMix(c, colTrans, life/50f));
      g.drawLine((int)x, (int)y, (int)tx, (int)ty);
      g.fillOval((int)tx-2, (int)ty-2, 4,4);
    }
  }
  
  class DbgMark extends Sprite {
    float x,y;
    Color c;
    public DbgMark(Color c, Vec2f p) {
      this.c = c; x = p.x; y = p.y; life = 50;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(colMix(c, colTrans, life/50f));
      g.drawLine((int)(x-3), (int)(y-3), (int)(x+3), (int)(y+3));
      g.drawLine((int)(x+3), (int)(y-3), (int)(x-3), (int)(y+3));
    }
  }
  
  abstract class Sprite {
    int life;
    boolean gone = false;
    Shape shape;
    Shape oobShape = new Shape(4);
    final Color colTrans = new Color(0,0,0,0);
    public void paint(Graphics2D g) {
      paintShape(g);
      if (DBGPHYS) paintShapeVerlet(g, 0, 0);
      //paintShapeNormals(g, 0, 0);
      g.setColor(Color.gray);
      /*if (DBGPHYS)*/ paintShape(g, oobShape);
    }
    public void update() { 
      if (life > 0) {
        life--; 
        if (life == 0) gone = true; 
      }
    }
    public void move(float dx, float dy) {
      shape.move(dx, dy);
      shape.shapify();
    }
    public void push(float dx, float dy) {
      shape.vstick.push(dx, dy);
    }
    public void paintShape(Graphics2D g, Shape s) { paintShape(g,s,0,0); }
    public void paintShape(Graphics2D g) { paintShape(g,shape,0,0); }
    public void paintShape(Graphics2D g, Shape s, float dx, float dy) {
      if (s == null) return;
      final int vcnt = s.countVertices();
      for (int i = 0; i <= vcnt; i++) {
        Vec2f v1 = s.getVertex(i % vcnt);
        Vec2f v2 = s.getVertex((i+1) % vcnt);
        g.drawLine((int)(v1.x+dx), (int)(v1.y+dy), (int)(v2.x+dx), (int)(v2.y+dy));
      }
    }
    public void paintShapeVerlet(Graphics2D g, float dx, float dy) {
      g.setColor(Color.DARK_GRAY);
      g.drawLine((int)(shape.vstick.v1.x + dx), (int)(shape.vstick.v1.y + dy),
          (int)(shape.vstick.v2.x + dx), (int)(shape.vstick.v2.y + dy));
      g.setColor(Color.red);
      g.drawRect((int)(shape.vstick.v1.x + dx)-3, (int)(shape.vstick.v1.y + dy)-3,6,6);
      g.setColor(Color.green);
      g.drawRect((int)(shape.vstick.v2.x + dx)-3, (int)(shape.vstick.v2.y + dy)-3,6,6);
    }
    public void paintShapeNormals(Graphics2D g, float dx, float dy) {
      if (shape == null) return;
      final int vcnt = shape.countVertices();
      for (int i = 0; i <= vcnt; i++) {
        Vec2f v1 = shape.getVertex(i % vcnt);
        Vec2f v2 = shape.getVertex((i+1) % vcnt);
        float cx = 0.5f*(v1.x + v2.x);
        float cy = 0.5f*(v1.y + v2.y);
        float nx = -(v2.y - v1.y);
        float ny =  (v2.x - v1.x);
        g.drawLine((int)(cx+dx), (int)(cy+dy), (int)(cx+nx+dx), (int)(cy+ny+dy));
      }
    }
    public Color colMix(Color c1, Color c2, float t) {
      float it = (1f-t);
      return new Color(
          (int)(t*c1.getRed() + it*c2.getRed()) & 0xff,
          (int)(t*c1.getGreen() + it*c2.getGreen()) & 0xff,
          (int)(t*c1.getBlue() + it*c2.getBlue()) & 0xff,
          (int)(t*c1.getAlpha() + it*c2.getAlpha()) & 0xff);
    }
  } // class Sprite
  
  /////////////////////////////////////////////////////////////////////////////
  // Physics and maths and such
  //

  public void stickConstraint(VStick stick, float dt) {
    float dx = stick.v2.x-stick.v1.x;
    float dy = stick.v2.y-stick.v1.y;
    final float cdist = (float)Math.sqrt(dx*dx + dy*dy);
    final float diff = (stick.dist - cdist) * stick.idist;
    final float c =  diff * 0.5f * dt;
    float fx = dx * c;
    float fy = dy * c;
    stick.v1.x -= fx;
    stick.v1.y -= fy;
    stick.v2.x += fx;
    stick.v2.y += fy;
  }
  
  public void angleConstraint(VStick stick, float targetAngle, float dt) {
    final float curAng = (float)Math.atan2(stick.v2.y-stick.v1.y, stick.v2.x-stick.v1.x);
    final float dang = (float)(targetAngle-curAng) * 0.5f;
    final float cos = (float)Math.cos(dang);
    final float sin = (float)Math.sin(dang);
    stick.x = 0.5f*(stick.v1.x + stick.v2.x);
    stick.y = 0.5f*(stick.v1.y + stick.v2.y);
    float fx = (stick.v1.x - stick.x) * cos + (stick.v1.y - stick.y) * -sin + stick.dist*0.5f; 
    float fy = (stick.v1.x - stick.x) * sin + (stick.v1.y - stick.y) * cos;
    fx *= dt;
    fy *= dt;
    stick.v1.x -= fx;
    stick.v1.y -= fy;
    stick.v2.x += fx;
    stick.v2.y += fy;
  }
  
  static class Collider {
    static Projection projections[] = new Projection[4];
    static Vec2f vectors[] = new Vec2f[64];
    static {
      for (int i = 0; i < projections.length; i++) { projections[i] = new Projection(); }
      for (int i = 0; i < vectors.length; i++) { vectors[i] = new Vec2f(); }
    }
    static int vix = 0;
    static int pix = 0;

    static void precalcVelocityOOB(Sprite spr) {
      Shape s = spr.shape;
      float speedFactSq = s.vstick.speedSq() * (s.vstick.idist * s.vstick.idist); 
      if (speedFactSq > 0.125f) {
        calcVelocityOOB(s, spr.oobShape);
      }
    }
    
    static float collides(Sprite spr1, Sprite spr2, Vec2f mtv1, Vec2f mtv2, Vec2f mtv, Vec2f collision) {
      vix = 0;
      pix = 0;
      // broad phase check
      if (!intersectAABB(spr1,spr2)) return 0;
      
      // narrow phase check
      float pene = intersectSAT(spr1,spr2,mtv1,mtv2,mtv,collision); 
      return pene < COLLISION_MIN_PENE ? 0 : pene;
    }
    
    // TODO PETER handle high velocity objects
    // broad phase:  create aabb' of aabb(position) + aabb(position + velocity)
    // narrow phase: project shape on velocity vectors normal = front and back sides of obb,
    //               top and bottom of obb is velocity translation vector,
    //               use this obb to find out where collision occurs
    
    static boolean intersectAABB(Sprite spr1, Sprite spr2) {
      Shape s1 = spr1.shape;
      Shape s2 = spr2.shape;
      boolean overlapX = 
          (s1.vstick.x - s1.vstick.distDSq2 < s2.vstick.x + s2.vstick.distDSq2) &&
          (s2.vstick.x - s2.vstick.distDSq2 < s1.vstick.x + s1.vstick.distDSq2);
      if (!overlapX) return false;
      boolean overlapY = 
          (s1.vstick.y - s1.vstick.distDSq2 < s2.vstick.y + s2.vstick.distDSq2) &&
          (s2.vstick.y - s2.vstick.distDSq2 < s1.vstick.y + s1.vstick.distDSq2);
      return overlapY;
    }
    
    static void calcVelocityOOB(Shape s, Shape oob) {
      Vec2f vel = vectors[vix++];
      Vec2f velN = vectors[vix++];
      Vec2f velPN = vectors[vix++];
      Vec2f tmp = vectors[vix++];
      //vel.set(s.vstick.v1.x - s.vstick.v1.ox, s.vstick.v1.y - s.vstick.v1.oy);
      vel.set(s.vstick.x - s.vstick.ox, s.vstick.y - s.vstick.oy);
      vel.assign(velN).norm();
      velN.assign(velPN).perp();
      
      // project shape onto normalized velocity vector and perped normalized velocity vector
      final int sVertices = s.countVertices();
      float minV = Float.MAX_VALUE; 
      float maxV = Float.MIN_VALUE;
      float minPV = Float.MAX_VALUE; 
      float maxPV = Float.MIN_VALUE;
      Vec2f refVertex = s.getVertex(0); // any vertex will do
      for (int i = 1; i < sVertices; i++) {
        s.getVertex(i).assign(tmp).sub(refVertex);
        float v = velN.dot(tmp);
        float pv = velPN.dot(tmp);
        if (v < minV) minV = v;
        if (v > maxV) maxV = v;
        if (pv < minPV) minPV = pv;
        if (pv > maxPV) maxPV = pv;
      }
      minV = Math.min(minV, 0);
      minPV = Math.min(minPV, 0);

      // oob_left/right length = (maxV-minV) + velocity
      // oob_top/bottom length = (maxPV-minPV)
      float vminx  = minV  * velN.x;
      float vminy  = minV  * velN.y;
      float vmaxx  = maxV  * velN.x + vel.x;
      float vmaxy  = maxV  * velN.y + vel.y;
      float pvminx = minPV * velPN.x;
      float pvminy = minPV * velPN.y;
      float pvmaxx = maxPV * velPN.x;
      float pvmaxy = maxPV * velPN.y;
      Vec2f p1 = oob.getVertex(0);
      Vec2f p2 = oob.getVertex(1);
      Vec2f p3 = oob.getVertex(2);
      Vec2f p4 = oob.getVertex(3);
      refVertex.assign(p1).assign(p2).assign(p3).assign(p4);
      p1.add(vminx, vminy).add(pvminx, pvminy);
      p2.add(vminx, vminy).add(pvmaxx, pvmaxy);
      p3.add(vmaxx, vmaxy).add(pvmaxx, pvmaxy);
      p4.add(vmaxx, vmaxy).add(pvminx, pvminy);
    }
    
    static float intersectSAT(Sprite spr1, Sprite spr2, Vec2f mtv1, Vec2f mtv2, Vec2f mtv, Vec2f collision) {
      Shape s1 = spr1.shape;
      Shape s2 = spr2.shape;
      Vec2f nearestVertex = vectors[vix++]; 
      Vec2f es1 = vectors[vix++]; 
      Vec2f ee1 = vectors[vix++]; 
      Vec2f es2 = vectors[vix++]; 
      Vec2f ee2 = vectors[vix++]; 
      float pene1, pene2;
      pene1 = sat(s1, s2, mtv1, es1, ee1);
      if (pene1 == 0) return 0;
      pene2 = sat(s2, s1, mtv2, es2, ee2);
      if (pene2 == 0) return 0;

      mtv2.neg();
      if (pene1 < pene2) {
        s2.findNearestVertex(es1, ee1, mtv1, nearestVertex);
        s2.calcCollisionPoint(es1, ee1, nearestVertex, collision);
      } else {
        s1.findNearestVertex(es2, ee2, mtv2, nearestVertex);
        s1.calcCollisionPoint(es2, ee2, nearestVertex, collision);
      }
      
      mtv1.mul(pene1);
      mtv2.mul(pene2);
      
      if (pene1 < pene2) {
        mtv1.assign(mtv);
      } else {
        mtv2.assign(mtv);
      }
      
      return pene1 < pene2 ? pene1 : pene2;
    }

    static float sat(Shape sref, Shape sother, Vec2f mtv, Vec2f collEdgeS, Vec2f collEdgeE) {
      float minOverlap = Float.MAX_VALUE;
      float minDist = Float.MAX_VALUE;
      final int srefVertices = sref.countVertices();
      Vec2f edgeS = vectors[vix++];
      Vec2f edgeE = vectors[vix++];
      Vec2f edge = vectors[vix++];
      Vec2f posEdge= vectors[vix++];
      Vec2f axis = vectors[vix++];
      Vec2f posOther = vectors[vix++];
      Projection pref = projections[pix++];
      Projection pother = projections[pix++];
      sother.getCenter(posOther);
      for (int i = 0; i < srefVertices; i++) {
        sref.getEdgeVertices(i, edgeS, edgeE);
        posEdge.x = 0.5f*(edgeS.x + edgeE.x);
        posEdge.y = 0.5f*(edgeS.y + edgeE.y);
        edge = edgeE.sub(edgeS, edge);
        axis = edge.perp(axis).norm();
        sref.project(axis, pref);
        sother.project(axis, pother);
        float overlap;
        if ((overlap = pref.overlap(pother)) == 0) {
          return 0;
        }
        if (overlap > sref.vstick.dist) {
          // TODO figure out why this happen
          Log.println("overlap exceed sticklen " + overlap + " > " + sref.vstick.dist + ": collision ignored");
          return 0; // returning 0 here makes things a lot more robust
        }
        float dist = posEdge.sub(posOther).lenSq();
        if (overlap <= minOverlap && dist < minDist) {
          axis.assign(mtv);
          edgeS.assign(collEdgeS);
          edgeE.assign(collEdgeE);
          minOverlap = overlap;
          minDist = dist;
        }
      }
      
      return minOverlap;
    }

  } // class Collider
  
  static class Projection extends Vec2f {
    public Vec2f nearestVertex;
    float minDist = Float.MAX_VALUE;
    public Projection() {reset();}
    public void reset() { x = Float.MAX_VALUE; y = -Float.MAX_VALUE; nearestVertex = null; minDist = Float.MAX_VALUE;}
    public void addDot(float d) {
      if (d < x) x = d;
      if (d > y) y = d;
    }
    public float overlap(Projection p) {
      if (x >= p.y || p.x >= y) {
        return 0; // no overlap
      }
      return Math.max(0, Math.min(y, p.y) - Math.max(x, p.x));
    }
  } // class Projection
  
  class VPoint extends Vec2f {
    float ox, oy;
    float ifriction = 0.95f;
    float vx, vy;
    void update(float dt) {
      vx = ifriction * (x-ox);
      vy = ifriction * (y-oy);
      ox = x; oy = y;
      x += vx*dt;
      y += vy*dt;
    }
    public void push(float dx, float dy) {
      x += dx;
      y += dy;
    }
    public void rest() {
      ox = x; oy = y; vx = vy = 0;
    }
    public float speedSq() {
      float dx = x - ox;
      float dy = y - oy;
      return dx*dx+dy*dy;
    }
  } // class VPoint

  class VStick extends Vec2f {
    float ox, oy;
    VPoint v1, v2;
    float dist, idist, distDSq2;
    public VStick(float dist) {
      v1 = new VPoint();
      v2 = new VPoint();
      v1.x = -dist / 2;
      v2.x =  dist / 2;
      v1.y = v2.y = 0;
      v1.ox = v1.x; v1.oy = v1.y;
      v2.ox = v2.x; v2.oy = v2.y;
      this.x = this.y = 0;
      this.dist = dist;
      this.idist = 1f/dist;
      this.distDSq2 = dist * 0.7071f;
    }
    public void setInverseFriction(float f) {
      v1.ifriction = v2.ifriction = f;
    }
    public void push(float dx, float dy) {
      x += dx; y += dy;
      v1.push(dx,dy);
      v2.push(dx,dy);
    }
    public void rest() {
      v1.rest(); v2.rest();
      this.x = this.ox = 0.5f*(v1.x + v2.x);
      this.y = this.oy = 0.5f*(v1.y + v2.y);
    }
    public void update(float dt) {
      v1.update(dt);
      v2.update(dt);
      ox = this.x;
      oy = this.y;
      this.x = 0.5f*(v1.x + v2.x);
      this.y = 0.5f*(v1.y + v2.y);
    }
    public void move(float dx, float dy) {
      v1.x += dx;   v1.y += dy;
      v2.x += dx;   v2.y += dy;
      v1.ox = v1.x; v1.oy = v1.y;
      v2.ox = v2.x; v2.oy = v2.y;
      this.x += dx; this.y += dy;
      this.ox = this.x;  this.oy = this.y;
    }
    public float speedSq() {
      float dx = this.x - this.ox;
      float dy = this.y - this.oy;
      return dx*dx+dy*dy;
    }
    // The frame is the verlet stick, comprising particles P1 and P2. Each world vertex G is a function
    // of this stick according to:
    //   G(P1,P2) = P1 + shape_x * (P2-P1) + shape_y * perp(P2-P1) =>
    //   gx = p1x + shape_x * (p2x-p1x) + shape_y * (p1y-p2y)
    //   gy = p1y + shape_x * (p2y-p1y) + shape_y * (p2x-p1x)
    //
    // To apply a dislocation M on collision point Gc, we need to figure out how P1 and P2 must be
    // moved to dislocate Gc according to M.
    // We derive G with regard to dp1x, dp1y, dp2x, dp2y, giving gradients:
    //   g'x = [1-shape_x, shape_y, shape_x, -shape_y]
    //   g'y = [-shape_y, 1-shape_x, shape_y, shape_x]
    //
    // To move Gc (gcx, gcy) by M (mdx, mdy) we apply
    //   p1x += mdx * (1-gcx) + mdy * (-gcy)
    //   p1y += mdx * (gcy)   + mdy * (1-gcx)
    //   p2x += mdx * (gcx)   + mdy * (gcy)
    //   p2y += mdx * (-gcy)  + mdy * (gcx)
    //
    // see https://pybullet.org/Bullet/phpBB3/viewtopic.php?t=10718
    public void impulse(Vec2f collision, Vec2f movement, float factor) {
      // get corresponding g coordinate in stick system by dotting
      float v1cx = collision.x - v1.x;
      float v1cy = collision.y - v1.y;
      float v12x = v2.x - v1.x;
      float v12y = v2.y - v1.y;
      
      float gscale = idist * idist;
      
      float gcx = (v1cx *  v12x + v1cy * v12y) * gscale;
      float gcy = (v1cx * -v12y + v1cy * v12x) * gscale; // perped v12
      
      // calc p1 and p2 dislocations
      float mdx = movement.x * factor;
      float mdy = movement.y * factor;
      
      float dv1x = mdx * (1f-gcx) + mdy * -gcy;
      float dv1y = mdx * gcy      + mdy * (1f-gcx);
      v1.x += dv1x; 
      v1.y += dv1y;
      float dv2x = mdx * gcx      + mdy * gcy;
      float dv2y = mdx * -gcy     + mdy * gcx; 
      v2.x += dv2x;
      v2.y += dv2y;
    }
  } // class VStick
  
  class Shape {
    List<Vec2f> defVert = new ArrayList<Vec2f>();
    List<Vec2f> vert = new ArrayList<Vec2f>();
    VStick vstick;
    
    long collisionGroups = 0;   // bitmask, each bit represents one group
    long collisionFilters = 0;  // bitmask, collisions where a filter bit matches colliders group bit are ignored
    
    public Shape() {}
    public Shape(int vertices) { for (int i = 0;i  < vertices; i++) vert.add(new Vec2f());}

    public void addVertex(Vec2f v) {defVert.add(v);}
    public Vec2f getVertex(int i) {return vert.get(i);}
    public int countVertices()  {return vert.size();}
    public Vec2f getEdge(int edge, Vec2f dst) {edge%=countVertices(); int nedge=(edge+1)%countVertices(); vert.get(nedge).sub(vert.get(edge), dst); return dst;}
    public void getEdgeVertices(int edge, Vec2f dstStart, Vec2f dstEnd) {edge%=countVertices(); int nedge=(edge+1)%countVertices(); vert.get(edge).assign(dstStart);vert.get(nedge).assign(dstEnd);}
    public Vec2f getCenter(Vec2f c) {c.x=vstick.x;c.y=vstick.y;return c;}

    public void solidify(float stickLen) {
      // construct verlet stick
      vstick = new VStick(stickLen);

      // normalize definition vertices according to stick length
      float n = 1f / stickLen;
      for (Vec2f v : defVert) {
        v.x *= n; v.y *= n;
        vert.add(new Vec2f());
      }
      
      shapify();
      
      // Optimization, collision, sat: check all edges, mark all that are parallel so
      // only one of all parallels is tested during sat
    }
    
    // align shape after verlet stick
    // x axis is along stick, y axis is sticks normal
    public void shapify() {
      final float xframeCX = vstick.v2.x - vstick.v1.x;
      final float xframeO  = vstick.v1.x;
      final float xframeCY = vstick.v1.y - vstick.v2.y;
      final float yframeCX = vstick.v2.y - vstick.v1.y;
      final float yframeO  = vstick.v1.y;
      final float yframeCY = vstick.v2.x - vstick.v1.x;
      final int verts = countVertices();
      for (int i = 0; i < verts; i++) {
        Vec2f def = defVert.get(i);
        Vec2f v = vert.get(i);
        v.x = def.x * xframeCX + xframeO + def.y * xframeCY;
        v.y = def.x * yframeCX + yframeO + def.y * yframeCY;
      }
    }
    
    public void impulse(Vec2f collision, Vec2f movement, float factor) {
      vstick.impulse(collision, movement, factor);
    }
    
    public Projection project(Vec2f axis, Projection dst) {
      dst.reset();
      for (Vec2f v : vert) {
        dst.addDot(v.dot(axis));
      }
      return dst;
    }

    public void move(float dx, float dy) {
      vstick.move(dx, dy); 
    }
    
    Vec2f t1 = new Vec2f();
    Vec2f t2 = new Vec2f();
    public Vec2f findNearestVertex(Vec2f edgeS, Vec2f edgeE, Vec2f norm, Vec2f dst) {
      float minDist = Float.MAX_VALUE;
      if (DBGPHYS) sprites.add(new DbgLine(Color.pink, edgeS, edgeE));
      for (Vec2f v : vert) {
        t1 = edgeS.assign(t1).add(norm);
        t2 = edgeE.assign(t2).sub(norm);
        // check if v is within area defined by lines starting at point edgeStart, direction norm
        // and point edgeEnd, direction -norm
        // sign = (x-x1)*(y2-y1) - (y-y1)*(x2-x1)
        float sign1 = (v.x - edgeS.x) * (t1.y - edgeS.y) - (v.y - edgeS.y) * (t1.x - edgeS.x);
        float sign2 = (v.x - edgeE.x) * (t2.y - edgeE.y) - (v.y - edgeE.y) * (t2.x - edgeE.x);
        if (sign1 < 0) sign1 = -1; else sign1 = 1;
        if (sign2 < 0) sign2 = -1; else sign2 = 1;
        if (sign1 == sign2) {
          // within area, get vertex distance to edge
          t1 = v.assign(t1).sub(edgeS);
          float dist = Math.abs(t1.dot(norm));
          if (dist < minDist) {
            minDist = dist;
            v.assign(dst);
          }
        }
      }
      if (DBGPHYS) sprites.add(new DbgMark(Color.pink, dst));
      return dst;
    }
    
    public Vec2f calcCollisionPoint(Vec2f edgeS, Vec2f edgeE, Vec2f nearVert, Vec2f dst) {
      t1 = edgeE.assign(t1).sub(edgeS).norm();
      t2 = nearVert.assign(t2).sub(edgeS);
      float t = t1.dot(t2);
      if (t < 0) t = 0; else if (t*t > t2.lenSq()) t = t2.len();
      dst = edgeS.assign(dst).add(t*t1.x, t*t1.y);
      return dst;
    }
  } // class Shape
  
  static class Vec2f {
    float x, y;
    public Vec2f() {}
    public Vec2f(float x, float y) {this.x=x; this.y=y;}
    public Vec2f(Vec2f v) {this.x=v.x; this.y=v.y;}
    
    public Vec2f assign(Vec2f dst) {dst.x=x;dst.y=y;return dst;}
    public Vec2f add(Vec2f v,Vec2f dst) {dst.x=x+v.x;dst.y=y+v.y;return dst;}
    public Vec2f add(float dx,float dy,Vec2f dst) {dst.x=x+dx;dst.y=y+dy;return dst;}
    public Vec2f sub(Vec2f v,Vec2f dst) {dst.x=x-v.x;dst.y=y-v.y;return dst;}
    public Vec2f sub(float dx,float dy,Vec2f dst) {dst.x=x-dx;dst.y=y-dy;return dst;}
    public Vec2f mul(float m,Vec2f dst) {dst.x=m*x;dst.y=y*m;return dst;}
    public Vec2f div(float m,Vec2f dst) {float t = 1f/m;return mul(t,dst);}
    public Vec2f neg(Vec2f dst) {dst.x=-x;dst.y=-y;return dst;}
    public Vec2f perp(Vec2f dst) {float t=x;dst.x=-y;dst.y=t;return dst;}
    public Vec2f norm(Vec2f dst) {float t=1f/(float)Math.sqrt(x*x+y*y);dst.x=x*t;dst.y=y*t;return dst;}

    public Vec2f set(float x, float y) {this.x=x;this.y=y;return this;}
    public Vec2f add(float dx, float dy) {this.x+=dx;this.y+=dy;return this;}
    public Vec2f add(Vec2f v) {this.x+=v.x;this.y+=v.y;return this;}
    public Vec2f sub(float dx, float dy) {this.x-=dx;this.y-=dy;return this;}
    public Vec2f sub(Vec2f v) {this.x-=v.x;this.y-=v.y;return this;}
    public Vec2f mul(float m) {this.x*=m;this.y*=m;return this;}
    public Vec2f div(float m) {float t = 1f/m;return mul(t);}
    public Vec2f neg() {x=-x;y=-y;return this;}
    public Vec2f perp() {float t=x;x=-y;y=t;return this;}
    public Vec2f norm() {float t=1f/(float)Math.sqrt(x*x+y*y);x*=t;y*=t;return this;}

    public float len() {return (float)Math.sqrt(x*x+y*y);}
    public float lenSq() {return x*x+y*y;}
    public float dot(Vec2f v) {return this.x*v.x+this.y*v.y;}
    public float angle() {float t=1f/(float)Math.sqrt(x*x+y*y);return (float)Math.atan2(y*t, x*t);}
    public static float dot(Vec2f v1,Vec2f v2) {return v1.x*v2.x+v1.y*v2.y;}
  } // class Vec2f
  
  /////////////////////////////////////////////////////////////////////////////
  // main setup and that
  //
  
  void start() {
    java.util.Timer timer = new java.util.Timer(true);
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        loop();
        Graphics2D g = (Graphics2D)screen.getGraphics();
        if (g != null) {
          draw(g);
          g.dispose();
        }
        repaint();
      }}, LOOP_DELAY_MS, LOOP_DELAY_MS);
  }

  public static void main(String[] args) {
    ProceduralNemesis n = new ProceduralNemesis();
    JFrame f = new JFrame();
    f.getContentPane().add(n);
    f.setSize(WIDTH, HEIGHT);
    f.setVisible(true);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    n.start();
  }
}
