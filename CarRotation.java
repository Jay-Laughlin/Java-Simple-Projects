package CarPhysics;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import javax.swing.*;

public class CarRotation
{

	public static void main(String[] args)
	{
		JFrame frame = new JFrame("Top‑Down Car");
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
	public MainPanel(JFrame parentFrame)
	{
		player = new RotatingPlayer();
		this.parentFrame = parentFrame;
		parentFrame.addKeyListener(player);
	}

	public void mainLoop()
	{
		while (true)
		{
			player.update();
			repaint();
			try
			{
				Thread.sleep(16);
			} catch (Exception e)
			{
			}
		}
	}

	@Override
	/**
	 * paintComponent is the main drawing for the scene
	 */
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		setBackground(Color.LIGHT_GRAY);

		Graphics2D g2 = (Graphics2D) g;

		// other things in the scene (below the car)
		g2.setColor(Color.blue);
		g2.fillRect(500, 100, 100, 100);

		// draw the car
		player.draw(g2);

		// other things in the scene (above the car)
		g2.setColor(Color.DARK_GRAY);
		g2.fillRect(200, 300, 100, 100);
	}
}

/*
 * ========================= Game Panel + Player: suggest divesting these
 * objects in a larger project =========================
 */
class RotatingPlayer implements KeyListener
{

	// Position & velocity (position is CENTER of car)
	float x = 300, y = 300;
	float xv = 0, yv = 0;

	// Motion tuning
	float acceleration = 0.35f;
	float rotation = 0f; // degrees
	float rotationAmount = 10.5f; // base steering strength
	float maxSteerSpeed = 7.0f; // speed at which steering is fully enabled

	// Input buffer
	boolean up, down, left, right;

	// Car dimensions/size
	final int carLength = 40;
	final int carWidth = 22;

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

		// Forward speed (projection of velocity)
		float forwardSpeed = xv * fx + yv * fy;
		float speed = Math.abs(forwardSpeed);

		// Acceleration
		if (up)
		{
			xv += fx * acceleration;
			yv += fy * acceleration;
		}
		if (down)
		{
			xv -= fx * acceleration;
			yv -= fy * acceleration;
		}

		// are we going forward or backwards right now?
		float direction = Math.signum(forwardSpeed);

		// Steering scales with forward motion but clamps at 1
		float steerFactor = Math.min(speed / maxSteerSpeed, 1.0f);
		steerFactor *= steerFactor; // soften high speed

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

		// Simple friction
		xv *= 0.92f;
		yv *= 0.92f;
	}

	public void draw(Graphics2D g2)
	{
		AffineTransform old = g2.getTransform();

		// Move origin to car center
		g2.translate(x, y);

		// Rotate around center
		g2.rotate(Math.toRadians(rotation));

		// Draw body centered at origin
		// starting at half the height and width draw the rectangle of the car - we want
		// to draw the car around 0,0 as this is where the rotation is going to be
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
		}
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}
}
