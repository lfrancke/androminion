package com.vdom.players;

import java.util.ArrayList;
import java.util.Collections;

import com.vdom.api.Card;
import com.vdom.api.GameType;
import com.vdom.core.Cards;
import com.vdom.core.Game;

public class VDomPlayerMary extends VDomPlayerSarah {

  @Override
  public boolean isAi() {
    return true;
  }

  @Override
  public String getPlayerName() {
    return getPlayerName(Game.maskPlayerNames);
  }

  @Override
  public String getPlayerName(boolean maskName) {
    return maskName ? "Player " + (playerNumber + 1) : "Mary";
  }

  @Override
  public void setupGameVariables(GameType gameType, Card[] cardsInPlay) {
    super.setupGameVariables(gameType, cardsInPlay);

    onlyBuyEarlySingle = false;
    earlyCardBuys =
      new Card[] {Cards.militia, Cards.seaHag, Cards.familiar, Cards.youngWitch, Cards.torturer, Cards.thief,
        Cards.minion, Cards.saboteur, Cards.pirateShip, Cards.ghostShip, Cards.rabble, Cards.goons, Cards.followers,
        Cards.fortuneTeller, Cards.jester};
    earlyCardBuyMax = 3;

    ArrayList<Card> cards = new ArrayList<>();
    //        for(Card c : valuedCards) {
    //            cards.add(c);
    //        }
    Collections.addAll(cards, earlyCardBuys);
    valuedCards = cards.toArray(new Card[0]);

    favorSilverGoldPlat = false;
  }
}
