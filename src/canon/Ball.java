package canon;

import processing.core.PApplet;
import processing.core.PVector;

public class Ball {

	// MIDI note params
	int note = -1;
	int note_velocity = -1;
	int note_velocity_off;
	
	// position params
	float v0 = 4;
	float g = .004f;
	//float g = .003f;
	float y = 0;
	PVector screen_xy; // position on the screen
	
	// times
	float t0;				// initial time
	float t_end; 			// when the ball hits 0 again
	float t_send_note_on;
	float t_send_note_off;
	float t_offset = 500;	 
	float duration = 0;
	float note_width = 8.41f;
	float x_offset = 90f;
	
	// state
	BallStatus state = BallStatus.INIT;
	boolean isAlive = true;
	int loop_number = 1;
	
	//debug
	boolean DEBUG = false;
	
	public Ball(int note, int note_velocity, float t0, boolean loop, boolean vary_height, boolean debug) {
		this.DEBUG = debug;
		
		// midi info
		this.note = note;
		//this.note_velocity = Math.max(1, note_velocity - 7); // a little softer because Disklavier tends to hit harder than people
		this.note_velocity = Math.max(1, note_velocity); // a little softer because Disklavier tends to hit harder than people
		
		// for animation
		this.t0 = t0;
		screen_xy = new PVector((float)((note-21) * note_width + x_offset), 0);
		
		// times
		t_end = v0/g + t0;
		t_send_note_on = t0 + 500000; // start these with some big number. they will be reset later
		t_send_note_off = t0 + 500000;
		
		// other settings
		if (loop)
			loop_number = 10;
		
		if (vary_height)
			this.v0 = PApplet.map(note_velocity, 0, 100, 2.5f, 5);	
	}
	
	public Ball(int note, int note_velocity, float t0, boolean vary_height, boolean debug) {
		new Ball(note, note_velocity, t0, false, vary_height, debug);
	}
	
	public float computePosition(float t) {
		t-=t0;
		y = (v0 * t) - (g * t * t);
		return y;
	}
	
	public void setState(BallStatus s) {
		state = s;
	}
	
	public void setNewTimes(float t) {
		// set new times
		t0 = t;
		t_end = v0/g + t;
		t_send_note_on = t_end - t_offset;
		t_send_note_off = Math.min(t_send_note_on + duration, t_end - 50);
	
		// reset status
		state = BallStatus.INIT;
	}
	
	public void setNoteDuration(int t) {
		duration = t - t0;
		
		if (DEBUG)
			System.out.println("duration: " + duration);
		
		t_send_note_on = t_end - t_offset;
		t_send_note_off = Math.min(t_send_note_on + duration, t_end - 50);
	}
}
