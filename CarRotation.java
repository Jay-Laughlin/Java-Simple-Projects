package CarPhysics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class CarRotation
{

	public static void main(String[] args)
	{
		JFrame frame = new JFrame("Top Down Car");
		frame.setSize(1000, 800);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		
		MainPanel panel = new MainPanel(frame);
		frame.add(panel);
		frame.setVisible(true);

		panel.mainLoop();
	}
}
class MainPanel extends JPanel
{
	RotatingPlayer player;
	JFrame parentFrame;
	Point cameraOffset;
	float cameraEasingFactor = 12f;
	
	ArrayList<GridAlignedObstacle> obstacles = new ArrayList<>();
	public MainPanel(JFrame parentFrame)
	{
		player = new RotatingPlayer();
		
		Point2D playerPt = player.getPosition();
		cameraOffset = new Point((int)playerPt.getX(),(int)playerPt.getY());
		this.parentFrame = parentFrame;
		parentFrame.addKeyListener(player);
		
		//hard coded boundaries (read and add level data here
		obstacles.add(new GridAlignedObstacle(-1000,-400,50,3800));
		obstacles.add(new GridAlignedObstacle(2000,-400,50,3800));
		obstacles.add(new GridAlignedObstacle(-1000,-400,3000,50));
		obstacles.add(new GridAlignedObstacle(-1000,3400,3050,50));
		
		//check angled rotation
		obstacles.add(new GridRotatedObstacle(500,200,200,200,30));
		obstacles.add(new GridRotatedObstacle(50,50,50,400,15));
		
	}

	public void mainLoop()
	{
		ArrayList<Collidable> collidables = new ArrayList<>();
		for (GridAlignedObstacle o : obstacles)
			collidables.add(o);
		
		while (true)
		{
			player.update();
			
			player.detectCollision(collidables);
			repaint();
			try
			{
				Thread.sleep(16);
			} catch (Exception e){}
		}
	}
	private void CameraCalculations(Graphics g)
	{
		//direct player connection to the camera
		//Point cameraOffset = player.getPosition();
		
		//easing camera
//		Point playerPosition =  player.getPosition();
//		Point cameraVector = new Point((int)(cameraOffset.getX() - playerPosition.x),
//									(int)(cameraOffset.getY() - playerPosition.y));
//		cameraOffset.setLocation(cameraOffset.getX()-cameraVector.x/cameraEasingFactor, 
//								cameraOffset.getY()-cameraVector.y/cameraEasingFactor);
//		

		float rad = (float)Math.toRadians(player.rotation);
		float fx = (float)Math.cos(rad);
		float fy = (float)Math.sin(rad);
		

		float speed = (float)Math.hypot(player.xv, player.yv);
		float normalizedSpeed = Math.min(speed / player.maxSpeed, 1.0f);


		float minLookAhead = 0;
		float maxLookAhead = 900;
		
		float lookAheadDistance =
		        minLookAhead +
		        normalizedSpeed * (maxLookAhead - minLookAhead);


		float targetX =
		        player.x + fx * lookAheadDistance;
		
		float targetY =
		        player.y + fy * lookAheadDistance;

		

		float dx = (float) (targetX - cameraOffset.getX());
		float dy = (float) (targetY - cameraOffset.getY());
		
		cameraOffset.setLocation(
		    cameraOffset.getX() + dx / cameraEasingFactor,
		    cameraOffset.getY() + dy / cameraEasingFactor
		);


	}
	@Override
	/**
	 * paintComponent is the main drawing for the scene
	 */
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;		
		setBackground(Color.LIGHT_GRAY);		
		
		CameraCalculations(g); //graphics passed for debugging
				
		// other things in the scene (below the car)
		g2.setColor(Color.blue);
		g2.fillRect(500 - cameraOffset.x, 100 - cameraOffset.y, 100, 100);

		//draw a screen grid
		final int gridSize = 200;
		int worldLeft   = cameraOffset.x;
		int worldRight  = cameraOffset.x + 1000;
		int worldTop    = cameraOffset.y;
		int worldBottom = cameraOffset.y + 800;

		int firstGridY = worldTop - (worldTop % 200);
		int firstGridX = worldLeft - (worldLeft % 200);
		

		g2.setColor(new Color(100,100,170));
		
