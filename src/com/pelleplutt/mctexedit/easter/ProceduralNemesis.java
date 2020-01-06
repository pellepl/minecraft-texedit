package com.pelleplutt.mctexedit.easter;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.pelleplutt.util.*;


// TODO: for highvelocity, find all objects that collides with the OOB
// and select the one with lowest translation along velocity for collision
// TODO: first find all collisions, then apply impulses


public class ProceduralNemesis extends JPanel {
  static final int LOOP_DELAY_MS = 20;
  static final float COLLISION_MIN_PENE = 0.01f;
  static final int WIDTH = 800;
  static final int HEIGHT = 600;
  static final int ITERATIONS = 12;
  static final float THING_DEF_ROT = 3.137328f;//3.14f*1.35f;
  static final float NEAR_ZERO = 0.001f;

  static final float FMAX = Float.MAX_VALUE;
  static final float FMIN = -FMAX;
  
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
  static final int KEY_SH1 = 16;
  static final int KEY_SH2 = 17;
  static final int KEY_SH3 = 18;
  static final int KEY_SH4 = 19;
  static final int KEY_SH5 = 20;
  static final int KEY_SH6 = 21;
  static final int KEY_SH7 = 22;
  static final int KEY_SH8 = 23;
  static final int KEY_SH9 = 24;
  static final int KEY_SH0 = 25;
  
