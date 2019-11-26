package com.pelleplutt.mctexedit;


public class Transformer {
  Coord p1 = new Coord();
  Coord p2 = new Coord();
  Coord p3 = new Coord();
  Coord p4 = new Coord();
  Coord n1 = new Coord();
  Coord n2 = new Coord();
  Coord n3 = new Coord();
  Coord n4 = new Coord();
  
  public void setCoords(Coord ip1, Coord ip2, Coord ip3, Coord ip4) {
    p1.set(ip1);
    p2.set(ip2);
    p3.set(ip3);
    p4.set(ip4);
    n1.normal(p4, p1);
    n2.normal(p1, p2);
    n3.normal(p2, p3);
    n4.normal(p3, p4);
  }
  
  float dotDiffNorm(Coord p, Coord pn, Coord n) {
    return (p.x - pn.x) * n.x + (p.y - pn.y) * n.y;  
  }

  public void calc(Coord xy, Coord uv) {
    float w1 = dotDiffNorm(xy, p1, n1);
    float w2 = dotDiffNorm(xy, p2, n2);
    float w3 = dotDiffNorm(xy, p3, n3);
    float w4 = dotDiffNorm(xy, p4, n4);
    
    uv.x = w1 / (w1 + w3);
    uv.y = w2 / (w2 + w4);
  }
  
  public static class Coord {
    float x, y;
    public Coord() {}
    public Coord(float xx, float yy) { x = xx; y = yy; }
    public void set(Coord c) { x = c.x; y = c.y; }
    public void set(float xx, float yy) { x = xx; y = yy; }
    void normal(Coord a, Coord b) { x = b.y - a.y; y = -(b.x - a.x); }
    public void rotate(Coord c, double ang, Coord pivot) {
      float rx = c.x - pivot.x;
      float ry = c.y - pivot.y;
      float cos = (float)Math.cos(ang);
      float sin = (float)Math.sin(ang);
      x = rx * cos - ry * sin + pivot.x;
      y = rx * sin + ry * cos + pivot.y;
    }
  }
}