		// Horizontal lines
		for (int y = firstGridY; y <= worldBottom; y += gridSize) {
		    int screenY = y - cameraOffset.y;
		    g2.drawLine(0, screenY, 1000, screenY);
		}
		
		// Vertical lines
		for (int x = firstGridX; x <= worldRight; x += gridSize) {
		    int screenX = x - cameraOffset.x;
		    g2.drawLine(screenX, 0, screenX, 800);
		}

		// draw the car
		player.draw(g2,cameraOffset);

		g2.setColor(Color.DARK_GRAY);
		for (GridAlignedObstacle o : obstacles)
		{
			o.draw(g2,cameraOffset);
		}
		
		// other things in the scene (above the car)
//		g2.setColor(Color.DARK_GRAY);
//		g2.fillRect(200, 300, 100, 100);
	}
}

//transfer object for passing data from the car to the Collidable collision methods
class CollisionQuery {
    public final float vx, vy;
    public final Point2D contactHint;

    public CollisionQuery(float vx, float vy, Point2D contactHint) {
        this.vx = vx;
        this.vy = vy;
        this.contactHint = contactHint;
    }
}
interface Collidable
{
	public Shape getCollisionShape();
	default Point2D getCollisionNormal(CollisionQuery p)
	{
		return null;
	}
}

/**
 * Right now only grid aligned obstacles will work
 */
class GridAlignedObstacle implements Collidable
{
	float x, y, width, height;
	public GridAlignedObstacle(int x, int y, int width, int height)
	{
		this.x = x;
		this.y= y;
		this.width = width;
		this.height = height;
	}
	public void draw(Graphics2D g2, Point cameraOffset)
	{
		g2.fillRect((int)x - cameraOffset.x,(int)y - cameraOffset.y,(int)width,(int)height);		
	}
	public Shape getCollisionShape()
	{
		return new Rectangle((int)x,(int)y,(int)width,(int)height);
	}
	public Point2D getCollisionNormal(CollisionQuery to)
	{
		Point2D p = to.contactHint;
		float left   = (float) p.getX() - x;
	    float right  = (float) (x + width - p.getX());
	    float top    = (float) p.getY() - y;
	    float bottom = (float) (y + height - p.getY());

	    float min = Math.min(Math.min(left, right), Math.min(top, bottom));

	    if (min == left)   return new Point2D.Float(-1, 0);
	    if (min == right)  return new Point2D.Float(1, 0);
	    if (min == top)    return new Point2D.Float(0, -1);
	    return new Point2D.Float(0, 1);
	}
}
/**
 * Collision with these is a bit... off
 * Solution will be to do per line collision detection so that I can absolutely know the normal
 */
class GridRotatedObstacle extends GridAlignedObstacle
{
	int rotation;
	Point2D[] normals;
	public GridRotatedObstacle(int x, int y, int width, int height, int rotation)
	{
		super(x, y, width, height);
		this.rotation = rotation;

		float cos = (float)Math.cos(Math.toRadians(rotation));
		float sin = (float)Math.sin(Math.toRadians(rotation));
		
		normals = new Point2D[]{
		    new Point2D.Float( cos,  sin),   // +X rotated
		    new Point2D.Float(-cos, -sin),   // -X rotated
		    new Point2D.Float(-sin,  cos),   // +Y rotated
		    new Point2D.Float( sin, -cos)    // -Y rotated
		};

	}
	public void draw(Graphics2D g2, Point cameraOffset)
	{

		AffineTransform old = g2.getTransform();
		
	    // 1. Move origin to obstacle center (in screen space)
	    g2.translate(
	        x + width / 2.0 - cameraOffset.x,
	        y + height / 2.0 - cameraOffset.y
	    );
	
	    // 2. Rotate around the new origin
	    g2.rotate(Math.toRadians(rotation));
	
	    // 3. Draw rectangle centered at origin
	    g2.fillRect(
	        (int)-width / 2,
	        (int)-height / 2,
	        (int)width,
	        (int)height
	    );
	
	    // 4. Restore transform
	    g2.setTransform(old);

	}
	public Shape getCollisionShape()
	{

	    Rectangle2D rect = new Rectangle2D.Float(x, y, width, height);

	    AffineTransform t = new AffineTransform();
	    t.rotate(Math.toRadians(rotation),
	             x + width / 2.0,
	             y + height / 2.0);

	    return t.createTransformedShape(rect);	
	}

