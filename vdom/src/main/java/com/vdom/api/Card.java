package com.vdom.api;

import java.io.Serializable;

import com.vdom.core.CardImpl;
import com.vdom.core.Cards.Kind;
import com.vdom.core.Expansion;
import com.vdom.core.Game;
import com.vdom.core.MoveContext;
import com.vdom.core.PileCreator;
import com.vdom.core.Player;
import com.vdom.core.Type;

public interface Card extends Serializable {

  Kind getKind();

  String getName();

  String getSafeName();

  Expansion getExpansion();

  boolean is(Type t, Player player);

  boolean is(Type t);

  int getNumberOfTypes(Player player);

  String getStats();

  String getDescription();

  int getCost(MoveContext context);

  int getCost(MoveContext context, boolean buyPhase);

  boolean costPotion();

  int getDebtCost(MoveContext context);

  int getVictoryPoints();

  boolean isOverpay(Player player);

  int getAddCards();

  int getAddActions();

  int getAddGold();

  boolean providePotion();

  int getAddBuys();

  int getAddVictoryTokens();

  int getAddCardsNextTurn();

  int getAddActionsNextTurn();

  int getAddGoldNextTurn();

  int getAddBuysNextTurn();

  boolean takeAnotherTurn();

  int takeAnotherTurnCardCount();

  /**
   * Does this card force you to trash a card when played? (Used for AI)
   *
   * @return Whether this card forces you to trash a card when played
   */
  boolean trashForced();

  boolean isCallableWhenCardGained();

  int getCallableWhenGainedMaxCost();

  boolean isCallableWhenActionResolved();

  boolean doesActionStillNeedToBeInPlayToCall();

  boolean isCallableWhenTurnStarts();

  void callWhenCardGained(MoveContext context, Card cardToGain);

  void callWhenActionResolved(MoveContext context, Card resolvedAction);

  void callAtStartOfTurn(MoveContext context);

  void play(Game game, MoveContext context);

  void play(Game game, MoveContext context, boolean fromHand);

  void play(Game game, MoveContext context, boolean fromHand, boolean treasurePlay);

  Integer getId();

  void isBuying(MoveContext context);

  void isBought(MoveContext context);

  void isTrashed(MoveContext context);

  boolean isImpersonatingAnotherCard();

  Card behaveAsCard();

  CardImpl getControlCard();

  boolean isTemplateCard();

  CardImpl getTemplateCard();

  boolean isPlaceholderCard();

  void setPlaceholderCard();

  CardImpl instantiate();

  PileCreator getPileCreator();

  //public void isGained(MoveContext context);
}
