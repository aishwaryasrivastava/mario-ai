package ch.idsia.agents.controllers;

import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
//import java.util.Random;
import java.lang.Math;

public class SarsaAgent extends BasicMarioAIAgent implements Agent
{
    // We let d-pad buttons be mutually exclusive to limit number of Q-values
    private static final int NUM_DIRECTIONS = 4; // left, right, down, none 
    private static final int NUM_BUTTONS = 2; // jump, speed
    private static final int NUM_ACTIONS = NUM_DIRECTIONS * NUM_BUTTONS * NUM_BUTTONS;

    // left, right, down, jump, speed, up
    private static final boolean[][] actionMap =
	new boolean[][]{{false, false, false, false, false, false}, // no action
			{true, false, false, false, false, false},  // left
			{false, true, false, false, false, false},  // right
			{false, false, true, false, false, false},  // down
			{false, false, false, true, false, false},  // jump
			{true, false, false, true, false, false},   // left jump
			{false, true, false, true, false, false},   // right jump
			{false, false, true, true, false, false},   // down jump (not necessary?)
			{false, false, false, false, true, false},  // speed
			{true, false, false, false, true, false},   // left speed
			{false, true, false, false, true, false},   // right speed
			{false, false, true, false, true, false},   // down speed (also does nothing)
			{false, false, false, true, true, false},   // jump speed
			{true, false, false, true, true, false},    // left jump speed
			{false, true, false, true, true, false},    // right jump speed
			{false, false, true, true, true, false}};   // down jump speed (?)

    // important instance variables
    private float[][] qValues;
    private int[][] stateActionFreqs;
    private StateRepresentation stateRep;
    
    // ----------- You'll probably want a few variables about here.... --------------- //



    // ------------------------------------------------------------------------------- //

    // reward observed for current state
    private float instantaneousReward;
    // housekeeping
    private float lastIntermediateReward;
    
    // here for an example random agent
    // private Random rng;
    
    public SarsaAgent()
    {
	super("SarsaAgent");
	// rng = new Random(42);
	stateRep = new AdamsCrappyStateRepresentation(this);
	qValues = new float[stateRep.numStates()][NUM_ACTIONS];
	stateActionFreqs = new int[stateRep.numStates()][NUM_ACTIONS];
	reset();

    }

    public void reset()
    {
	// This gets called when we die and start a new episode.
	// Note that we don't want to reset the Q-table or frequency table on
	// a reset, since we want to keep learning where we left off.
	action = new boolean[Environment.numberOfKeys];
	instantaneousReward = 0;
	// You'll need to reset some variables that you've declared though ...
	// --------------------------------------------------------------------- //



	// --------------------------------------------------------------------- //

    }


    // number of times we want to visit a state/action pair before we think we know
    // what it's really worth
    private static final int EXPLOITATION_THRESHOLD = 30;

    // how much we think an under-visited state/action pair is worth.
    private static final float OPTIMISM = 1000.0f;
    
    /**
       Sets the exploration policy by determining the utility of a state
       based on the number of times it's been visited (qCount). qValue is the current
       Q-value (utility) for the state/action pair being considered.
     */
    private float explorationUtility(float qValue, int qCount){
	if (qCount < EXPLOITATION_THRESHOLD){
	    return OPTIMISM;
	} else {
	    return qValue;
	}
    }
    
    /**
       The amount by which to mutiply updates to Q-state values, as a function of how long the
       agent has been operating.
     */
    private float learningRate(int frequency){
	return 50f/(50+frequency);
    }

    public boolean[] getAction()
    {
	// this Agent requires observation integrated in advance.
	// System.out.println(marioStatus);
	// System.out.println(instantaneousReward);

	// CURRENT STATE
	int s = stateRep.stateIndex(mergedObservation);
	boolean[] action = new boolean[Environment.numberOfKeys];

	// EXAMPLE: choose a random action
	// (requires import and initialization of Random above)
	// int act = rng.nextInt(NUM_ACTIONS);

	// EXAMPLE: always walk right
	 int act = 2;

	// TODO: choose next action based on current q-values and exploration fxn











	// TODO: if we're aware of a previous state...

	    // TODO: update frequency


            // TODO: update q-value for previous state
	    // Note that we choose our state based on the exploration function,
	    // but we use its actual current Q-value (NOT its exploration
	    // utility) to update the value of the previous state. The policy
	    // determines the action we choose (i.e. this is an on-policy algorithm),
	    // but not our learned utility of the action-state pair.
	    






	
	System.arraycopy(actionMap[act], 0, action, 0, Environment.numberOfKeys);

	// TODO: update variables for the next turn



	
	//System.out.println("s: "+ s);
	//System.out.println(Arrays.deepToString(mergedObservation));
	return action;
    }

    /**
       This method gets called on every frame, *before* getAction is called by the controller.
       That means that the current value of instantaneousReward is the observed reward for being in the
       present state (i.e. exactly what you need). You shouldn't have to mess with this; just
       grab your reward from the instantaneousReward variable.
     */
    public void giveIntermediateReward(float intermediateReward)
    {
	instantaneousReward = intermediateReward - lastIntermediateReward - 1.0f;
	lastIntermediateReward = intermediateReward;
    }
    
}