	public Point2D getCollisionNormal(CollisionQuery q)
	{	

		float cx = x + width  / 2.0f;
		float cy = y + height / 2.0f;


		// Transform contact point into obstacle-local space
		float rad = (float)Math.toRadians(-rotation);
		float cos = (float)Math.cos(rad);
		float sin = (float)Math.sin(rad);

		float localX =  (float)((q.contactHint.getX() - cx) * cos
		                       - (q.contactHint.getY() - cy) * sin);
		float localY =  (float)((q.contactHint.getX() - cx) * sin
		                       + (q.contactHint.getY() - cy) * cos);


		float dx = Math.abs(localX) - width  / 2f;
		float dy = Math.abs(localY) - height / 2f;
		
		// Choose the dominant penetration axis
		boolean hitLongSide = dx > dy;
		

		if (hitLongSide) {
		    // normal is ±X face in local space
		    float sign = Math.signum(localX);
		    return new Point2D.Float(sign * cos, sign * sin);
		} else {
		    // normal is ±Y face in local space
		    float sign = Math.signum(localY);
		    return new Point2D.Float(-sign * sin, sign * cos);
		}


	}
}
/*
 * Car class
 * update is the actions/moving
 * collision is called from the game loop
 * 	and the handle method is called when there is a collision
 */
class RotatingPlayer implements KeyListener, Collidable
{
	// Position & velocity (position is CENTER of car)
	float x = 0, y = 0;
	float xv = 0, yv = 0;
	float throttle; //stores acceleration

	// Motion tuning
	float maxSpeed = 40f;
	float rotation = 0f; // degrees
	float rotationAmount = 7.5f; // base steering strength
	float maxSteerSpeed = 7.0f; // speed at which steering is fully enabled

	//acceleration
	float baseEnginePower = 0.6f;
	float reverseFactor = 0.4f; // reverse is weaker
	
	//breaking
	float brakeStrength = 0.4f;
	
	// anisotropic friction
	float forwardFriction = 0.98f;  // rolling friction
	float sideFriction    = 0.85f;  // lateral friction (stronger)
	float collisionalRotationFactor = 100f;
	
	// Input buffer
	boolean up, down, left, right, brake;

	// Car dimensions/size
	final int carLength = 40;
	final int carWidth = 22;

	
	boolean collision;
	
	/**
	 * updates position and rotation of the car based on the input buffer This code
	 * calculates the forward direction and does movement related to that
	 */
	public void update()
	{
		float rad = (float) Math.toRadians(rotation);

		// Forward direction
		float fx = (float) Math.cos(rad);
		float fy = (float) Math.sin(rad);
		float sx = -fy;
		float sy =  fx;

		// Forward speed (projection of velocity)
		float forwardSpeed = xv * fx + yv * fy;
		float speed = Math.abs(forwardSpeed);
		float sideSpeed    = xv * sx + yv * sy;

		// anisotropic friction
		forwardSpeed *= forwardFriction;  // rolling friction
		sideSpeed    *= sideFriction;  // lateral friction (stronger)

		//Note this braking only effects forward motion
		//brakes may slightly effect side/lateral motion but
		//most lateral dampening is based on tire friction and 
		//is applied regardless of braking
		float effectiveBrake =
		        brakeStrength / (1.0f + Math.abs(forwardSpeed) * 0.1f);
		
		if (brake) {
		    if (forwardSpeed > 0) {
		        forwardSpeed = Math.max(forwardSpeed - effectiveBrake, 0);
		    } else if (forwardSpeed < 0) {
		        forwardSpeed = Math.min(forwardSpeed + effectiveBrake, 0);
		    }
		    //breaking removes some lateral steering/stability
		    	//typically only handbreaks lock the wheels, in modern ABS this is probably not true
		    sideSpeed *= 1.05f; //can only do if we know it is less than normal side friction
		}			
		
		//acceleration
		{
			//forward or reverse
			int driveDir = 0;			//coast, slow down
			if (up)   driveDir =  1;	//accelerate, strong
			if (down) driveDir = -1;	//accelerate (but subtract from forward), weaker
			
			if (driveDir != 0)
			    throttle = Math.min(throttle + 0.05f, 1.0f);
			else
			    throttle = Math.max(throttle - 0.08f, 0.0f);
				
			float enginePower = (driveDir > 0)
			        			? baseEnginePower
			        			: baseEnginePower * reverseFactor;

			float normalized = Math.min(Math.abs(forwardSpeed) / maxSpeed, 1.0f);
			float engineFactor = 1.0f - normalized * normalized;

			float engineForce = throttle * enginePower * engineFactor * driveDir;
		
			forwardSpeed += engineForce;
		}
		
		// recombine speeds to get the velocity components
		xv = forwardSpeed * fx + sideSpeed * sx;
		yv = forwardSpeed * fy + sideSpeed * sy;

		//Steering:
		// are we going forward or backwards right now?
		float direction = Math.signum(forwardSpeed);

		// Steering scales with forward motion but clamps at 1
		float normalized = Math.min(speed / maxSteerSpeed, 1.0f);
		//too aggressive
		//float steerFactor = normalized * (1.0f - normalized);
		//float steerFactor = (float)Math.sqrt(normalized) * (1.0f - normalized);

		float steerFactor =
		        0.5f * normalized +
		        0.5f * (float)Math.sqrt(normalized) * (1.0f - normalized);

		if (brake) {
		    steerFactor *= 0.6f;
		}	
		
		if (left)
		{
			rotation -= rotationAmount * steerFactor * direction;
		}
		if (right)
		{
			rotation += rotationAmount * steerFactor * direction;
		}

		// constrain our rotation between 0 and 360
		// note: we don't need this, but... if we rotated a bunch... we could have
		// overflow issues, plus its clearer this way if we want to output it or see it
		rotation = (rotation + 360) % 360;

		// modify position based on velocity
		x += xv;
		y += yv;

	}
	public Point getPosition()
	{
		return new Point((int)x,(int)y);
	}
	