  boolean keyStatus[] = new boolean[32];
  Collider collider = new Collider();
  Player player;
  Thing thing; // TODO remove
  java.util.List<Sprite> sprites = new ArrayList<Sprite>();
  java.util.List<Sprite> deadSprites = new ArrayList<Sprite>();
  ArrayList<Sprite> clonedSprites;
  BufferedImage screen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB); 
  boolean debugMode = false;
  boolean debugShow = false;
  boolean debugStep = false;
  int loopNbr = 0;
  float mag = 1f, transX = 0f, transY = 0f;
  
  void setupScene() {
    player = new Player();
    player.move(32, 100);
    player.getVerlet().setInverseFriction(0.75f);
    player.rest();
    sprites.add(player);
    thing = new Thing();
    thing.move(200, 100);
    thing.rotate(THING_DEF_ROT);
    thing.rest();
    sprites.add(thing);
    Sprite enemy = new EnemyFighter();
    enemy.move(400,300);
    enemy.rest();
    sprites.add(enemy);
    /**/ 
    for (int j = 0; j < 5; j++) {
      for (int i = 0; i < 5; i++) {
        Thing2 t = new Thing2();
        t.move(100 + i*16, 160 + j*20 + ((i&1)==0?10:0));
        t.rest();
        sprites.add(t);
      }
    }
    for (int i = 0; i < 5; i++) {
      Thing3 t = new Thing3();
      t.move(270, 220 + i * 20);
      t.rest();
      sprites.add(t);
    }
    /**/
    Ground ground = new Ground();
    sprites.add(ground);
  }

  public ProceduralNemesis() {
    Log.log = false;

    setupScene();
    
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
    registerKey("7", "shift", KEY_SH7);
    addMouseListener(mouseInteractor);
    addMouseMotionListener(mouseInteractor);
    addMouseWheelListener(mouseInteractor);
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
  void dbgRect(Color c, Vec2f nw, float width, float height) {
    dbgRect(c, nw, width, height, 50, null);
  }
  void dbgRect(Color c, Vec2f nw, float width, float height, int life, String str) {
    Vec2f ne = new Vec2f(nw).add(width, 0);
    Vec2f se = new Vec2f(nw).add(width, height);
    Vec2f sw = new Vec2f(nw).add(0, height);
    sprites.add(new DbgLine(c, nw, ne, life, str));
    sprites.add(new DbgLine(c, ne, se, life, null));
    sprites.add(new DbgLine(c, se, sw, life, null));
    sprites.add(new DbgLine(c, sw, nw, life, null));
  }
  void dbgShape(Color c, Shape s) {
    sprites.add(new DbgShape(c, s, 50, null));
  }
  void dbgShape(Color c, Shape s, int life, String str) {
    sprites.add(new DbgShape(c, s, life, str));
  }

  final MouseAdapter mouseInteractor = new MouseAdapter() {
    int clickAnchorX, clickAnchorY;
    int anchorX, anchorY;
    public void mousePressed(MouseEvent me) {
      clickAnchorX = anchorX = me.getX();
      clickAnchorY = anchorY = me.getY();
    }
    public void mouseReleased(MouseEvent me) {
    }
    public void mouseMoved(MouseEvent me) {
    }
    public void mouseDragged(MouseEvent me) {
      int dx = me.getX() - anchorX;
      int dy = me.getY() - anchorY;
      transX += (float)(dx / mag);
      transY += (float)(dy / mag);
      anchorX = me.getX();
      anchorY = me.getY();
    }
    public void mouseWheelMoved(MouseWheelEvent me) {
      int mx = me.getX();
      int my = me.getY();
      float oMag = mag;
      float xx = transX + mx / mag;
      float yy = transY + my / mag;
      float dmag = (float)Math.pow(10, Math.log10(mag))*0.1f;
      if (me.getWheelRotation() < 0) {
        mag += dmag;
      } else if (mag > 1.0f) {
        mag -= dmag;
      }
      // o' = x - r'*((x-o)/r)
      transX = xx - mag * ((xx - transX)/oMag);
      transY = yy - mag * ((yy - transY)/oMag);
    }
  };
  
  /////////////////////////////////////////////////////////////////////////////
  // User interaction, loops, events and stuff
  //
  
  void registerKey(String keyName, final int keyIndex) {
    registerKey(keyName, null, keyIndex);
  }
  void registerKey(String keyName, String modifier, final int keyIndex) {
    final String m = (modifier == null ? "" : (modifier + " "));
    final String id = m + keyName;
    getInputMap().put(KeyStroke.getKeyStroke(m + "pressed " + keyName), "p"+id);
    getActionMap().put("p"+id, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent a) {
        keyStatus[keyIndex] = true;
      }});
    getInputMap().put(KeyStroke.getKeyStroke(m + "released " + keyName), "r"+id);
    getActionMap().put("r"+id, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent a) {
        keyStatus[keyIndex] = false;
      }});
  }
  
  static final float PLAYER_STEP = 1f;
  int fireCoolOff = 0;
  void updateInteractions() {
    float a = player.shape.angle();
    float dx = (float)Math.cos(a);
    float dy = (float)Math.sin(a);
    dx *= PLAYER_STEP;
    dy *= PLAYER_STEP;
    
    float pdx = 0;
    float pdy = 0;

    boolean diag = false;
    if (keyStatus[KEY_UP]) {
      pdx +=  dy; pdy += -dx;
    }
    if (keyStatus[KEY_DOWN]) {
      pdx += -dy; pdy +=  dx;
    }
    if (keyStatus[KEY_LEFT]) {
      pdx += -dx; pdy += -dy;
      diag |= (keyStatus[KEY_UP] || keyStatus[KEY_DOWN]); 
    }
    if (keyStatus[KEY_RIGHT]) {
      pdx +=  dx; pdy +=  dy;
      diag |= (keyStatus[KEY_UP] || keyStatus[KEY_DOWN]); 
    }
    if (diag) {
      pdx *= 0.7071f; // 1/sqrt(2);
      pdy *= 0.7071f;
    }
    player.move(pdx, pdy);

    
    if (keyStatus[KEY_ESC]) {
      Log.println("---------------------------  RESET");
      sprites.clear();
      setupScene();
      keyStatus[KEY_ESC] = false;
    }
    if (keyStatus[KEY_1]) {
      player.rotateStatic(0.1f);
    }
    if (keyStatus[KEY_2]) {
      player.rotateStatic(-0.1f);
    }
    if (keyStatus[KEY_3]) {
      thing.move(
          0.15f*(player.shape.getX() - thing.shape.getX()), 
          0.15f*(player.shape.getY() - thing.shape.getY()));
      keyStatus[KEY_3] = false;
    }

    if (keyStatus[KEY_SH7]) {
      if (clonedSprites != null) {
        Log.log = true;
        Log.println("..............................  TIMEWARP");
        sprites.clear();
        player = null;
        thing = null;
        for (Sprite s : clonedSprites) {
          try {
            Sprite cs = (Sprite)s.clone();
            sprites.add(cs);
            if (cs instanceof Player) player = (Player)cs;
            if (cs instanceof Thing) thing = (Thing)cs;
          } catch (CloneNotSupportedException e) {
            e.printStackTrace();
          }
        }
        Log.log = debugShow;
      }
      keyStatus[KEY_SH7] = false;
    }
    if (keyStatus[KEY_7]) {
      Log.log = true;
      Log.println("..............................  TIMECAPSULING");
      clonedSprites = new ArrayList<Sprite>();
      for (Sprite s : sprites) {
        try {
          clonedSprites.add((Sprite)s.clone());
        } catch (CloneNotSupportedException e) {
          e.printStackTrace();
        }
      }
      Log.log = debugShow;
      keyStatus[KEY_7] = false;
    }
      
    if (keyStatus[KEY_8]) {
      debugShow = !debugShow;
      Log.log = true;
      Log.println("..............................  DEBUG " + (debugShow ? "ON" : "OFF"));
      Log.log = debugShow;
      keyStatus[KEY_8] = false;
    }
    if (keyStatus[KEY_9]) {
      debugMode = !debugMode;
      debugShow = debugMode;
      Log.log = true;
      Log.println("..............................  DEBUGMODE " + (debugMode ? "ON" : "OFF"));
      Log.log = debugShow;
      keyStatus[KEY_9] = false;
    }
    if (keyStatus[KEY_0]) {
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
  } // updateInteractions()
  
  void playerFire(int dx, int dy) {
    Shot shot = new Shot();
    float a = player.shape.angle();
    float sdx = (float)Math.cos(a);
    float sdy = (float)Math.sin(a);
    shot.move(
        player.shape.getX() + sdx*15.f,
        player.shape.getY() + sdy*15.f);
    shot.rotateStatic(player.shape.angle());
    shot.rest();
    shot.move(sdx*16f, sdy*16f);
    shot.life = 40;
    shot.shape.setInverseFriction(0.975f);
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
  int iteration = ITERATIONS;
  public void loop() {
    updateInteractions();
    if (debugMode && !debugStep) return;
    if (iteration == ITERATIONS && debugShow) {
      loopNbr++;
      Log.printf("..............................  DEBUG LOOP %d", loopNbr);
    }

    debugStep = false;

    for (Sprite sprite : sprites) {
      sprite.tick();
      if (sprite.gone) {
        deadSprites.add(sprite);
        continue;
      }
    }

    while (!deadSprites.isEmpty()) {
      sprites.remove(deadSprites.remove(0));
    }

    if (!debugMode || iteration == ITERATIONS) {
      angleConstraint((VStick)player.getVerlet(), 0f, 0.025f);
      for (Sprite sprite : sprites) {
        if (sprite.shape == null || sprite.shape.isResting()) {
          continue;
        }
        stickConstraint((VStick)sprite.getVerlet(), 1f);
        sprite.shape.update(1f);
        sprite.shape.shapify();
        collider.checkHighVelocity(sprite);
      }
    }


    // TODO
    // Find all high velocity sprites.
    // Test all high velocity sprites and find least translation vector (if any) amongst possible collisions.
    // Move all high velocity sprites according to least translation vector.
    // Resolve loop:
    //   Test all sprites for collisions. Record collision data.
    //   Apply all found collision data.
    //   Update constraints.
    //   Repeat.
    final int spriteCount = sprites.size();
    while (iteration-- > 0) {
      if (debugShow) Log.printf("..........................  ITERATION %d", iteration);
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
            Log.printf("[resolv %d]  precollision obj:%d[%+.1f,%+.1f]%s / %d[%+.1f,%+.1f]%s   collXY:%+.1f,%+.1f   pen:%+.1f  mtv:%+.1f,%+.1f;len:%.1f",
                iteration,
                sprA.id, sprA.shape.getX(), sprA.shape.getY(), sprA.highVelocity ? "(HV)": "    ",
                sprB.id, sprB.shape.getX(), sprB.shape.getY(), sprB.highVelocity ? "(HV)": "    ",
                phyColl.x,phyColl.y,
                pene,
                phyMTV.x,phyMTV.y,phyMTV.len()
            );
            if (debugShow) {
              dbgMark(Color.white, phyColl, 1, null);
              dbgLine(Color.yellow, sprA.getVec(), phyMTV.assign(new Vec2f()).mul(-5f).add(sprA.getVec()), 1, null);
              dbgMark(Color.yellow, sprA.getVec(), 1, "A");
              dbgLine(Color.cyan,   sprB.getVec(), phyMTV.assign(new Vec2f()).mul(+5f).add(sprB.getVec()), 1, null);
              dbgMark(Color.cyan,   sprB.getVec(), 1, "B");
            }
            
            float inviMassSum = 2f/(sprA.imass + sprB.imass);
            if (sprA.imass > 0) sprA.shape.impulse(phyColl, phyMTV, -sprA.imass * inviMassSum);
            if (sprB.imass > 0) sprB.shape.impulse(phyColl, phyMTV, sprB.imass * inviMassSum);
            Log.printf("[resolv %d] postcollision obj:%d[%+.1f,%+.1f]%s / %d[%+.1f,%+.1f]%s",
              iteration,
              sprA.id, sprA.shape.getX(), sprA.shape.getY(), sprA.highVelocity ? "(HV)": "    ",
              sprB.id, sprB.shape.getX(), sprB.shape.getY(), sprB.highVelocity ? "(HV)": "    "
            );
          }
        }
      }
      if (!haveCollisions) {
        iteration = ITERATIONS;
        break;
      }
      if (debugMode && iteration > 0) {
        return;
      }
    } // while maxIterations
    if (debugMode && iteration > 0) {
      return;
    }
    iteration = ITERATIONS;
  } // loop()
  
  
  @Override
  public void paint(Graphics g) {
    g.drawImage(screen, 0, 0, this);
  }

  Stroke stroke = new BasicStroke(2);
  
  public void draw(Graphics2D g) {
    g.setColor(Color.black);
    g.fillRect(0, 0, WIDTH, HEIGHT);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    //g.setStroke(stroke);
    AffineTransform origTransform = g.getTransform();
    g.translate(transX*mag, transY*mag);
    for (Sprite sprite : sprites) {
      sprite.paint(g);
    }
    g.setTransform(origTransform);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // Graphics and such
  //

  class Player extends Sprite {
    public Player() {
      ConvexShape s = new ConvexShape();
      s.addVertex(new Vec2f( 36,  0));
      s.addVertex(new Vec2f(  4, -8));
      s.addVertex(new Vec2f(  0,  0));
      s.addVertex(new Vec2f(  4,  8));
      s.solidify(36);
      this.shape = s;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(Color.cyan);
      super.paint(g);
    }
  }
  class Ground extends Sprite {
    public Ground() {
      ConvexShape s = new ConvexShape();
      final float BIGNUM = 1000f;
      s.addVertex(new Vec2f( 2f*BIGNUM, BIGNUM));
      s.addVertex(new Vec2f( 2f*BIGNUM,-BIGNUM));
      s.addVertex(new Vec2f(  0,       -BIGNUM));
      s.addVertex(new Vec2f(  0,        BIGNUM));
      s.solidify(2f*BIGNUM);
      this.shape = s;
      moveStatic(0, BIGNUM + HEIGHT-190f);
      this.imass = 0;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(Color.gray);
      super.paint(g);
    }
  } // class Thing
  class Thing extends Sprite {
    public Thing() {
      ConvexShape s = new ConvexShape();
      s.addVertex(new Vec2f( 76,  40));
      s.addVertex(new Vec2f( 80, -32));
      s.addVertex(new Vec2f(  10, -40));
      s.addVertex(new Vec2f(  0,  22));
      s.solidify(80);
      this.shape = s;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(Color.green);
      super.paint(g);
    }
  } // class Thing
  class Thing2 extends Sprite {
    public Thing2() {
      ConvexShape s = new ConvexShape();
      s.addVertex(new Vec2f( 16,  0));
      s.addVertex(new Vec2f( 12,  -8));
      s.addVertex(new Vec2f( 4,  -8));
      s.addVertex(new Vec2f( 0,  0));
      s.addVertex(new Vec2f( 4,  8));
      s.addVertex(new Vec2f( 12,  8));
      s.solidify(16);
      this.shape = s;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(Color.magenta);
      super.paint(g);
    }
  } // class Thing2
  class Thing3 extends Sprite {
    public Thing3() {
      ConvexShape s = new ConvexShape();
      s.addVertex(new Vec2f( 90,  0));
      s.addVertex(new Vec2f( 88,  -2));
      s.addVertex(new Vec2f( 2,  -2));
      s.addVertex(new Vec2f( 0,  0));
      s.addVertex(new Vec2f( 2,  2));
      s.addVertex(new Vec2f( 88,  2));
      s.solidify(90);
      this.shape = s;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(Color.orange);
      super.paint(g);
    }
  } // class Thing3
  class EnemyFighter extends Sprite {
    public EnemyFighter() {
      ConvexShape s = new ConvexShape();
      s.addVertex(new Vec2f( 40,   0));
      s.addVertex(new Vec2f( 32, -10));
      s.addVertex(new Vec2f(  0,   0));
      s.addVertex(new Vec2f( 32,  10));
      s.solidify(40);
      this.shape = s;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(Color.pink);
      super.paint(g);
    }
  } // class EnemyFighter
  class Shot extends Sprite {
    public Shot() {
      ConvexShape s = new ConvexShape();
      s.addVertex(new Vec2f( 10, 0));
      s.addVertex(new Vec2f( 5,-3));
      s.addVertex(new Vec2f( 0, 0));
      s.addVertex(new Vec2f( 5, 3));
      s.solidify(10);
      this.shape = s;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(Color.yellow);
      super.paint(g);
    }
  } // class Shot
  
  interface Dbg {}
  class DbgShape extends Sprite implements Dbg {
    Color c; List<Vec2f> v = new ArrayList<Vec2f>();
    float totalLife; String descr;
    public DbgShape(Color c, Shape s, int lifetime, String str) {
      this.c = c; life = lifetime; totalLife = lifetime; descr = str;
      for (int i = 0; i < s.countVertices(); i++) v.add(new Vec2f(s.getVertex(i)));
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(colMix(c, colTrans, life/totalLife));
      final int vcnt = v.size();
      for (int i = 0; i <= vcnt; i++) {
        Vec2f v1 = v.get(i % vcnt);
        Vec2f v2 = v.get((i+1) % vcnt);
        g.drawLine((int)(v1.x * mag), (int)(v1.y * mag), (int)(v2.x * mag), (int)(v2.y * mag));
      }
      if (descr != null) {
        g.drawString(descr, (int)(v.get(0).x * mag), (int)(v.get(0).y * mag));
      }
    }
  } // class DbgShape
  class DbgLine extends Sprite implements Dbg {
    float x,y,tx, ty; Color c;
    float totalLife; String descr;
    public DbgLine(Color c, Vec2f from, Vec2f to, int lifetime, String str) {
      this.c = c; x = from.x; y = from.y; tx = to.x; ty = to.y; life = lifetime; totalLife = lifetime; descr = str;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(colMix(c, colTrans, life/totalLife));
      g.drawLine((int)(x * mag), (int)(y * mag), (int)(tx * mag), (int)(ty * mag));
      g.fillOval((int)(tx * mag-2), (int)(ty * mag-2), 4,4);
      if (descr != null) {
        g.drawString(descr, (int)((x+tx) * mag)/2, (int)((y+ty) * mag)/2);
      }
    }
  } // class DbgLine
  
  class DbgMark extends Sprite implements Dbg {
    float x,y; Color c;
    float totalLife; String descr;
    public DbgMark(Color c, Vec2f p, int lifetime, String str) {
      this.c = c; x = p.x; y = p.y; life = lifetime; totalLife = lifetime; descr = str;
    }
    @Override
    public void paint(Graphics2D g) {
      g.setColor(colMix(c, colTrans, life/totalLife));
      g.drawLine((int)(x * mag-3), (int)(y * mag-3), (int)(x * mag+3), (int)(y * mag+3));
      g.drawLine((int)(x * mag+3), (int)(y * mag-3), (int)(x * mag-3), (int)(y * mag+3));
      if (descr != null) {
        g.drawString(descr, (int)(x * mag), (int)(y * mag+20));
      }
    }
  } // class DbgMark
  
  static int _gid = 0;
  abstract class Sprite implements Cloneable {
    int id = _gid++;
    int life;
    boolean gone = false;
    float imass = 1f;
    Shape shape;
    ConvexShape oobShape = new ConvexShape(4);
    boolean highVelocity = false;
    float highVelBodyX, highVelBodyY;
    Vec2f vel = new Vec2f();
    float velScalar;
    Vec2f velN = new Vec2f();
    final Color colTrans = new Color(0,0,0,0);
    public void paint(Graphics2D g) {
      paintShape(g);
      //paintShapeVerlet(g, 0, 0);
      //paintShapeNormals(g, shape, 0, 0);
      if (debugShow) {
        g.setColor(Color.gray);
        paintShape(g, oobShape);
      }
    }
    public void tick() { 
      if (life > 0) {
        life--; 
        if (life == 0) gone = true; 
      }
    }
    public Vec2f getVec() {
      return new Vec2f(shape.getX(), shape.getY());
    }
    public void move(float dx, float dy) {
      shape.move(dx, dy);
    }
    public void moveStatic(float dx, float dy) {
      shape.moveStatic(dx, dy);
      shape.shapify();
    }
    public void moveTo(float x, float y) {
      shape.moveTo(x, y);
      shape.shapify();
    }
    public void rotate(float dang) {
      shape.rotate(dang);
      shape.shapify();
    }
    public void rotateStatic(float dang) {
      shape.rotateStatic(dang);
      shape.shapify();
    }
    public void rotateTo(float ang) {
      shape.rotateTo(ang);
      shape.shapify();
    }
    public void rest() {
      shape.getVerlet().rest();
    }
    public Verlet getVerlet() {      
      return shape.getVerlet();
    }
    Vec2f t1 = new Vec2f();
    Vec2f t2 = new Vec2f();
    public void paintShape(Graphics2D g, Shape s) { paintShape(g,s,0,0); }
    public void paintShape(Graphics2D g) { paintShape(g,shape,0,0); }
    public void paintShape(Graphics2D g, float dx, float dy) { paintShape(g,shape,dx,dy); }
    public void paintShape(Graphics2D g, Shape s, float dx, float dy) {
      if (s == null) return;
      final int vcnt = s.countVertices();
      for (int i = 0; i < vcnt; i++) {
        s.getEdgeVertices(i, t1, t2);
        g.drawLine((int)((t1.x+dx) * mag), (int)((t1.y+dy) * mag), (int)((t2.x+dx) * mag), (int)((t2.y+dy) * mag));
      }
    }
    public void paintShapeNormals(Graphics2D g, ConvexShape s, float dx, float dy) {
      if (s == null) return;
      final int vcnt = s.countVertices();
      for (int i = 0; i <= vcnt; i++) {
        Vec2f v1 = s.getVertex(i % vcnt);
        Vec2f v2 = s.getVertex((i+1) % vcnt);
        float cx = 0.5f*(v1.x + v2.x);
        float cy = 0.5f*(v1.y + v2.y);
        float nx = -(v2.y - v1.y);
        float ny =  (v2.x - v1.x);
        g.drawLine((int)((cx+dx) * mag), (int)((cy+dy) * mag), (int)((cx+nx+dx) * mag), (int)((cy+ny+dy) * mag));
      }
    }
    public Color colMix(Color c1, Color c2, float t) {
      float it = (1f-t);
      return new Color(
          (int)(t*c1.getRed()   + it*c2.getRed())   & 0xff,
          (int)(t*c1.getGreen() + it*c2.getGreen()) & 0xff,
          (int)(t*c1.getBlue()  + it*c2.getBlue())  & 0xff,
          (int)(t*c1.getAlpha() + it*c2.getAlpha()) & 0xff);
    }
    public Object clone() throws CloneNotSupportedException {
      Sprite clone = (Sprite)super.clone();
      if (shape != null) clone.shape = (Shape)shape.clone();
      clone.oobShape= (ConvexShape)oobShape.clone();
      clone.vel = (Vec2f)vel.clone();
      clone.velN = (Vec2f)velN.clone();
      return clone; 
    }
  } // class Sprite
  
  /////////////////////////////////////////////////////////////////////////////
  // Physics and maths and such
  //

  public void stickConstraint(VStick stick, float dt) {
    float dx = stick.v2.x-stick.v1.x;
    float dy = stick.v2.y-stick.v1.y;
    final float cdist = (float)Math.sqrt(dx*dx + dy*dy);
    final float diff = (stick.size - cdist) * stick.isize;
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

    void checkHighVelocity(Sprite spr) {
      Shape s = spr.shape;
      spr.highVelocity = 
        Math.abs(s.getVX()) > 0.5f*(s.aabbMax.x - s.aabbMin.x) ||
        Math.abs(s.getVY()) > 0.5f*(s.aabbMax.y - s.aabbMin.y);
      if (spr.highVelocity) {
        calcVelocityOOB(spr);
      }
    } // checkHighVelocity

    public Vec2f calcCollisionPoint(Vec2f edgeS, Vec2f edgeE, Vec2f nearVert, Vec2f dst) {
      Vec2f t1 = vectors[vix++];
      Vec2f t2 = vectors[vix++];
      t1 = edgeE.assign(t1).sub(edgeS).norm();
      t2 = nearVert.assign(t2).sub(edgeS);
      float t = t1.dot(t2);
      if (t < 0) t = 0; else if (t*t > t2.lenSq()) t = t2.len();
      dst = edgeS.assign(dst).add(t*t1.x, t*t1.y);
      return dst;
    } // calcCollisionPoint
    
    float collides(Sprite sprA, Sprite sprB, Vec2f mtvA, Vec2f mtvB, Vec2f mtv, Vec2f collision) {
      vix = 0;
      pix = 0;
      // broad phase check
      if (!intersectAABB(sprA,sprB)) return 0;
      Log.printf("%d / %d aabb intersects", sprA.id, sprB.id);
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
    } // collides
    
    boolean intersectAABB(Sprite spr1, Sprite spr2) {
      Shape s1 = spr1.shape;
      Shape s2 = spr2.shape;
      float s1aabbminx = s1.aabbMin.x;
      float s1aabbminy = s1.aabbMin.y;
      float s1aabbmaxx = s1.aabbMax.x;
      float s1aabbmaxy = s1.aabbMax.y;
      float s2aabbminx = s2.aabbMin.x;
      float s2aabbminy = s2.aabbMin.y;
      float s2aabbmaxx = s2.aabbMax.x;
      float s2aabbmaxy = s2.aabbMax.y;

      if (spr1.highVelocity) {
        float v1x = s1.getVX();
        if (v1x < 0) s1aabbminx += v1x;
        else         s1aabbmaxx += v1x;
      }
      if (spr2.highVelocity) {
        float v2x = s2.getVX();
        if (v2x < 0) s2aabbminx += v2x;
        else         s2aabbmaxx += v2x;
      }

      boolean overlapX = (s1aabbminx < s2aabbmaxx) && (s2aabbminx < s1aabbmaxx);
      if (!overlapX) return false;

      if (spr1.highVelocity) {
        float v1y = s1.getVY();
        if (v1y < 0) s1aabbminy += v1y;
        else         s1aabbmaxy += v1y;
      }
      if (spr2.highVelocity) {
        float v2y = s2.getVY();
        if (v2y < 0) s2aabbminy += v2y;
        else         s2aabbmaxy += v2y;
      }

//      dbgRect(spr1.highVelocity ? Color.cyan : Color.blue, new Vec2f(s1aabbminx, s1aabbminy), s1aabbmaxx-s1aabbminx, s1aabbmaxy-s1aabbminy, 1, null);
//      dbgRect(spr2.highVelocity ? Color.cyan : Color.blue, new Vec2f(s2aabbminx, s2aabbminy), s2aabbmaxx-s2aabbminx, s2aabbmaxy-s2aabbminy, 1, null);

      boolean overlapY = (s1aabbminy < s2aabbmaxy) && (s2aabbminy < s1aabbmaxy);
      return overlapY;// && overlapX;
    } // intersectAABB
    
    void calcVelocityOOB(Sprite spr) {
      Shape s = spr.shape;
      ConvexShape oob = spr.oobShape;
      Vec2f velPN = vectors[vix++];
      Vec2f tmp = vectors[vix++];
      spr.vel.set(s.getVX(), s.getVY());
      spr.velScalar = spr.vel.len();
      spr.vel.assign(spr.velN).mul(1f/spr.velScalar);
      spr.velN.assign(velPN).perp();
      
      // project shape onto normalized velocity vector and perped normalized velocity vector
      final int sVertices = s.countVertices();
      float minV = FMAX;
      float maxV = FMIN;
      float minPV = FMAX;
      float maxPV = FMIN;
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
    } // calcVelocityOOB
    
    float intersectSAT(Sprite spr1, Sprite spr2, Vec2f mtv1, Vec2f mtv2, Vec2f mtv, Vec2f collision) {
      Shape s1 = spr1.shape;
      Shape s2 = spr2.shape;
      Vec2f nearestVertex = vectors[vix++]; 
      Vec2f es1 = vectors[vix++]; 
      Vec2f ee1 = vectors[vix++]; 
      Vec2f es2 = vectors[vix++]; 
      Vec2f ee2 = vectors[vix++]; 
      float pene1, pene2;
      Vec2f origV1 = vectors[vix++]; 
      Vec2f origV1o = vectors[vix++]; 
      Vec2f origV2 = vectors[vix++]; 
      Vec2f origV2o = vectors[vix++]; 
      boolean collHV1 = false;
      boolean collHV2 = false;

      if (spr1.highVelocity) {
        ((VStick)s1.getVerlet()).save(origV1,origV1o,origV2,origV2o);
        if (!highVelSAT(spr1, spr2)) return 0;
        collHV1 = true;
      } else if (spr2.highVelocity) {
        ((VStick)s2.getVerlet()).save(origV1,origV1o,origV2,origV2o);
        if (!highVelSAT(spr2, spr1)) return 0;
        collHV2 = true;
      }
      pene1 = sat(s1, s2, null, mtv1, es1, ee1);
      if (pene1 == 0) {
        // If highvel collision was registered but stationary sat found nothing, restore state
        if (collHV1) {
          if (debugShow) dbgShape(Color.red, s1, 1, "NOCOLL");
          Log.printf("[HV] registered but no static collision, restore %d from %+.1f,%+.1f to %+.1f,%+.1f", spr1.id,
              s1.getX(), s1.getY(), (origV1.x + origV2.x)*0.5f,  (origV1.y + origV2.y)*0.5f);
          ((VStick)s1.getVerlet()).restore(origV1, origV1o, origV2, origV2o);
          s1.shapify();
          if (debugShow) dbgShape(Color.cyan, s1, 1, "RESTORED");
        }
        if (collHV2) {
          if (debugShow) dbgShape(Color.red, s2, 1, "NOCOLL");
          Log.printf("[HV] registered but no static collision, restore %d from %+.1f,%+.1f to %+.1f,%+.1f", spr2.id,
              s2.getX(), s2.getY(), (origV1.x + origV2.x)*0.5f,  (origV1.y + origV2.y)*0.5f);
          ((VStick)s2.getVerlet()).restore(origV1, origV1o, origV2, origV2o);
          s2.shapify();
          if (debugShow) dbgShape(Color.cyan, s2, 1, "RESTORED");
        }
        return 0;
      } else {
        if (collHV1) spr1.highVelocity = false;
        if (collHV2) spr2.highVelocity = false;
      }
      pene2 = sat(s2, s1, null, mtv2, es2, ee2);
      if (pene2 == 0) {
        // If highvel collision was registered but stationary sat found nothing, restore state
        if (collHV1) {
          if (debugShow) dbgShape(Color.red, s1, 1, "NOCOLL");
          Log.printf("[HV] registered but no static collision, restore %d from %+.1f,%+.1f to %+.1f,%+.1f", spr1.id,
            s1.getX(), s1.getY(), (origV1.x + origV2.x)*0.5f,  (origV1.y + origV2.y)*0.5f);
          ((VStick)s1.getVerlet()).restore(origV1, origV1o, origV2, origV2o);
          s1.shapify();
          if (debugShow) dbgShape(Color.cyan, s1, 1, "RESTORED");
        }
        if (collHV2) {
          if (debugShow) dbgShape(Color.red, s2, 1, "NOCOLL");
          Log.printf("[HV] registered but no static collision, restore %d from %+.1f,%+.1f to %+.1f,%+.1f", spr2.id,
            s2.getX(), s2.getY(), (origV1.x + origV2.x)*0.5f,  (origV1.y + origV2.y)*0.5f);
          ((VStick)s2.getVerlet()).restore(origV1, origV1o, origV2, origV2o);
          s2.shapify();
          if (debugShow) dbgShape(Color.cyan, s2, 1, "RESTORED");
        }
        return 0;
      } else {
        if (collHV1) spr1.highVelocity = false;
        if (collHV2) spr2.highVelocity = false;
      }
      if (debugShow && (collHV1 || collHV2)) {
        dbgShape(Color.magenta, collHV1 ? s1 : s2, 1, null);
      }
      Log.printf("sat %d / %d found (pene1:%+.1f, pene2:%+.1f)", spr1.id, spr2.id, pene1, pene2);

      mtv1.neg();
      mtv2.neg();
      if (pene1 < pene2) {
        s2.findNearestVertex(es1, ee1, mtv1, nearestVertex);
        calcCollisionPoint(es1, ee1, nearestVertex, collision);
      } else {
        s1.findNearestVertex(es2, ee2, mtv2, nearestVertex);
        calcCollisionPoint(es2, ee2, nearestVertex, collision);
      }
      
      mtv1.mul(pene1);
      mtv2.mul(pene2);
      
      if (pene1 < pene2) {
        mtv1.assign(mtv);
      } else {
        mtv2.assign(mtv);
      }
      
      Log.printf("    %d / %d collision  pene:%+.1f  mtv:%+.1f,%+.1f  coll.xy:%+.1f,%+.1f", 
          spr1.id, spr2.id, pene1 < pene2 ? pene1 : pene2,
          mtv.x, mtv.y,
          collision.x, collision.y);

      return pene1 < pene2 ? pene1 : pene2;
    } // intersectSAT

    boolean highVelSAT(Sprite sprHV, Sprite spr) {
      Vec2f es = vectors[vix++];
      Vec2f ee = vectors[vix++];
      float peneHighVel = sat(sprHV.oobShape, spr.shape, null, null, null, null);
      if (peneHighVel == 0) {
        Log.printf("    [HV] %d / %d NO SAT", sprHV.id, spr.id);
        return false;
      }
      peneHighVel = sat(spr.shape, sprHV.oobShape, sprHV.vel, null, es, ee);
      if (peneHighVel != 0) {
        Vec2f frontVertex = sprHV.shape.findFarthestVertexInDirection(sprHV.vel);
        float t = collider.getTrajectoryEdgeCrossing(frontVertex, sprHV.velN, es, ee);
        if (Float.isNaN(t) || t < -0.1f || t > (sprHV.velScalar + 0.1f)) {
          // Should not happen, but for some reason the SAT returns bad edge,
          // iterate through edges to find the best edge
          Log.println(String.format("         BAD SAT EDGE t:%f, e %+.1f,%+.1f--%+.1f,%+.1f", t, es.x, es.y, ee.x, ee.y));
          if (debugShow) dbgLine(Color.red, es, ee, 1, "BAD");
          t = FMAX;
          Vec2f esCand = vectors[vix++]; 
          Vec2f eeCand = vectors[vix++]; 
          for (int i = 0; i < spr.shape.countVertices(); i++) {
            spr.shape.getEdgeVertices(i, esCand, eeCand);
            float candT = collider.getTrajectoryEdgeCrossing(frontVertex, sprHV.velN, esCand, eeCand);
            if (!Float.isNaN(candT) && Math.abs(candT) < Math.abs(t)) {
              t = candT;
              esCand.assign(es);
              eeCand.assign(ee);
            }
          }
          if (t == FMAX) return false; // straight out impossible if the shapes are convex
          Log.println(String.format("         NEW SAT EDGE t:%f, e %+.1f,%+.1f--%+.1f,%+.1f", t, es.x, es.y, ee.x, ee.y));
          if (debugShow) dbgLine(Color.green, es, ee, 1, "NEW");
        }
        boolean trajFail = true;
        if (!Float.isNaN(t) && Math.abs(t) <= (sprHV.velScalar + 0.1f)) {
          float trajDX = sprHV.velN.x * t;
          float trajDY = sprHV.velN.y * t;
          float oldx = sprHV.shape.getX();
          float oldy = sprHV.shape.getY();
          sprHV.shape.moveStatic(trajDX, trajDY);
          sprHV.rest();
          sprHV.shape.move(
            sprHV.velN.x * sprHV.shape.getVerlet().size * 0.5f, 
            sprHV.velN.y * sprHV.shape.getVerlet().size * 0.5f);
          sprHV.shape.shapify();
          if (debugShow) {
            dbgLine(Color.yellow, new Vec2f(oldx, oldy), new Vec2f(oldx+sprHV.vel.x, oldy+sprHV.vel.y), 1, "HV velocity");
          }
          Log.printf("    [HV] COLLISION: spr pen:%+.1f, t:%+.1f, sprite translation dxy:%+.1f,%+.1f,from %+.1f,%+.1f to %+.1f,%+.1f", 
              peneHighVel, t, trajDX, trajDY, oldx, oldy, sprHV.shape.getX(), sprHV.shape.getY());
          trajFail = false;
        } else {
          Log.printf("         traj rejected t:%f velScalar:%f", t, sprHV.velScalar);
        }
        if (trajFail) {
          if (debugShow) {
            Log.printf("    [HV] spr %d trajectory fail vel %+.1f,%+.1f", sprHV.id, sprHV.vel.x, sprHV.vel.y);
            dbgLine(Color.red, frontVertex, sprHV.vel.assign(new Vec2f()).add(frontVertex), 1, "fail: HV-dir");
            dbgLine(Color.red, es, ee, 1, "fail: HV-edge");
          }
          return false;
        }
      } else {
        Log.printf("    [HV] %d / %d NO SAT", sprHV.id, spr.id);
        return false;
      }
      return true;
    } // highvelSat
    
    float sat(Shape sref, Shape sother, Vec2f otherVelHint, Vec2f mtv, Vec2f collEdgeS, Vec2f collEdgeE) {
      float minOverlap = FMAX;
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

      for (int i = 0; i < srefVertices; i++) {
        sref.getEdgeVertices(i, edgeS, edgeE);
        if (debugShow) dbgLine(Color.white, edgeS, edgeE, 1, "e" + i);
        edge = edgeE.sub(edgeS, edge);
        axis = edge.perp(axis);
        axis.norm();
        sref.project(axis, pref);
        sother.project(axis, pother);
        float overlap;
        if ((overlap = pref.overlap(pother)) == 0) {
          Log.printf("        edge %d no coll, projection gap found %f", i, overlap);
          return 0;
        }
        if (sref.getVerlet() != null && overlap > sref.getVerlet().size * 1.5f) {
          // TODO figure out why this happen, surely it is because of containment?
          Log.println("overlap exceed verlet size " + overlap + " > " + sref.getVerlet().size + ": collision ignored");
          return 0; // returning 0 here makes things a lot more robust
        }
        
        float dot = axis.dot(otherVelHint == null ? dirOther : otherVelHint); 
        if (overlap <= minOverlap) {
          if (mtv != null) axis.assign(mtv);
          minOverlap = overlap;
          if (dot < 0) {
            if (collEdgeS != null) edgeS.assign(collEdgeS);
            if (collEdgeE != null) edgeE.assign(collEdgeE);
          }
        }
      }
      if (debugShow) {
        Log.printf("        collision found, pene %+.1f", minOverlap);
        if (collEdgeS != null) dbgLine(Color.magenta, collEdgeS, collEdgeE, 1, null);  
      }
      return minOverlap;
    } // sat

    float getTrajectoryEdgeCrossing(Vec2f s, Vec2f d, Vec2f es, Vec2f ee) {
      float denom = (-d.x) * (es.y - ee.y) - (-d.y) * (es.x - ee.x);
      if (Math.abs(denom) <= NEAR_ZERO) return Float.NaN;
      float nom = (s.x - es.x) * (es.y - ee.y) - (s.y - es.y) * (es.x - ee.x);
      return nom/denom;
    }
  } // class Collider
  
  static class Projection extends Vec2f {
    public Vec2f nearestVertex;
    float minDist = FMAX;
    public Projection() {reset();}
    public void reset() { x = FMAX; y = FMIN; nearestVertex = null; minDist = FMAX;}
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
  
  abstract class Verlet implements Cloneable {
    float x,y,ox,oy,size;
    float gravy, gravx;
    public float getX() {return x;}
    public float getY() {return y;}
    public float getOX() {return ox;}
    public float getOY() {return oy;}
    public float getVX() {return x-ox;}
    public float getVY() {return y-oy;}
    public float angle() {return 0;}
    public float speedSq() {
      float dx = x - ox;
      float dy = y - oy;
      return dx*dx+dy*dy;
    }
    public void rest() {
      ox = x; oy = y;
    }
    public void setGravity(float x, float y) {
      gravx = x; gravy = y;
    }
    public abstract void update(float dt);
    public abstract void move(float dx, float dy);
    public abstract void moveStatic(float dx, float dy);
    public abstract void moveTo(float x, float y);
    public abstract void rotate(float dang);
    public abstract void rotateStatic(float dang);
    public abstract void rotateTo(float ang);
    public abstract void impulse(Vec2f collision, Vec2f movement, float factor);
    public abstract void setInverseFriction(float f);
    public Object clone() throws CloneNotSupportedException { 
      return super.clone(); 
    } 
  } // class Verlet

  class VParticle extends Verlet {
    float ifriction = 0.95f;
    float vx, vy;

    public float getVX() {return vx;}
    public float getVY() {return vy;}
    public void update(float dt) {
      vx = ifriction * (x-ox);
      vy = ifriction * (y-oy);
      ox = x; oy = y;
      x += vx*dt + gravx;
      y += vy*dt + gravy;
    }
    public void move(float dx, float dy) {
      x += dx; y += dy;
    }
    public void moveTo(float x, float y) {
      this.x = x; this.y = y;
    }
    public void moveStatic(float dx, float dy) {
      x += dx; y += dy; ox += dx; oy += dy;
    }
    public void rotate(float dang) {}
    public void rotateStatic(float dang) {}
    public void rotateTo(float ang) {}
    public void rest() {
      super.rest();
      vx = vy = 0;
    }
    public void impulse(Vec2f coll, Vec2f movement, float factor) {
      move(movement.x*factor, movement.y*factor);
    }
    public void setInverseFriction(float f) { ifriction = f; }
  } // class VParticle

  class VStick extends Verlet {
    VParticle v1, v2;
    float isize, sizeDSq2;
    public VStick(float dist) {
      v1 = new VParticle();
      v2 = new VParticle();
      v1.x = -dist / 2;
      v2.x =  dist / 2;
      v1.y = v2.y = 0;
      v1.ox = v1.x; v1.oy = v1.y;
      v2.ox = v2.x; v2.oy = v2.y;
      this.x = this.y = 0;
      this.size = dist;
      this.isize = 1f/dist;
      this.sizeDSq2 = dist * 0.7071f;
    }

    public void save(Vec2f origV1, Vec2f origV1O, Vec2f origV2, Vec2f origV2O) {
      origV1.x = v1.x;
      origV1.y = v1.y;
      origV1O.x = v1.ox;
      origV1O.y = v1.oy;
      origV2.x = v2.x;
      origV2.y = v2.y;
      origV2O.x = v2.ox;
      origV2O.y = v2.oy;
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
      updateCenter();
    }
    public void setInverseFriction(float f) {
      v1.ifriction = v2.ifriction = f;
    }
    public void setGravity(float x, float y) {
      v1.gravx = v2.gravx = x;
      v1.gravy = v2.gravy = y;
      gravx = x; gravy = y;
    }

    public void move(float dx, float dy) {
      x += dx; y += dy;
      v1.move(dx,dy);
      v2.move(dx,dy);
    }
    public void rest() {
      v1.rest(); v2.rest();
      super.rest();
      updateCenter();
    }
    public void update(float dt) {
      v1.update(dt);
      v2.update(dt);
      ox = x;
      oy = y;
      updateCenter();
    }
    public void updateCenter() {
      x = 0.5f*(v1.x + v2.x); 
      y = 0.5f*(v1.y + v2.y); 
    }
    public float angle() {
      return (float)Math.atan2(v2.y - v1.y,v2.x - v1.x);
    }
    public void moveStatic(float dx, float dy) {
      v1.moveStatic(dx, dy);
      v2.moveStatic(dx, dy);
      x += dx; y += dy;
      ox += dx; oy += dy;
      updateCenter();
    }
    public void moveTo(float x, float y) {
      v1.moveTo(x, y);
      v2.moveTo(x, y);
      this.x += x; this.y = y;
      updateCenter();
    }
    public void rotate(float dang) {
      final float cos = (float)Math.cos(dang);
      final float sin = (float)Math.sin(dang);
      updateCenter();
      float fx = (v1.x - x) * cos + (v1.y - y) * -sin; 
      float fy = (v1.x - x) * sin + (v1.y - y) * cos;
      v1.x = x+fx;
      v1.y = y+fy;
      v2.x = x-fx;
      v2.y = y-fy;
    }
    public void rotateStatic(float dang) {
      float o1x = v1.x;
      float o1y = v1.y;
      float o2x = v2.x;
      float o2y = v2.y;
      rotate(dang);
      v1.ox += (v1.x - o1x);
      v1.oy += (v1.y - o1y);
      v2.ox += (v2.x - o2x);
      v2.oy += (v2.y - o2y);
    }
    public void rotateTo(float ang) {
      final float cos = (float)Math.cos(ang);
      final float sin = (float)Math.sin(ang);
      updateCenter();
      float fx = cos * size * 0.5f;
      float fy = sin * size * 0.5f;
      v1.x = x-fx;
      v1.y = y-fy;
      v2.x = x+fx;
      v2.y = y+fy;
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
      
      float gscale = isize * isize;
      
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
    
    public Object clone() throws CloneNotSupportedException { 
      VStick clone = (VStick)super.clone();
      clone.v1 = (VParticle)v1.clone();
      clone.v2 = (VParticle)v2.clone();
      return clone;
    } 
  } // class VStick
  

  abstract class Shape implements Cloneable {
    long collisionGroups = 0;   // bitmask, each bit represents one group
    long collisionFilters = 0;  // bitmask, collisions where a filter bit matches colliders group bit are ignored
    Vec2f aabbMin = new Vec2f();
    Vec2f aabbMax = new Vec2f();
    Verlet verlet;
    boolean resting = true;
    
    public void setInverseFriction(float ifri) {verlet.setInverseFriction(ifri);}
    public float getX() {return verlet.getX();}
    public float getY() {return verlet.getY();}
    public float getVX() {return verlet.getVX();}
    public float getVY() {return verlet.getVY();}
    public float angle() {return verlet.angle();}
    public void update(float dt) {
      if (verlet != null) {
        verlet.update(dt);
        float dx = Math.abs(verlet.getVX());
        float dy = Math.abs(verlet.getVY());
        resting = (dx+dy) <= 0.01f;
      } else {
        resting = true;
      }
    }
    public boolean isResting() { return resting; }
    public Verlet getVerlet() { return verlet; }
    public void move(float dx, float dy) {
      resting = false;
      verlet.move(dx, dy); 
    }
    public void moveStatic(float dx, float dy) {
      verlet.moveStatic(dx, dy); 
    }
    public void moveTo(float x, float y) {
      resting = false;
      verlet.moveTo(x, y); 
    }
    public void rotate(float dang) {
      resting = false;
      verlet.rotate(dang); 
    }
    public void rotateStatic(float dang) {
      verlet.rotateStatic(dang); 
    }
    public void rotateTo(float ang) {
      resting = false;
      verlet.rotateTo(ang); 
    }
    public void setGravity(float x, float y) {
      verlet.setGravity(x,y);
    }
    public int subShapeCount() {
      return 0;
    }
    public Shape getSubShape(int ix) {
      return this;
    }

    abstract int countVertices();
    abstract Vec2f getVertex(int ix);
    abstract Vec2f getEdge(int edge, Vec2f dst);
    abstract void getEdgeVertices(int edge, Vec2f dstStart, Vec2f dstEnd);
    abstract void updateCenter();
    abstract Vec2f getCenter(Vec2f c);
    abstract void shapify();
    abstract void impulse(Vec2f collision, Vec2f movement, float factor);
    abstract Projection project(Vec2f axis, Projection dst);
    abstract Vec2f findNearestVertex(Vec2f edgeS, Vec2f edgeE, Vec2f norm, Vec2f dst);
    abstract Vec2f findFarthestVertexInDirection(Vec2f d);
    public Object clone() throws CloneNotSupportedException {
      Shape s = (Shape)super.clone();
      if (verlet != null) s.verlet = (Verlet)verlet.clone();
      s.aabbMin = (Vec2f)aabbMin.clone();
      s.aabbMax = (Vec2f)aabbMax.clone();
      return s;
    }
  } // class Shape
  class CompoundShape extends Shape {
    List<Shape> shapes = new ArrayList<Shape>();
    List<Integer> vertexCounts = new ArrayList<Integer>();
    List<float[]> dimOffsets = new ArrayList<float[]>();
    int vertices;
    VStick vstick;

    public void addShape(Shape s, float dx, float dy, float dang) {
      shapes.add(s);
      dimOffsets.add(new float[]{dx,dy,dang});
      rehashShapes();
    }
    public int subShapeCount() {
      return shapes.size();
    }
    public Shape getSubShape(int ix) {
      return shapes.get(ix);
    }

    public int countVertices() { return vertices; }
    public Vec2f getVertex(int ix) {
      int six = getShapeIx(ix);
      int offset = vertexCounts.get(six);
      return shapes.get(six).getVertex(ix - offset);
    }
    public Vec2f getEdge(int ix, Vec2f dst) {
      int six = getShapeIx(ix);
      int offset = vertexCounts.get(six);
      return shapes.get(six).getEdge(ix - offset, dst);
    }
    public void getEdgeVertices(int ix, Vec2f dstStart, Vec2f dstEnd) {
      int six = getShapeIx(ix);
      int offset = vertexCounts.get(six);
      shapes.get(six).getEdgeVertices(ix - offset, dstStart, dstEnd);
    }
    public void updateCenter() {
      vstick.updateCenter();
    }
    public Vec2f getCenter(Vec2f c) {
      c.x = vstick.x;
      c.y = vstick.y;
      return c;
    }
    public void shapify() {
      vstick.updateCenter();
      final float a = vstick.angle();
      final float x = vstick.getX();
      final float y = vstick.getY();
      final int shapeCount = shapes.size();
      aabbMin.x = aabbMin.y = FMAX;
      aabbMax.x = aabbMax.y = FMIN;
      for (int ix = 0; ix < shapeCount; ix++) {
        Shape s = shapes.get(ix);
        float[] dimOffset = dimOffsets.get(ix);
        s.moveTo(x + dimOffset[0], y + dimOffset[1]);
        s.rotateTo(a + dimOffset[2]);
        s.shapify();
        if (s.aabbMin.x < aabbMin.x) aabbMin.x = s.aabbMin.x;
        if (s.aabbMin.y < aabbMin.y) aabbMin.y = s.aabbMin.y;
        if (s.aabbMax.x > aabbMax.x) aabbMax.x = s.aabbMax.x;
        if (s.aabbMax.y > aabbMax.y) aabbMax.y = s.aabbMax.y;
      }
    }
    public void impulse(Vec2f collision, Vec2f movement, float factor) {
      resting = false;
      verlet.impulse(collision, movement, factor);
      stickConstraint(vstick, 1f);
      shapify();
      updateCenter();
    }
    public Projection project(Vec2f axis, Projection dst) {
      throw new RuntimeException("Must not project compound shapes");
    }
    public Vec2f findNearestVertex(Vec2f edgeS, Vec2f edgeE, Vec2f norm, Vec2f dst) {
      throw new RuntimeException("Must not find nearest vertex to edge in compound shapes");
    }
    public Vec2f findFarthestVertexInDirection(Vec2f d) {
      throw new RuntimeException("Must not find farthest vertex in direction in compound shapes");
    }

    int getShapeIx(int vertexIx) {
      int vix = 0;
      final int shapeCount = shapes.size();
      while (vix < shapeCount && vertices < vertexCounts.get(vix)) vix++;
      return --vix;
    }

    void rehashShapes() {
      vertices = 0;
      vertexCounts.clear();
      for (Shape s : shapes) {
        vertices += s.countVertices();
        vertexCounts.add(vertices);
      }
    }

    public Object clone() throws CloneNotSupportedException {
      CompoundShape clone = (CompoundShape)super.clone();
      clone.shapes = new ArrayList<Shape>();
      clone.vertexCounts = new ArrayList<Integer>();
      for (Shape s : shapes) clone.shapes.add((Shape)s.clone());
      for (int i : vertexCounts) clone.vertexCounts.add(i);
      return clone; 
    } 
  } // class CompoundShape

  class ConvexShape extends Shape {
    List<Vec2f> defVert = new ArrayList<Vec2f>();
    List<Vec2f> vert = new ArrayList<Vec2f>();
    VStick vstick;
    Vec2f t1 = new Vec2f();
    Vec2f t2 = new Vec2f();

    public ConvexShape() {}
    public ConvexShape(int vertices) { for (int i = 0;i  < vertices; i++) vert.add(new Vec2f());}
    public Verlet getVerlet() {return verlet;}
    public void addVertex(Vec2f v) {defVert.add(v);}
    public Vec2f getVertex(int i) {return vert.get(i);}
    public int countVertices()  {return vert.size();}
    public Vec2f getEdge(int edge, Vec2f dst) {edge%=countVertices(); int nedge=(edge+1)%countVertices(); vert.get(nedge).sub(vert.get(edge), dst); return dst;}
    public void getEdgeVertices(int edge, Vec2f dstStart, Vec2f dstEnd) {edge%=countVertices(); int nedge=(edge+1)%countVertices(); vert.get(edge).assign(dstStart);vert.get(nedge).assign(dstEnd);}
    public void updateCenter() { vstick.updateCenter(); }
    public Vec2f getCenter(Vec2f c) {
      if (verlet != null) {
        c.x=verlet.x;c.y=verlet.y;
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
      verlet = vstick;
      resting = false;
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
      aabbMin.x = aabbMin.y = FMAX;
      aabbMax.x = aabbMax.y = FMIN;
      for (int i = 0; i < verts; i++) {
        Vec2f def = defVert.get(i);
        Vec2f v = vert.get(i);
        v.x = def.x * xframeCX + xframeO + def.y * xframeCY;
        v.y = def.x * yframeCX + yframeO + def.y * yframeCY;
        if (v.x < aabbMin.x) aabbMin.x = v.x;
        if (v.y < aabbMin.y) aabbMin.y = v.y;
        if (v.x > aabbMax.x) aabbMax.x = v.x;
        if (v.y > aabbMax.y) aabbMax.y = v.y;
      }
    }
    
    public void impulse(Vec2f collision, Vec2f movement, float factor) {
      resting = false;
      verlet.impulse(collision, movement, factor);
      stickConstraint(vstick, 1f);
      shapify();
      updateCenter();
    }
    
    public Projection project(Vec2f axis, Projection dst) {
      dst.reset();
      for (Vec2f v : vert) {
        dst.addDot(v.dot(axis));
      }
      return dst;
    }

    public Vec2f findNearestVertex(Vec2f edgeS, Vec2f edgeE, Vec2f norm, Vec2f dst) {
      float minDist = FMAX;
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

    public Object clone() throws CloneNotSupportedException {
      ConvexShape clone = (ConvexShape)super.clone();
      if (verlet != null) clone.vstick = (VStick)clone.verlet;
      clone.t1 = (Vec2f)t1.clone();
      clone.t2 = (Vec2f)t2.clone();
      clone.defVert = new ArrayList<Vec2f>();
      for (Vec2f v : defVert) clone.defVert.add((Vec2f)v.clone());
      clone.vert = new ArrayList<Vec2f>();
      for (Vec2f v : vert) clone.vert.add((Vec2f)v.clone());
      return clone; 
    } 
  } // class ConvexShape
  
  static class Vec2f implements Cloneable {
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
    public float angle() {return (float)Math.atan2(y,x);}
    public float distTo(Vec2f v) {return (float)Math.sqrt((v.x-x)*(v.x-x)+(v.y-y)*(v.y-y));}
    public float distToSq(Vec2f v) {return (v.x-x)*(v.x-x)+(v.y-y)*(v.y-y);}
    public static float dot(Vec2f v1,Vec2f v2) {return v1.x*v2.x+v1.y*v2.y;}
    public Object clone() throws CloneNotSupportedException { return super.clone(); } 
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
    f.setLocation(100,100);
    f.setVisible(true);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    n.start();
  }
}
