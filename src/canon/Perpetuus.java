package canon;

import java.awt.Container;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.BasicStroke;
import java.awt.Graphics2D;

import processing.core.PApplet;
import processing.core.PGraphicsJava2D;
import processing.core.PVector;
import rwmidi.*;

public class Perpetuus extends PApplet {
	private static final long serialVersionUID = 1L;
	 
	// settings
	private boolean USE_MIDI_INPUT = true;
	private boolean USE_MIDI_OUTPUT = true;
	private boolean DEBUG = false;
	private boolean VARIABLE_HEIGHT = false;
	
	// this should be false, unless:
	// 1. you're Daesun
	// 2. you're using Ableton to input MIDI
	private boolean DAESUN_ABLETON_INPUT = true;
	
	float y_scale = 6;		// height scale of balls
	int y_offset = 215;		// adjustment for projection onto keys
	int rad = 7;			// radius of balls
	
	// MIDI
	private MidiInput input;
	private MidiOutput output;
	
	// Canon;
	private Canon canon;
	private Phrase current_phrase;
	
	// modes
	private boolean sus_on = false;
	private boolean record = true;
	
	// concurrency issues
	boolean lock = false;
	int lock_delay = 2;
	
	// dotted line
	float[] dashes = {0.5f, 20f};
	BasicStroke pen;
	
	public void setup() {
		size(1024, 535 + y_offset); 
		
		stroke(255);
		fill(255);
		smooth();
	
		pen = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER,
					1.0f, dashes, 0.0f);

		Graphics2D g2 = ((PGraphicsJava2D) g).g2;
		g2.setStroke(pen);	
		
		
		if (USE_MIDI_INPUT)
			input = RWMidi.getInputDevices()[0].createInput(this);
		if (USE_MIDI_OUTPUT)
			output = RWMidi.getOutputDevices()[0].createOutput();
		
		
		if (DAESUN_ABLETON_INPUT){
			System.out.println("Input devices:");
			for (MidiInputDevice x : RWMidi.getInputDevices()){
				System.out.println(x.toString());
			}
			System.out.println("Output devices:");
			for (MidiOutputDevice x : RWMidi.getOutputDevices()){
				System.out.println(x.toString());
			}
			
			input = RWMidi.getInputDevices()[0].createInput();
			// fix this to just output to "Java Sound Synthesizer Sun Microsystems"
			output = RWMidi.getOutputDevices()[2].createOutput();
		}
	
		//println(RWMidi.getOutputDeviceNames());
		
