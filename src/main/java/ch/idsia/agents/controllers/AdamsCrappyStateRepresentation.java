package ch.idsia.agents.controllers;
    
import ch.idsia.agents.controllers.StateRepresentation;
import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;

import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;


public class AdamsCrappyStateRepresentation implements StateRepresentation{

    BasicMarioAIAgent owner;
    
    // spiky/near, spiky/mid, spiky/far, spiky/left,
    // goomba/near, goomba/mid, goomba/far, goomba/left, none
    private static final int NUM_ENEMY_STATES = 9; 
    private static final int NUM_ENEMIES_TRACKED = 2; // power
    private static final int NUM_GROUND_GAP_DISTANCES = 4; // near, mid, far, none
    private static final int NUM_JUMP_GAP_DISTANCES = 2; // under, not
    // near/one, near/more, mid/one, mid/more, far/one, far/more, none
    private static final int NUM_LEDGE_STATES = 7; 
    private static final int NUM_PRINCESS_STATES = 2; // visible, not
    private static final int NUM_TERMINAL_STATES = 2; // you win or you die

    private static final int STATE_GRID_WIDTH = 16;
    private static final int STATE_GRID_HEIGHT = 16;
    private static final int NUM_STATES = (NUM_PRINCESS_STATES *
					   ((int)Math.pow(NUM_ENEMY_STATES, NUM_ENEMIES_TRACKED) *
					    NUM_GROUND_GAP_DISTANCES *
					    NUM_LEDGE_STATES +
					    NUM_JUMP_GAP_DISTANCES) +
					   NUM_TERMINAL_STATES) ;


    // declare these at class level to avoid constantly re-allocating.
    private List<int[]> terrain;
    private Queue<int[]> enemies;
    private List<int[]> goodStuff;
    private List<int[]> stuffToLandOn;

    /** Constructor */
    public AdamsCrappyStateRepresentation(BasicMarioAIAgent owner){
	this.owner = owner; // need this for access to some state variables.

	terrain = new ArrayList<int[]>(); // coordinates only
	enemies = new PriorityQueue<int[]>(16, new Comparator<int[]>(){
		final BasicMarioAIAgent owner = AdamsCrappyStateRepresentation.this.owner;
		// order enemies by proximity to Mario
		public int compare(int[] lhs, int[] rhs){
		    if (lhs == null || rhs == null) {
			throw new NullPointerException();
		    } else if (Math.abs(lhs[1] - owner.marioEgoCol) <
			       Math.abs(rhs[1] - owner.marioEgoCol)){
			return -1;
		    } else if (Math.abs(lhs[1] - owner.marioEgoCol) ==
			       Math.abs(rhs[1] - owner.marioEgoCol)){
			return 0;
		    } else {
			return 1;
		    }
		}
	    }); // coordinates and type
	goodStuff = new ArrayList<int[]>(); // coordinates and type
	stuffToLandOn = new ArrayList<int[]>(); // coordinates only
    }