	//get the 4 points of the car
	Point2D[] getWorldCorners() 
	{
	    Point2D[] corners = new Point2D[4];

	    float hx = carLength / 2f;
	    float hy = carWidth  / 2f;

	    float rad = (float)Math.toRadians(rotation);
	    float cos = (float)Math.cos(rad);
	    float sin = (float)Math.sin(rad);

	    // local corners
	    float[][] local = {
	        {-hx, -hy}, // front-left
	        { hx, -hy}, // front-right
	        { hx,  hy}, // rear-right
	        {-hx,  hy}  // rear-left
	    };

	    for (int i = 0; i < 4; i++) {
	        float lx = local[i][0];
	        float ly = local[i][1];

	        float wx = x + lx * cos - ly * sin;
	        float wy = y + lx * sin + ly * cos;

	        corners[i] = new Point2D.Float(wx, wy);
	    }
	    return corners;
	}
	public Shape getCollisionShape()
	{
		AffineTransform transform = new AffineTransform();
		Rectangle2D rect = new Rectangle2D.Float(x-carLength/2,y-carWidth/2,carLength,carWidth);
		// Rotate around the center of the rectangle
		transform.rotate(Math.toRadians(this.rotation), rect.getX() + rect.getWidth() / 2, rect.getY() + rect.getHeight() / 2);
		
		Shape rotatedRect = transform.createTransformedShape(rect);
		return rotatedRect;
	}
	// we should use generics to take in the Collidables like:
	// ArrayList<? extends Collidable>
	// but this is a fundamentals 3 topic
	public void detectCollision(ArrayList<Collidable> collisionShapes)
	{
		collision = false;
		Shape ourShape = getCollisionShape();
		Area ourArea = new Area(ourShape);
		
		for (Collidable c : collisionShapes)
		{
			Area area = new Area(c.getCollisionShape());
			boolean collide = checkCollision(ourArea, area);
			collision |= collide;
			if (collide)
				handleCollision(c, area);
		}
	}
	