		canon = new Canon();
		current_phrase = new Phrase();
		canon.addPhrase(current_phrase);		
	
	}

	public void draw() {
		background(0);
		
		while(lock) 
			delay(lock_delay);
		lock = true;
		
		// go through each Phrase in the Canon
		Iterator<Phrase> canon_iterator = canon.getPhrases();
		
		while(canon_iterator.hasNext()) {
			Phrase phrase = canon_iterator.next();
			int phrase_length = phrase.getLength();
			
			// go through each Ball in the Phrase
			Iterator<Ball> phrase_iterator = phrase.getBalls();
			
			//fill(phrase.color);
			//stroke(phrase.color);
			
			while(phrase_iterator.hasNext()) {
				Ball ball = phrase_iterator.next();		// get ball
				int t = millis();						// get time
				
				// if ball is not alive anymore
				if (!ball.isAlive) {
					phrase.removeBall(phrase_iterator, ball);
					phrase_length--;
					
					// if the phrase is done, remove it from canon
					if (phrase.timedOutInput(t) && (phrase_length == 0) && (phrase.note_on_count > 0))
						canon_iterator.remove();
				}
				else {
//					if (t < (ball.t_end - ball.t0)/2 + ball.t0) {
//						ball.computePosition(t);
//					}
//					drawBall(ball.screen_xy, ball.y, phrase.LOOPING && (ball.loop_number > 1));
					// compute ball position
					if (t < ball.t_end) {
						ball.computePosition(t);
						drawBall(ball.screen_xy, ball.y, phrase.LOOPING && (ball.loop_number > 1));
						
						// a little before ball hits bottom, send note on to Disklavier
						if (t > ball.t_send_note_on && ball.state == BallStatus.INIT) {
							if (USE_MIDI_OUTPUT) {
								output.sendNoteOn(0, ball.note, ball.note_velocity);
								ball.setState(BallStatus.NOTE_ON);
								
								if (DEBUG)
									println("SENT note on " + ball.note + " velocity: " + ball.note_velocity);
							}
							
						}
						// after a bit, send note off. sending some extra note_off messages just in case they don't get through
						else if (t > ball.t_send_note_off) {
							if (USE_MIDI_OUTPUT) { 
								output.sendNoteOff(0, ball.note, ball.note_velocity_off);
								if (DEBUG && ball.state != BallStatus.NOTE_OFF)
									println("SENT note off " + ball.note + " velocity: " + ball.note_velocity_off);
								ball.setState(BallStatus.NOTE_OFF);
							}
						}
						
					} else {
						if (phrase.LOOPING && (ball.loop_number > 1)) {
							ball.setNewTimes(t);
							ball.loop_number--;
						} else
							ball.isAlive = false;
						
						if (DEBUG)
							println("--------");
					}
				}
			}
			if (phrase.LOOPING) {
				phrase.updateCurve(5, 3);
				drawCurve(phrase.curve_pts);
			}
		}
		lock = false;
	}
	
	private void drawBall(PVector xy, float y, boolean loop) {
		y = height - y/y_scale;
		xy.y = y;
		
//		if (loop) {
//			fill(255);
//		} else {
//			fill(0);
//		}
			
		ellipse(xy.x, y - y_offset, rad, rad);
	}
	
	private void drawCurve(ArrayList<PVector> curvePts) {
		for (int i = 0; i < curvePts.size(); i++)
			if (i != 0)
				line(curvePts.get(i).x, curvePts.get(i).y - y_offset, curvePts.get(i-1).x, curvePts.get(i-1).y);
	}
	
	public void noteOnReceived(Note n) {
		while(lock) 
			delay(lock_delay);
		lock = true;
		
		if (DEBUG)
			println("note on " + n.getPitch() + ", " + n.getVelocity());
		
		if (current_phrase.timedOutInput(millis()) || (canon.numPhrases() == 0)) {
			current_phrase = new Phrase(sus_on, (int) random(50, 255));
			canon.addPhrase(current_phrase);
		}
		current_phrase.addBall(new Ball(n.getPitch(), n.getVelocity(), millis(), sus_on, VARIABLE_HEIGHT, DEBUG));
		
		lock = false;
	}
	
	public void noteOffReceived(Note n) {
		while(lock)
			delay(lock_delay);
		lock = true;
		
		if (DEBUG)
			println("note off " + n.getPitch() + ", " + n.getVelocity());
		
		current_phrase.updateBall(n.getPitch(), n.getVelocity(), millis());
		
		lock = false;
	}
	
	public void controllerChangeReceived(Controller c) {
		if (c.getCC() != 66)	// 66 is sus. 64 is damper. 67 is soft
			return;
		
		if (c.getValue() == 127) {
			sus_on = true;
		//	current_phrase = new Phrase(sus_on, (int) random(50, 255)); // these cause the program to crash
		//	canon.addPhrase(current_phrase);
		//	current_phrase.TIME_OUT = 10000;
		}
		else {
			sus_on = false;
		//	current_phrase = new Phrase(sus_on, (int) random(50, 255));
		//	canon.addPhrase(current_phrase);
		}
		
		//if (DEBUG)
			println("sus pedal: " + sus_on + " " + c.getValue());
	}
	
	public void mousePressed() {
		println("test note output");
		output.sendNoteOn(0, 50, 20);
		delay(500);
		output.sendNoteOff(0, 50, 20);
	}
	
	public void keyPressed() {
		if (key == 'r') {
			record = !record;
			println("RECORDING: " + record);
		} else if (key == 'p') {
			println("PLAY!");
		}
			
	}
	
	public static void main(String[] args) {
		/* 
		 * places window on second screen automatically if there's additional display
		 * 
		 */
		int primary_width;
		int screen_y = 0;
		
		GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice devices[] = environment.getScreenDevices();
		String location;
		if (devices.length > 1) {
			primary_width = devices[0].getDisplayMode().getWidth();
			location = "--location=" +primary_width+ "," + screen_y;
		} else {
			location="--location=0,0";
		}
	    PApplet.main(new String[] { location, Perpetuus.class.getName() });
	}
 }
