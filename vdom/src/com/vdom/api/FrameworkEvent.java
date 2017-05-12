package com.vdom.api;

import java.util.HashMap;

public class FrameworkEvent {

  private Type type;
  private GameType gameType;
  private HashMap<String, Double> gameTypeWins;

  public FrameworkEvent(Type type) {
    this.type = type;
  }

  public HashMap<String, Double> getGameTypeWins() {
    return gameTypeWins;
  }

  public void setGameTypeWins(HashMap<String, Double> gameTypeWins) {
    this.gameTypeWins = gameTypeWins;
  }

  public Type getType() {
    return type;
  }

  public GameType getGameType() {
    return gameType;
  }

  public void setGameType(GameType gameType) {
    this.gameType = gameType;
  }

  public enum Type {
    GameTypeStarting, // A new GameType is starting
    GameTypeOver, // GameType completed

    AllDone
  }
}
