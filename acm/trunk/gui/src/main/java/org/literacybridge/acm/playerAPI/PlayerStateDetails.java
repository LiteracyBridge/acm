package org.literacybridge.acm.playerAPI;

import org.literacybridge.acm.playerAPI.SimpleSoundPlayer.PlayerState;

public class PlayerStateDetails {

	private	PlayerState currentPlayerState = null;
	private double currentPoitionInSecs = 0.0;
	
	public PlayerStateDetails(PlayerState currentPlayerState, double currentPoitionInSecs) {
		this.currentPlayerState = currentPlayerState;
		this.currentPoitionInSecs = currentPoitionInSecs;
	}

	public PlayerState getCurrentPlayerState() {
		return currentPlayerState;
	}

	public double getCurrentPoitionInSecs() {
		return currentPoitionInSecs;
	}

	@Override
	public String toString() {
		return "PlayerStateDetails [currentPlayerState=" + currentPlayerState
				+ ", currentPoitionInSecs=" + currentPoitionInSecs + "]";
	}
}
