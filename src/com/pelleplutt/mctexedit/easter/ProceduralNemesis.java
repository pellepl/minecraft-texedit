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
  static final float THING_DEF_ROT = 3.137328f;//3.14f*1.35f;
  static final float NEAR_ZERO = 0.0001f;
  
  static final int KEY_UP = 0;
  static final int KEY_DOWN = 1;
  static final int KEY_LEFT = 2;
  static final int KEY_RIGHT = 3;
  static final int KEY_SPACE = 4;
  static final int KEY_ESC = 5;
  static final int KEY_1 = 6;
  static final int KEY_2 = 7;
  static final int KEY_3 = 8;
  static final int KEY_4 = 9;
  static final int KEY_5 = 10;
  static final int KEY_6 = 11;
  static final int KEY_7 = 12;
  static final int KEY_8 = 13;
  static final int KEY_9 = 14;
  static final int KEY_0 = 15;
  
  boolean keyStatus[] = new boolean[16];
  Collider collider = new Collider();
  Player player;
  Thing thing; // TODO remove
  java.util.List<Sprite> sprites = new ArrayList<Sprite>();
  java.util.List<Sprite> deadSprites = new ArrayList<Sprite>();
  BufferedImage screen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB); 
  boolean debugMode = false;
  boolean debugStep = false;
  int loopNbr = 0;

  public ProceduralNemesis() {
    player = new Player();
    player.moveStatic(32, 100);
    sprites.add(player);
    thing = new Thing();
    thing.moveStatic(200, 100);
    thing.rotateStatic(THING_DEF_ROT);
    sprites.add(thing);
    Sprite enemy = new EnemyFighter();
    enemy.moveStatic(400,300);
    sprites.add(enemy);
    
    registerKey("W", KEY_UP);
    registerKey("S", KEY_DOWN);
    registerKey("A", KEY_LEFT);
    registerKey("D", KEY_RIGHT);
    registerKey("SPACE", KEY_SPACE);
    registerKey("ESCAPE", KEY_ESC);
    registerKey("1", KEY_1);
    registerKey("2", KEY_2);
    registerKey("3", KEY_3);
    registerKey("4", KEY_4);
    registerKey("5", KEY_5);
    registerKey("6", KEY_6);
    registerKey("7", KEY_7);
    registerKey("8", KEY_8);
    registerKey("9", KEY_9);
    registerKey("0", KEY_0);
    setFocusable(true);
    requestFocus();
  }
  
  void dbgMark(Color c, Vec2f v) {
    sprites.add(new DbgMark(c, v, 50, null));
  }
  void dbgMark(Color c, Vec2f v, int life, String str) {
    sprites.add(new DbgMark(c, v, life, str));
  }
  void dbgLine(Color c, Vec2f v1, Vec2f v2) {
    sprites.add(new DbgLine(c, v1, v2, 50, null));
  }
  void dbgLine(Color c, Vec2f v1, Vec2f v2, int life, String str) {
    sprites.add(new DbgLine(c, v1, v2, life, str));
  }
  void dbgShape(Color c, Shape s) {
    sprites.add(new DbgShape(c, s, 50, null));
  }
  void dbgShape(Color c, Shape s, int life, String str) {
    sprites.add(new DbgShape(c, s, life, str));
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
    if (diag) {
      pdx *= 0.7071f; // 1/sqrt(2);
      pdy *= 0.7071f;
    }
    player.moveStatic(pdx, pdy);

    
    if (keyStatus[KEY_ESC]) {
      Log.println("---------------------------");
      thing.life = 1;
      thing = new Thing();
      thing.moveStatic(200, 100);
      thing.rotateStatic(THING_DEF_ROT);
      sprites.add(thing);
      keyStatus[KEY_ESC] = false;
    }
    if (keyStatus[KEY_1]) {
      player.rotateStatic(0.1f);
    }
    if (keyStatus[KEY_2]) {
      player.rotateStatic(-0.1f);
    }
    if (keyStatus[KEY_3]) {
      thing.push(
          0.15f*(player.shape.vstick.x - thing.shape.vstick.x), 
          0.15f*(player.shape.vstick.y - thing.shape.vstick.y));
      keyStatus[KEY_3] = false;
    }

    if (keyStatus[KEY_9]) {
      debugMode = !debugMode;
      Log.println("..............................  DEBUGMODE " + (debugMode ? "ON" : "OFF"));
      keyStatus[KEY_9] = false;
    }
    if (keyStatus[KEY_0]) {
      Log.println("..............................  DEBUG LOOP " + loopNbr);
      debugStep = true;
      keyStatus[KEY_0] = false;
    }

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
    shot.moveStatic(
        player.shape.vstick.v1.x + sdx*(player.shape.vstick.dist + shot.shape.vstick.dist),
        player.shape.vstick.v1.y + sdy*(player.shape.vstick.dist + shot.shape.vstick.dist));
    shot.rotateStatic(player.shape.angle());
    shot.push(sdx*16f, sdy*16f);
    shot.life = 40;
    shot.shape.vstick.setInverseFriction(0.975f);
    shot.shape.collisionGroups = (1<<0);
    shot.shape.collisionFilters = (1<<0);
    sprites.add(shot);
  }

  //////////////
  // Main game loop
  Vec2f phyMTVA = new Vec2f();
  Vec2f phyMTVB = new Vec2f();
  Vec2f phyMTV = new Vec2f();
  Vec2f phyColl = new Vec2f();
  public void loop() {
    updateInteractions();
    if (debugMode && !debugStep) return;
    loopNbr++;
    debugStep = false;

    angleConstraint(player.shape.vstick, 0f, 0.025f);
    for (Sprite sprite : sprites) {
      sprite.update();
      if (sprite.gone) {
        deadSprites.add(sprite);
        continue;
      }
      if (sprite.shape == null) {
        continue;
      }
      stickConstraint(sprite.shape.vstick, 1f);
      sprite.shape.vstick.update(1f);
      sprite.shape.shapify();
      collider.precalcVelocityOOB(sprite);
    }

    while (!deadSprites.isEmpty()) {
      sprites.remove(deadSprites.remove(0));
    }

    final int spriteCount = sprites.size();
    int maxIterations = 5;
    while (maxIterations-- > 0) {
      boolean haveCollisions = false;
      for (int i = 0; i < spriteCount; i++) {
        Sprite sprA = sprites.get(i);
        if (sprA.shape == null) continue;
        for (int j = i + 1; j < spriteCount; j++) {
          Sprite sprB = sprites.get(j);
          if (sprB.shape == null) continue;
          if ((sprA.shape.collisionGroups & sprB.shape.collisionFilters) +
              (sprA.shape.collisionFilters & sprB.shape.collisionGroups) != 0) continue;
          float pene = collider.collides(sprA, sprB, phyMTVA, phyMTVB, phyMTV, phyColl);
          if (pene > 0) {
            haveCollisions = true;

            if (debugMode) {
              Log.println(String.format("LOOP [coll it %d] collision obj:%d[%+.1f,%+.1f]%s / %d[%+.1f,%+.1f]%s   collXY:%+.1f,%+.1f   pen:%+.1f  mtv:%+.1f,%+.1f;len:%.1f",
                  maxIterations,
                  sprA.id, sprA.shape.vstick.x, sprA.shape.vstick.y, sprA.highVelocity ? "(HV)": "    ",
                  sprB.id, sprB.shape.vstick.x, sprB.shape.vstick.y, sprB.highVelocity ? "(HV)": "    ",
                  phyColl.x,phyColl.y,
                  pene,
                  phyMTV.x,phyMTV.y,phyMTV.len()
              ));
              dbgMark(Color.white, phyColl, 10, null);
              dbgLine(Color.red, sprA.shape.vstick, phyMTV.assign(new Vec2f()).mul(-5f).add(sprA.shape.vstick), 10, null);
              dbgMark(Color.red, sprA.shape.vstick,  10, null);
              dbgLine(Color.blue, sprB.shape.vstick, phyMTV.assign(new Vec2f()).mul(5f).add(sprB.shape.vstick), 10, null);
              dbgMark(Color.blue, sprB.shape.vstick,  10, null);
            }
            
            float inviMassSum = 1f; // TODO PETER1f/(sprA.imass + sprB.imass);

            sprA.shape.impulse(phyColl, phyMTV, -sprB.imass * inviMassSum);
            sprB.shape.impulse(phyColl, phyMTV, sprA.imass * inviMassSum);
            stickConstraint(sprA.shape.vstick, 1f);
            stickConstraint(sprB.shape.vstick, 1f);
            sprA.shape.shapify();
            sprB.shape.shapify();
            sprA.shape.updateCenter();
            sprB.shape.updateCenter();
          }
        }
      }
      if (!haveCollisions) {
        break;
      }
    } // while maxIterations
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
      shape.addVertex(new Vec2f( 76,  40));
      shape.addVertex(new Vec2f( 80, -32));
      shape.addVertex(new Vec2f(  10, -40));
      shape.addVertex(new Vec2f(  0,  22));
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
  
  interface Dbg {}
  class DbgShape extends Sprite implements Dbg {
    Color c; List<Vec2f> v = new ArrayList<Vec2f>();
    float totalLife; String descr;
    public DbgShape(Color c, Shape s, int lifetime, String str) {
      this.c = c; for (Vec2f sv : s.vert) v.add(new Vec2f(sv)); life = lifetime; totalLife = lifetime; descr = str;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(colMix(c, colTrans, life/totalLife));
      final int vcnt = v.size();
      for (int i = 0; i <= vcnt; i++) {
        Vec2f v1 = v.get(i % vcnt);
        Vec2f v2 = v.get((i+1) % vcnt);
        g.drawLine((int)(v1.x), (int)(v1.y), (int)(v2.x), (int)(v2.y));
      }
      if (descr != null) {
        g.drawString(descr, (int)v.get(0).x, (int)v.get(0).y);
      }
    }
  }
  class DbgLine extends Sprite implements Dbg {
    float x,y,tx, ty; Color c;
    float totalLife; String descr;
    public DbgLine(Color c, Vec2f from, Vec2f to, int lifetime, String str) {
      this.c = c; x = from.x; y = from.y; tx = to.x; ty = to.y; life = lifetime; totalLife = lifetime; descr = str;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(colMix(c, colTrans, life/totalLife));
      g.drawLine((int)x, (int)y, (int)tx, (int)ty);
      g.fillOval((int)tx-2, (int)ty-2, 4,4);
      if (descr != null) {
        g.drawString(descr, (int)(x+tx)/2, (int)(y+ty)/2);
      }
    }
  }
  
  class DbgMark extends Sprite implements Dbg {
    float x,y; Color c;
    float totalLife; String descr;
    public DbgMark(Color c, Vec2f p, int lifetime, String str) {
      this.c = c; x = p.x; y = p.y; life = lifetime; totalLife = lifetime; descr = str;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(colMix(c, colTrans, life/totalLife));
      g.drawLine((int)(x-3), (int)(y-3), (int)(x+3), (int)(y+3));
      g.drawLine((int)(x+3), (int)(y-3), (int)(x-3), (int)(y+3));
      if (descr != null) {
        g.drawString(descr, (int)(x), (int)(y+20));
      }
    }
  }
  
  static int _gid = 0;
  abstract class Sprite {
    int id = _gid++;
    int life;
    boolean gone = false;
    float imass = 1f;
    Shape shape;
    Shape oobShape = new Shape(4);
    boolean highVelocity = false;
    float highVelBodyX, highVelBodyY;
    Vec2f vel = new Vec2f();
    Vec2f velN = new Vec2f();
    final Color colTrans = new Color(0,0,0,0);
    public void paint(Graphics2D g) {
      paintShape(g);
      if (DBGPHYS) paintShapeVerlet(g, 0, 0);
      //paintShapeNormals(g, shape, 0, 0);
      g.setColor(Color.gray);
      if (debugMode) paintShape(g, oobShape);
    }
    public void update() { 
      if (life > 0) {
        life--; 
        if (life == 0) gone = true; 
      }
    }
    public void moveStatic(float dx, float dy) {
      shape.moveStatic(dx, dy);
      shape.shapify();
    }
    public void rotateStatic(float dang) {
      shape.rotateStatic(dang);
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
    public void paintShapeNormals(Graphics2D g, Shape s, float dx, float dy) {
      if (s == null) return;
      final int vcnt = s.countVertices();
      for (int i = 0; i <= vcnt; i++) {
        Vec2f v1 = s.getVertex(i % vcnt);
        Vec2f v2 = s.getVertex((i+1) % vcnt);
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
    final float dang = (float)(targetAngle-curAng) * 0.5f * dt;
    final float cos = (float)Math.cos(dang);
    final float sin = (float)Math.sin(dang);
    float fx = (stick.v1.x - stick.x) * cos + (stick.v1.y - stick.y) * -sin; 
    float fy = (stick.v1.x - stick.x) * sin + (stick.v1.y - stick.y) * cos;
    stick.v1.x = stick.x+fx;
    stick.v1.y = stick.y+fy;
    stick.v2.x = stick.x-fx;
    stick.v2.y = stick.y-fy;
  }
  
  class Collider {
    Projection projections[] = new Projection[8];
    Vec2f vectors[] = new Vec2f[64];
    public Collider() {
      for (int i = 0; i < projections.length; i++) { projections[i] = new Projection(); }
      for (int i = 0; i < vectors.length; i++) { vectors[i] = new Vec2f(); }
    }
    int vix = 0;
    int pix = 0;

    void precalcVelocityOOB(Sprite spr) {
      Shape s = spr.shape;
      float speedFactSq = s.vstick.speedSq() * (s.vstick.idist * s.vstick.idist); 
      spr.highVelocity = speedFactSq > 0.125f;
      if (spr.highVelocity) {
        calcVelocityOOB(spr);
      }
    }
    
    float collides(Sprite sprA, Sprite sprB, Vec2f mtvA, Vec2f mtvB, Vec2f mtv, Vec2f collision) {
      vix = 0;
      pix = 0;
      // broad phase check
      if (!intersectAABB(sprA,sprB)) return 0;
      if (debugMode) {
        Log.println(String.format("%d / %d aabb intersects", sprA.id, sprB.id));
      }
      // narrow phase check
      float pene = intersectSAT(sprA,sprB,mtvA,mtvB,mtv,collision);
      if (pene >= COLLISION_MIN_PENE) {
        Vec2f ca = vectors[vix++];
        Vec2f cb = vectors[vix++];
        sprA.shape.getCenter(ca);
        sprB.shape.getCenter(cb);
        cb.sub(ca);
        if (cb.dot(mtv) < 0) {
          // make sure the mtv separates the objects
          mtv.neg();
        }
        return pene;
      } else {
        return 0f;
      }
    }
    
    // broad phase:  create aabb' of aabb(position) + aabb(position + velocity)
    // narrow phase: project shape on velocity vectors normal = front and back sides of obb,
    //               top and bottom of obb is velocity translation vector,
    //               use this obb to find out where collision occurs
    
    boolean intersectAABB(Sprite spr1, Sprite spr2) {
      Shape s1 = spr1.shape;
      Shape s2 = spr2.shape;
      float s1MinX, s1MaxX, s1MinY, s1MaxY;
      float s2MinX, s2MaxX, s2MinY, s2MaxY;
      s1MinX = -s1.vstick.distDSq2;
      s1MaxX =  s1.vstick.distDSq2;
      s2MinX = -s2.vstick.distDSq2;
      s2MaxX =  s2.vstick.distDSq2;
      if (spr1.highVelocity) {
        float v1x = s1.vstick.x - s1.vstick.ox;
        if (v1x < 0) s1MinX += v1x;
        else         s1MaxX += v1x;
      }
      if (spr2.highVelocity) {
        float v2x = s2.vstick.x - s2.vstick.ox;
        if (v2x < 0) s2MinX += v2x;
        else         s2MaxX += v2x;
      }
      boolean overlapX = 
          (s1.vstick.x + s1MinX < s2.vstick.x + s2MaxX) &&
          (s2.vstick.x + s2MinX < s1.vstick.x + s1MaxX);

      if (!overlapX) return false;
      s1MinY = -s1.vstick.distDSq2;
      s1MaxY =  s1.vstick.distDSq2;
      s2MinY = -s2.vstick.distDSq2;
      s2MaxY =  s2.vstick.distDSq2;
      if (spr1.highVelocity) {
        float v1y = s1.vstick.y - s1.vstick.oy;
        if (v1y < 0) s1MinY += v1y;
        else         s1MaxY += v1y;
      }
      if (spr2.highVelocity) {
        float v2y = s2.vstick.y - s2.vstick.oy;
        if (v2y < 0) s2MinY += v2y;
        else         s2MaxY += v2y;
      }
      boolean overlapY = 
          (s1.vstick.y + s1MinY < s2.vstick.y + s2MaxY) &&
          (s2.vstick.y + s2MinY < s1.vstick.y + s1MaxY);

      return overlapY;
    }
    
    void calcVelocityOOB(Sprite spr) {
      Shape s = spr.shape;
      Shape oob = spr.oobShape;
      Vec2f velPN = vectors[vix++];
      Vec2f tmp = vectors[vix++];
      spr.vel.set(s.vstick.x - s.vstick.ox, s.vstick.y - s.vstick.oy);
      spr.vel.assign(spr.velN).norm();
      spr.velN.assign(velPN).perp();
      
      // project shape onto normalized velocity vector and perped normalized velocity vector
      final int sVertices = s.countVertices();
      float minV = Float.MAX_VALUE; 
      float maxV = Float.MIN_VALUE;
      float minPV = Float.MAX_VALUE; 
      float maxPV = Float.MIN_VALUE;
      Vec2f refVertex = s.getVertex(0); // any vertex will do
      for (int i = 1; i < sVertices; i++) {
        s.getVertex(i).assign(tmp).sub(refVertex);
        float v = spr.velN.dot(tmp);
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
      float vminx  = minV  * spr.velN.x;
      float vminy  = minV  * spr.velN.y;
      float vmaxx  = maxV  * spr.velN.x + spr.vel.x;
      float vmaxy  = maxV  * spr.velN.y + spr.vel.y;
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
      spr.highVelBodyX = (maxV - minV) * spr.velN.x;
      spr.highVelBodyY = (maxV - minV) * spr.velN.y;
    }
    
    float intersectSAT(Sprite spr1, Sprite spr2, Vec2f mtv1, Vec2f mtv2, Vec2f mtv, Vec2f collision) {
      Shape s1 = spr1.shape;
      Shape s2 = spr2.shape;
      Vec2f nearestVertex = vectors[vix++]; 
      Vec2f es1 = vectors[vix++]; 
      Vec2f ee1 = vectors[vix++]; 
      Vec2f es2 = vectors[vix++]; 
      Vec2f ee2 = vectors[vix++]; 
      float pene1, pene2;
      Vec2f frontVertex;
      boolean collHV1 = false;
      boolean collHV2 = false;
      Vec2f origV1 = vectors[vix++]; 
      Vec2f origV1O = vectors[vix++]; 
      Vec2f origV2 = vectors[vix++]; 
      Vec2f origV2O = vectors[vix++]; 
      
      if (spr1.highVelocity) {
        if (debugMode) Log.println(String.format("sat [HV] spr1 %d", spr1.id));
        float peneHighVel = sat(spr1.oobShape, s2, null, mtv1, es1, ee1);
        if (peneHighVel == 0) {
          if (debugMode) Log.println(String.format("    [HV] %d / %d no sat", spr1.id, spr2.id));
          return 0;
        }
        peneHighVel = sat(s2, spr1.oobShape, spr1.vel, mtv1, es1, ee1);
        if (peneHighVel != 0) {
          frontVertex = s1.findFarthestVertexInDirection(spr1.vel);
          float t = collider.getTrajectoryEdgeCrossing(frontVertex, spr1.vel, es1, ee1);
          if (t < 0) {
            // TODO Should not happen
            Log.println(String.format("    [HV] spr1 %d trajectory fail %f, vel %+.1f,%+.1f", spr1.id, t, spr1.vel.x, spr1.vel.y));
          }
          if (!Float.isNaN(t)) {
            collHV1 = true;
            s1.vstick.v1.assign(origV1);
            s1.vstick.v1.assignOld(origV1O);
            s1.vstick.v2.assign(origV2);
            s1.vstick.v2.assignOld(origV2O);
            Vec2f velNorm = spr1.vel.norm(vectors[vix++]);
            s1.vstick.moveStatic(spr1.vel.x * t, spr1.vel.y * t);
            s1.vstick.moveStatic(velNorm.x * spr1.shape.vstick.dist * 0.5f, velNorm.y * spr1.shape.vstick.dist * 0.5f);
            if (debugMode) Log.println(String.format("    [HV] COLLISION: spr1 pen:%+.1f, t:%+.1f, sprite translation xy:%+.1f,%+.1f", 
                peneHighVel, t, spr1.vel.x * t, spr1.vel.y * t));
          } else {
            if (debugMode) {
              Log.println(String.format("    [HV] spr1 %d trajectory fail %f, vel %+.1f,%+.1f", spr1.id, t, spr1.vel.x, spr1.vel.y));
              dbgLine(Color.magenta, frontVertex, spr1.vel.assign(new Vec2f()).add(frontVertex));
              dbgLine(Color.blue, es1, ee1);
              collision.set(frontVertex.x + spr1.vel.x * t, frontVertex.y + spr1.vel.y * t);
              dbgMark(Color.red, collision);
            }
          }
        } else {
          if (debugMode) {
            Log.println(String.format("    [HV] %d / %d NO SAT", spr2.id, spr1.id));
            return 0;
          }
        }
        
      } else if (spr2.highVelocity) {
        if (debugMode) Log.println(String.format("sat [HV] spr2 %d", spr2.id));
        float peneHighVel = sat(spr2.oobShape, s1, null, mtv2, es2, ee2);
        if (peneHighVel == 0) {
          if (debugMode) Log.println(String.format("    [HV] %d / %d NO SAT", spr2.id, spr1.id));
          return 0;
        }
        peneHighVel = sat(s1, spr2.oobShape, spr2.vel, mtv2, es2, ee2);
        if (peneHighVel != 0) {
          frontVertex = s2.findFarthestVertexInDirection(spr2.vel);
          float t = collider.getTrajectoryEdgeCrossing(frontVertex, spr2.vel, es2, ee2);
          if (t < 0) {
            // TODO Should not happen
            Log.println(String.format("    [HV] spr1 %d trajectory fail %f, vel %+.1f,%+.1f", spr2.id, t, spr2.vel.x, spr2.vel.y));
          }
          if (!Float.isNaN(t)) {
            collHV2 = true;
            s2.vstick.v1.assign(origV1);
            s2.vstick.v1.assignOld(origV1O);
            s2.vstick.v2.assign(origV2);
            s2.vstick.v2.assignOld(origV2O);
            Vec2f velNorm = spr2.vel.norm(vectors[vix++]);
            s2.vstick.moveStatic(spr2.vel.x * t, spr2.vel.y * t);
            s2.vstick.moveStatic(velNorm.x * s2.vstick.dist * 0.5f, velNorm.y * s2.vstick.dist * 0.5f);
            if (debugMode) Log.println(String.format("    [HV] COLLISION: spr2 pen:%+.1f, t:%+.1f, sprite translation xy:%+.1f,%+.1f", 
                peneHighVel, t, spr2.vel.x * t, spr2.vel.y * t));
          } else {
            if (debugMode) {
              Log.println(String.format("    [HV] spr2 %d trajectory fail %f, vel %+.1f,%+.1f", spr2.id, t, spr2.vel.x, spr2.vel.y));
              dbgLine(Color.magenta, frontVertex, spr2.vel.assign(new Vec2f()).add(frontVertex));
              dbgLine(Color.blue, es2, ee2);
              collision.set(frontVertex.x + spr2.vel.x * t, frontVertex.y + spr2.vel.y * t);
              dbgMark(Color.red, collision);
            }
          }
        } else {
          if (debugMode) Log.println(String.format("    [HV] %d / %d NO SAT", spr1.id, spr2.id));
          return 0;
        }
      }

      pene1 = sat(s1, s2, null, mtv1, es1, ee1);
      if (pene1 == 0) {
        // If any highvel collision as registered but no stationary sat found anything, restore state
        if (collHV1) s1.vstick.restore(origV1, origV1O, origV2, origV2O);
        if (collHV2) s2.vstick.restore(origV1, origV1O, origV2, origV2O);
        return 0;
      }
      pene2 = sat(s2, s1, null, mtv2, es2, ee2);
      if (pene2 == 0) {
        // If any highvel collision as registered but no stationary sat found anything, restore state
        if (collHV1) s1.vstick.restore(origV1, origV1O, origV2, origV2O);
        if (collHV2) s2.vstick.restore(origV1, origV1O, origV2, origV2O);
        return 0;
      }
      if (debugMode) Log.println(String.format("sat %d / %d found (pene1:%+.1f, pene2:%+.1f)", spr1.id, spr2.id, pene1, pene2));

      mtv1.neg();
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
      
      if (debugMode) {
        Log.println(String.format("    %d / %d collision  pene1/2:%+.1f/%+.1f  mtv1:%+.1f,%+.1f  mtv2:%+.1f,%+.1f", 
            spr1.id, spr2.id, pene1, pene2,
            mtv1.x, mtv1.y, mtv2.x, mtv2.y));
      }
      if (pene1 < pene2) {
        mtv1.assign(mtv);
      } else {
        mtv2.assign(mtv);
      }
      
      return pene1 < pene2 ? pene1 : pene2;
    }

    float sat(Shape sref, Shape sother, Vec2f otherVelHint, Vec2f mtv, Vec2f collEdgeS, Vec2f collEdgeE) {
      float minOverlap = Float.MAX_VALUE;
      final int srefVertices = sref.countVertices();
      Vec2f edgeS = vectors[vix++];
      Vec2f edgeE = vectors[vix++];
      Vec2f edge = vectors[vix++];
      Vec2f axis = vectors[vix++];
      Vec2f posRef = vectors[vix++];
      Vec2f dirOther = vectors[vix++];
      Projection pref = projections[pix++];
      Projection pother = projections[pix++];
      sref.getCenter(posRef);
      sother.getCenter(dirOther).sub(posRef).neg();
      if (debugMode) {
        // dbgMark(Color.green, posRef, 1,"ref");
        // dbgShape(Color.green, sref, 1, null);
        // dbgMark(Color.red, sother.getCenter(new Vec2f()), 1, "oth");
        // dbgShape(Color.red, sother, 1, null);
        // if (otherVelHint != null)
        //   dbgLine(Color.red, sother.getCenter(new Vec2f()), sother.getCenter(new Vec2f()).add(otherVelHint), srefVertices, "vel");
        // else
        //   dbgLine(Color.red, sother.getCenter(new Vec2f()), sother.getCenter(new Vec2f()).add(dirOther), srefVertices, "dir");
      }

      for (int i = 0; i < srefVertices; i++) {
        sref.getEdgeVertices(i, edgeS, edgeE);
        if (debugMode) dbgLine(Color.white, edgeS, edgeE, 1, "e" + i);
        edge = edgeE.sub(edgeS, edge);
        axis = edge.perp(axis);
        axis.norm();
        sref.project(axis, pref);
        sother.project(axis, pother);
        float overlap;
        if ((overlap = pref.overlap(pother)) == 0) {
          if (debugMode) Log.println(String.format("        edge %d no coll, projection gap found %f", i, overlap));
          return 0;
        }
        if (sref.vstick != null && overlap > sref.vstick.dist) {
          // TODO figure out why this happen, surely it is because of containment?
          Log.println("overlap exceed sticklen " + overlap + " > " + sref.vstick.dist + ": collision ignored");
          return 0; // returning 0 here makes things a lot more robust
        }
        
        // TODO PETER come up with something better for finding edge
        float dot = axis.dot(otherVelHint == null ? dirOther : otherVelHint); 
        if (debugMode) Log.println(String.format("        edge %d overlaps by %+.1f", i, overlap));
        if ((int)(overlap * 100) <= (int)(minOverlap * 100)) { // allow some numeric difference
          axis.assign(mtv);
          minOverlap = overlap;
          if (dot < 0) {
            // if the edge has a normal pointing away from other object, it cannot collide 
            edgeS.assign(collEdgeS);
            edgeE.assign(collEdgeE);
          }
        }
      }
      if (debugMode) {
        Log.println(String.format("        collision found, pene %+.1f", minOverlap));
        dbgLine(Color.pink, collEdgeS, collEdgeE, 10, null);  
      }
      return minOverlap;
    }

    float getTrajectoryEdgeCrossing(Vec2f s, Vec2f d, Vec2f es, Vec2f ee) {
      float denom = (-d.x) * (es.y - ee.y) - (-d.y) * (es.x - ee.x);
      if (Math.abs(denom) <= NEAR_ZERO) return Float.NaN;
      float nom = (s.x - es.x) * (es.y - ee.y) - (s.y - es.y) * (es.x - ee.x);
      return nom/denom;
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
    public Vec2f assignOld(Vec2f dst) {
      dst.x = ox;
      dst.y = oy;
      return dst;
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
    public void restore(Vec2f origV1, Vec2f origV1O, Vec2f origV2, Vec2f origV2O) {
      v1.x = origV1.x;
      v1.y = origV1.y;
      v1.ox = origV1O.x;
      v1.oy = origV1O.y;
      v2.x = origV2.x;
      v2.y = origV2.y;
      v2.ox = origV2O.x;
      v2.oy = origV2O.y;
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
      updateCenter();
      ox = this.x;
      oy = this.y;
    }
    public void update(float dt) {
      v1.update(dt);
      v2.update(dt);
      ox = this.x;
      oy = this.y;
      updateCenter();
    }
  public void updateCenter() {
    x = 0.5f*(v1.x + v2.x); 
    y = 0.5f*(v1.y + v2.y); 
  }
  public void moveStatic(float dx, float dy) {
      v1.x += dx;   v1.y += dy;
      v2.x += dx;   v2.y += dy;
      v1.ox = v1.x; v1.oy = v1.y;
      v2.ox = v2.x; v2.oy = v2.y;
      this.x += dx; this.y += dy;
      this.ox = this.x;  this.oy = this.y;
    }
    public void rotateStatic(float dang) {
      final float cos = (float)Math.cos(dang);
      final float sin = (float)Math.sin(dang);
      updateCenter();
      float fx = (v1.x - x) * cos + (v1.y - y) * -sin; 
      float fy = (v1.x - x) * sin + (v1.y - y) * cos;
      v1.x = x+fx;
      v1.y = y+fy;
      v2.x = x-fx;
      v2.y = y-fy;
      v1.ox = v1.x; v1.oy = v1.y;
      v2.ox = v2.x; v2.oy = v2.y;
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
    public float angle() {return (float)Math.atan2(vstick.v2.y - vstick.v1.y,vstick.v2.x - vstick.v1.x);}
    public void updateCenter() {
      vstick.updateCenter();
    }
    public Vec2f getCenter(Vec2f c) {
      if (vstick != null) {
        c.x=vstick.x;c.y=vstick.y;
      } else {
        float sx = 0; float sy = 0;
        for (Vec2f v : vert) {
          sx += v.x; sy += v.y;
        }
        c.x=sx/(float)vert.size();
        c.y=sy/(float)vert.size();
      }
      return c;
    }

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
      // TODO PETER. testing, help the impulse a wee bit
//      float mx = movement.x*factor*0.5f;
//      float my = movement.y*factor*0.5f;
//      vstick.v1.x += mx;
//      vstick.v1.y += my;
//      vstick.v2.x += mx;
//      vstick.v2.y += my;
//      vstick.v1.ox += mx;
//      vstick.v1.oy += my;
//      vstick.v2.ox += mx;
//      vstick.v2.oy += my;
    }
    
    public Projection project(Vec2f axis, Projection dst) {
      dst.reset();
      for (Vec2f v : vert) {
        dst.addDot(v.dot(axis));
      }
      return dst;
    }

    public void moveStatic(float dx, float dy) {
      vstick.moveStatic(dx, dy); 
    }
    public void rotateStatic(float dang) {
      vstick.rotateStatic(dang); 
    }
    
    Vec2f t1 = new Vec2f();
    Vec2f t2 = new Vec2f();
    public Vec2f findNearestVertex(Vec2f edgeS, Vec2f edgeE, Vec2f norm, Vec2f dst) {
      float minDist = Float.MAX_VALUE;
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
      return dst;
    }

    public Vec2f findFarthestVertexInDirection(Vec2f d) {
      t1.set(d.x, d.y);
      vert.get(0).assign(t2);
      float maxDot = 0;
      Vec2f res = t2;
      for (int i = 1; i < vert.size(); i++) {
        Vec2f v = vert.get(i);
        float dot = (v.x - t2.x) * d.x + (v.y - t2.y) * d.y;
        if (dot > maxDot) {
          maxDot = dot;
          res = v;
        }
      }
      return res;
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
    public float distTo(Vec2f v) {return (float)Math.sqrt((v.x-x)*(v.x-x)+(v.y-y)*(v.y-y));}
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
    f.setSize(600, 400);
    f.setLocation(0,200);
    f.setVisible(true);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    n.start();
  }
}
