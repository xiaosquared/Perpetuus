package canon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import processing.core.PApplet;
import processing.core.PVector;

public class Phrase {
	public int TIME_OUT = 250; 	// time out between phrases
	
	int last_note_time = 0;				// time last note in phrase was released
	
	int note_on_count = 0;
	int note_off_count = 0;
	
	private ArrayList<Ball> phrase_balls;
	private HashMap<Integer, ArrayList<Ball>> midi_ball_map; // balls hashed by their midi numbers
	
	ArrayList<PVector> curve_pts;		// updated every frame to draw line between points
	
	boolean LOOPING = false;
	
	int color;
	
	public Phrase(boolean loop, int color) {
		phrase_balls = new ArrayList<Ball>();
		midi_ball_map = new HashMap<Integer, ArrayList<Ball>>();
		curve_pts = new ArrayList<PVector>();
		LOOPING = loop;
		this.color = color;
	}
	
	public Phrase() {
		this(false, 0);
	}
	
	/**
	 * Returns whether this phrase still accepts input notes or not
	 */
	public boolean timedOutInput(int time) {
		return (note_on_count > 0) && (note_off_count == note_on_count) && ((time - last_note_time) > TIME_OUT);
	}
	
	public void addBall(Ball ball) {
		// add to phrase
		phrase_balls.add(ball);
		
		// add to map
		ArrayList<Ball> balls_list = midi_ball_map.get(Integer.valueOf(ball.note));
		if (balls_list == null) {
			balls_list = new ArrayList<Ball>();
			midi_ball_map.put(Integer.valueOf(ball.note), balls_list);
		}
		balls_list.add(ball);
		
		// update count
		note_on_count ++;
	}
	
	public void updateBall(int note, int velocity, int note_off_time) {
		// find ball from HashMap
		ArrayList<Ball> balls_list = midi_ball_map.get(Integer.valueOf(note));
		int list_length = balls_list.size();
		if ((balls_list == null) || (list_length == 0))
			return;
		Ball ball = balls_list.get(list_length-1);
		
		// set stuff for this ball
		ball.note_velocity_off = velocity;
		ball.setNoteDuration(note_off_time);
		
		// update stuff for the phrase
		note_off_count++;
		last_note_time = note_off_time;
	}
	
	public Iterator<Ball> getBalls() {
		return phrase_balls.iterator();
	}
	
	public int getLength() {
		return phrase_balls.size();
	}
	
	public void removeBall(Iterator<Ball> iterator, Ball ball) {
		// remove it from the ArrayList with iterator
		iterator.remove();
		
		// remove from the hashmap
		removeFromMap(ball);
	}
	
	private void removeFromMap(Ball ball) {
		ArrayList<Ball> balls_list = midi_ball_map.get(Integer.valueOf(ball.note));
		if (balls_list == null)
			return;
		balls_list.remove(ball);
	}
	
	public void updateCurve(int num, int smooth_times) {
		num++;
		
		curve_pts.clear();
		Iterator<Ball> balls = phrase_balls.iterator();
		
		Ball b0;
		if (balls.hasNext()) {
			b0 = balls.next();
			curve_pts.add(new PVector(b0.screen_xy.x, b0.screen_xy.y));
		}
		else
			return;
		
		while(balls.hasNext()) {
			Ball b1 = balls.next();
			for (int i = 0; i < num; i++)
				curve_pts.add(new PVector(PApplet.lerp(b0.screen_xy.x, b1.screen_xy.x, (float) i/num), 
										PApplet.lerp(b0.screen_xy.y, b1.screen_xy.y, (float) i/num)));
			curve_pts.add(new PVector(b1.screen_xy.x, b1.screen_xy.y));
			b0 = b1;
		}
		
		// smooth it a few times
		while (smooth_times > 0) { 
			smoothCurve(curve_pts);
			smooth_times --;
		}
	}
	
	private void smoothCurve(ArrayList<PVector> c) {  // n is number of influence points to each side
		int curve_size = c.size();

		if (curve_size < 2)
			return;

		for (int i = 1; i <= curve_size - 2; i++) {
			PVector p = c.get(i);
			PVector p_l = c.get(i-1);
			PVector p_r = c.get(i+1);

			// edges are only influenced by one point around
			if ((i == 1) || (i == curve_size-2)) {
				p.x = .5f * p.x + (.25f * p_l.x) + (.25f * p_r.x);
				p.y = .5f * p.y + (.25f * p_l.y) + (.25f * p_r.y);
			} 

			// others influenced by both
			else {
				PVector p_l2 = c.get(i-2);
				PVector p_r2 = c.get(i+2);    
				p.x = (.25f * p.x) + (.25f * p_l.x) + (.25f * p_r.x) + (.125f * p_l2.x) + (.125f * p_r2.x);
				p.y = (.25f * p.y) + (.25f * p_l.y) + (.25f * p_r.y) + (.125f * p_l2.y) + (.125f * p_r2.y);
			}
		}
	}
}