    /**
       Maps a 16x16 grid of observed cells, centered on Mario, of enemies and 
       blocks into an enumerated state. 
     */
    public int stateIndex(byte[][] mergedObservation){
	int state = 0;
	int lastTerrainRow = 0;
	for (int i = owner.marioEgoRow - (STATE_GRID_HEIGHT/2);
	     i < owner.marioEgoRow + (STATE_GRID_HEIGHT/2);
	     i++){
	    for (int j = owner.marioEgoCol - (STATE_GRID_WIDTH/2);
		 j < owner.marioEgoCol + (STATE_GRID_WIDTH/2);
		 j++) {
		// Use default "zoom level" (level 1 for both enemies and blocks),
		// which lumps enemies and blocks into groups without generalizing
		// everything as "pass-through-able" or not.
		byte b = mergedObservation[i][j];
		switch(b){
		case (0): // Empty cell, no sprites or bricks
		case (GeneralizerLevelScene.LADDER):
		case (Sprite.KIND_FIREBALL):
		    break; // don't care
		case (Sprite.KIND_FIRE_FLOWER):
		case (Sprite.KIND_MUSHROOM): // coins return same value as mushrooms
		    goodStuff.add(new int[]{i, j, 0});
		    break;
		case (Sprite.KIND_GOOMBA):
		    enemies.add(new int[]{i, j, 0});
		    break;
		case (Sprite.KIND_SPIKY):
		    enemies.add(new int[]{i, j, 1});
		    break;
		case (GeneralizerLevelScene.BORDER_CANNOT_PASS_THROUGH):
		    if (j >= owner.marioEgoCol){
			// can we see a wall to jump over?
			if (i <= owner.marioEgoRow &&
			    mergedObservation [i][j-1] != b) {
			    terrain.add(new int[]{i, j});
			} else if (i > owner.marioEgoRow) { // for calculating gaps
			    terrain.add(new int[]{i, j});
			    // also add to stuffToLandOn?
			}
		    }
		    if (i > lastTerrainRow) {
			lastTerrainRow = i;
		    }
		    break;
		case (GeneralizerLevelScene.BORDER_HILL):
		    stuffToLandOn.add(new int[]{i, j});
		    break;
		case (GeneralizerLevelScene.FLOWER_POT_OR_CANNON):
		    terrain.add(new int[]{i, j});
		    break;
		case (Sprite.KIND_PRINCESS):
		    //System.out.println("Princess!!");
		    goodStuff.add(new int[]{i, j, 1}); // distinguish from powerups
		    break;
		case (GeneralizerLevelScene.BRICK):
		    goodStuff.add(new int[]{i, j, 2});
		    break;
		default:
		    break;
		}
	    }
	}

	// ---------   Now analyze the lists we just built ------------ //

	// find nearest terrain to jump on/over if it exists
	// (top-left corner of a ledge)
	// find gaps while we're at it

	int lastBlock = owner.marioEgoCol -1 ;
	int gapStart = -1;
	int gapEnd = -1;
	// all things in terrain should be >= marioEgoCol
	int[] nearestLedge =
	    new int[]{owner.marioEgoRow + STATE_GRID_HEIGHT, owner.marioEgoCol + STATE_GRID_WIDTH};

	for (int[] block : terrain){
	    if (block[1] < nearestLedge[1] && block[0] <= owner.marioEgoRow) {
		nearestLedge[0] = block[0];
		nearestLedge[1] = block[1];
	    }
	    // find any gaps if they exist
	    if (block[0] == lastTerrainRow){
		if (block[1] - lastBlock > 1){
		    gapStart = lastBlock + 1;
		    gapEnd = block[1];
		}
		lastBlock = block[1];
	    }
	}
	if (lastBlock < owner.marioEgoCol + (STATE_GRID_WIDTH / 2) - 1){
	    gapStart = lastBlock + 1;
	    gapEnd = owner.marioEgoCol + (STATE_GRID_WIDTH / 2); //assume the end is just over the horizon
	}
	int[] enemy1 = enemies.poll();
	int[] enemy2 = enemies.poll();

	int princessVisible = 0;

	for (int[] thing : goodStuff){
	    if (thing[2] == 1) {
		
		princessVisible = 1;
	    }
	}

	// ----------------- Now actually calculate the state number ----------- //

	if (owner.marioStatus == Mario.STATUS_RUNNING) {
	    state += princessVisible * (NUM_ENEMY_STATES *
					NUM_ENEMY_STATES *
					NUM_GROUND_GAP_DISTANCES *
					NUM_LEDGE_STATES +
					NUM_JUMP_GAP_DISTANCES);
	    
	    // mutually exclusive jumping/non-jumping representations to save states
	    // (but we always care if the princess is on-screen)
	    if (owner.isMarioOnGround){
		int enemyState = 0;
		int diff = 0;
		if (enemy1 != null) { // otherwise leave enemyState zero
		    // enemy[2] == 0 is a stompable, == 1 is unstompable
		    diff = enemy1[1] - owner.marioEgoCol;
		    if (diff < 0) {
			enemyState = enemy1[2] == 0 ? 1 : 5; // left
		    } else if (diff < 2) {
			enemyState = enemy1[2] == 0 ? 2 : 6; // near
		    } else if (diff < 4) {
			enemyState = enemy1[2] == 0 ? 3 : 7; // mid
		    } else {
			enemyState = enemy1[2] == 0 ? 4 : 8; // far
		    }
		}

		state += enemyState * (NUM_ENEMY_STATES *
				       NUM_GROUND_GAP_DISTANCES *
				       NUM_LEDGE_STATES);

		enemyState = 0;
		if (enemy2 != null) { // otherwise leave it zero
		    diff = enemy2[1] - owner.marioEgoCol;
		    if (diff < 0) {
			enemyState = enemy2[2] == 0 ? 1 : 5; // left
		    } else if (diff < 2) {
			enemyState = enemy2[2] == 0 ? 2 : 6; // near
		    } else if (diff < 4) {
			enemyState = enemy2[2] == 0 ? 3 : 7; // mid
		    } else {
			enemyState = enemy2[2] == 0 ? 4 : 8; // far
		    }
		}
				       
		state += enemyState * (NUM_GROUND_GAP_DISTANCES *
				       NUM_LEDGE_STATES);

		int gapState = 0;
		// System.out.println("gapStart: " + gapStart);
		diff = gapStart - owner.marioEgoCol;
		if (gapStart >= 0 ){ // otherwise mark it zero
		    if (diff < 2) { gapState = 1; } // near ...
		    else if (diff < 4) { gapState = 2; }
		    else { gapState = 3; } // ... far!
		}
				       
		state += gapState * (NUM_LEDGE_STATES);

		int ledgeState = 0;
		diff = nearestLedge[1] - owner.marioEgoCol;
		if (nearestLedge[1] <= owner.marioEgoCol + (STATE_GRID_WIDTH / 2)) {
		    // row (inverse height) must be <= marioEgoRow if we're aware of it
		    // if it's equal, that's a one-square hop. Strictly less-than, then,
		    // is two or more squares up.
		    if (diff < 2) {
			ledgeState = nearestLedge[0] == owner.marioEgoRow ? 1 : 2; //near+1 : near+>1
		    } else if (diff < 4) {
			ledgeState = nearestLedge[0] == owner.marioEgoRow ? 3 : 4; //mid+1 : mid+>1
		    } else {
			ledgeState = nearestLedge[0] == owner.marioEgoRow ? 5 : 6; //far+1 : far+>1
		    }
		}

		state += ledgeState;
	    
	    } else {
		state += (NUM_ENEMY_STATES *
			  NUM_ENEMY_STATES *
			  NUM_GROUND_GAP_DISTANCES *
			  NUM_LEDGE_STATES);

		int jumpState;
		// System.out.println("gap: "+ gapStart + ", " + gapEnd + ", ego: " + marioEgoCol);
		if (gapStart <= owner.marioEgoCol && gapEnd > owner.marioEgoCol){
		    jumpState = 0;
		} else {
		    jumpState = 1;
		}

		state += jumpState;
	    }
	} else { 
	    state += NUM_PRINCESS_STATES * (NUM_ENEMY_STATES *
					    NUM_ENEMY_STATES *
					    NUM_GROUND_GAP_DISTANCES *
					    NUM_LEDGE_STATES +
					    NUM_JUMP_GAP_DISTANCES);
	    
	    int terminalState = owner.marioStatus == Mario.STATUS_DEAD ? 0 : 1;
	    state += terminalState;

	}
	// System.out.println("last terrain row: "+ lastTerrainRow);
	// System.out.println("e1: " + enemy1 == null ? "null" : Arrays.toString(enemy1));
	// System.out.println("e2: " + enemy2 == null ? "null" : Arrays.toString(enemy2));
	// System.out.println(Arrays.toString(nearestLedge));
	terrain.clear();
	enemies.clear();
	goodStuff.clear();
	stuffToLandOn.clear();
	return state;
    }

    public boolean isTerminalState(int state){
    	return state >= (NUM_STATES - NUM_TERMINAL_STATES - 1);
    }

    public int numStates(){
	return NUM_STATES;
    }

}