	public boolean checkCollision(Area ourArea, Area theirArea)
	{					
		theirArea.intersect(ourArea);
		return !theirArea.isEmpty();		
	}
	public void handleCollision(Collidable c, Area area)
	{
		//normal of the surface we are colliding with
		Point2D collisionNormal = c.getCollisionNormal(new CollisionQuery(xv,yv,getPosition()));
		float nx = (float) collisionNormal.getX();
		float ny = (float) collisionNormal.getY();
		
		//simple move out of the object
			Rectangle2D overlapBounds = area.getBounds2D();
			float penetrationDepth = (float)Math.min(
			    overlapBounds.getWidth(),
			    overlapBounds.getHeight()
			);
			
			//move car out of the object we are colliding with
			x += nx * penetrationDepth;
			y += ny * penetrationDepth;
		
		//manually loop to move out of what we're colliding with:
//			int iterations = 0;
//			int max = 20;
//			while (intersecting && iterations < max) {
//			    x += nx * step;
//			    y += ny * step;
//			}

			
			
		//stop velocity in the direction of the normal
		float vn = xv * nx + yv * ny;
		if (vn < 0) {
		    xv -= vn * nx;
		    yv -= vn * ny;
		}
		

//		// Keep only tangential velocity explicitly
//		float vt = xv * (-ny) + yv * nx;
//
//		xv = vt * (-ny);
//		yv = vt * nx;


		
		//get which corner collided with the object:
		Point2D overlapCenter = new Point2D.Float(	//center of the other object
		    (float)(overlapBounds.getX() + overlapBounds.getWidth() / 2),
		    (float)(overlapBounds.getY() + overlapBounds.getHeight() / 2)
		);
		Point2D corners[] = getWorldCorners();

		//loop through the corners to see which is the closest
		int closestIndex = 0;
		double bestDist2 = corners[0].distanceSq(overlapCenter);
		
		for (int i = 1; i < corners.length; i++) {
		    double d2 = corners[i].distanceSq(overlapCenter);
		    if (d2 < bestDist2) {
		        bestDist2 = d2;
		        closestIndex = i;
		    }
		}
		
		Point2D hitCorner = corners[closestIndex];

		float rx = (float)(hitCorner.getX() - x);
		float ry = (float)(hitCorner.getY() - y);
		float spin = rx * ny - ry * nx;

		float rad = (float) Math.toRadians(rotation);

		// Forward direction
		float fx = (float) Math.cos(rad);
		float fy = (float) Math.sin(rad);
		float forwardSpeed = xv * fx + yv * fy;
		float speed = Math.abs(forwardSpeed);
		float impactScale = Math.min(speed / maxSpeed, 1.0f);
		rotation += Math.signum(spin) * collisionalRotationFactor * impactScale;

		
	}

	public void draw(Graphics2D g2, Point cameraOffset)
	{
		//put the car in the center of the screen
		//Note: should use screen width and height instead of hard-coded values
		g2.translate(500, 400);
		
		AffineTransform old = g2.getTransform();

		// Move origin to car center
		g2.translate(x - cameraOffset.x, y - cameraOffset.y);

		// Rotate around center
		g2.rotate(Math.toRadians(rotation));

		// Draw body centered at origin
		// starting at half the height and width draw the rectangle of the car - we want
		// to draw the car around 0,0 as this is where the rotation is going to be
		if (collision)
			g2.setColor(Color.red);
		else
			g2.setColor(Color.GRAY);
		g2.fillRect(-carLength / 2, -carWidth / 2, carLength, carWidth);

		// Front indicator
		g2.setColor(Color.RED);
		// g2.fillOval(CAR_W / 4, -CAR_H / 4, CAR_H / 2, CAR_H / 2);

		g2.setStroke(new BasicStroke(2));
		g2.drawLine(carLength / 4, carWidth / 2, carLength / 2, 0);
		g2.drawLine(carLength / 4, -carWidth / 2, carLength / 2, 0);
		g2.drawLine(0, 0, carLength / 2, 0);
		g2.setStroke(new BasicStroke(1));

		// restore transform
		g2.setTransform(old);
		
		// debug collision shape
//		g2.setColor(Color.magenta);
//		Shape s = getCollisionShape();
//		g2.draw(s);
		
		
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
		case KeyEvent.VK_W:
			up = true;
			break;
		case KeyEvent.VK_S:
			down = true;
			break;
		case KeyEvent.VK_A:
			left = true;
			break;
		case KeyEvent.VK_D:
			right = true;
			break;
		case KeyEvent.VK_SPACE:
			brake = true;
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
		case KeyEvent.VK_W:
			up = false;
			break;
		case KeyEvent.VK_S:
			down = false;
			break;
		case KeyEvent.VK_A:
			left = false;
			break;
		case KeyEvent.VK_D:
			right = false;
			break;
		case KeyEvent.VK_SPACE:
			brake = false;
			break;
		}
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}	
}
