package com.vdom.players;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
import com.vdom.core.BasePlayer;
import com.vdom.core.Cards;
import com.vdom.core.Game;
import com.vdom.core.GetCardsInGameOptions;
import com.vdom.core.MoveContext;
import com.vdom.core.Player;
import com.vdom.core.Type;
import com.vdom.core.Util;

public class VDomPlayerEarl extends BasePlayer {

  static Card[] HAND = new Card[0];
  static int THRONE_ROOM_PLAYS = 0;
  static int THRONE_ROOM_DUDS = 0;
  static HashSet<Card> DEFENDABLE_ATTACK_CARDS = new HashSet<>();
  private final int silverTurnCount = 0;
  private final ArrayList<HistoryItem> historyItems = new ArrayList<>();
  Random rand = new Random(System.currentTimeMillis());
  // int turnCount = 0;
  private int treasureMapsBought = 0;

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
    return maskName ? "Player " + (playerNumber + 1) : "Earl";
  }

  @Override
  public void gameEvent(GameEvent event) {
    super.gameEvent(event);
    if (Game.debug) {
      if ((event.getPlayer() != this) ||
          (event.getType() == GameEvent.EventType.GameStarting)) {
        return;
      }
      if (event.getType() == GameEvent.EventType.TurnBegin) {
        // this.turnCount += 1;
        HAND = getHand().toArray();
        for (Card card : HAND) {
          historyItems.add(new HistoryItem(turnCount, card, 0, HistoryItem.Action.IN_HAND));
        }
      } else if ((event.getType() == GameEvent.EventType.BuyingCard) || (event.getType()
                                                                         == GameEvent.EventType.CardObtained)) {
        Card card = event.getCard();
        historyItems.add(new HistoryItem(turnCount, card, 0, HistoryItem.Action.BOUGHT));

        if (card.is(Type.Victory)) {
          for (Card thisCard : HAND) {
            historyItems.add(
              new HistoryItem(turnCount, thisCard, card.getVictoryPoints(), HistoryItem.Action.VICTORY_HELPER));
          }
        } else if (card.is(Type.Curse, event.context.getPlayer())) {
          for (Card thisCard : HAND) {
            historyItems.add(
              new HistoryItem(turnCount, thisCard, card.getVictoryPoints(), HistoryItem.Action.VICTORY_HELPER));
          }
        }
      } else if (event.getType() == GameEvent.EventType.PlayingCard) {
        historyItems.add(new HistoryItem(turnCount, event.getCard(), 0, HistoryItem.Action.PLAYED));
      } else if (event.getType() == GameEvent.EventType.GameOver) {
        Player player = event.getContext().getPlayer();
        if (!(player.getWin())) {
          calculateStats(historyItems);
        }
      }
    }
  }

  @Override
  public void newGame(MoveContext context) {
    super.newGame(context);
  }

  @Override
  public Card doAction(MoveContext context) {
    Card card =
      fromHand(calculateAction(turnCount, getHand().toArray(), context.countThroneRoomsInEffect(), context));
    debug("myAction: " + Arrays.toString(getHand().toArray()) + " -> " + card);

    return card;
  }

  @Override
  public Card doBuy(MoveContext context) {
    Card card = calculateBuy(context, context.getCoinAvailableForBuy());

    if (willWin(card)) {
      return card;
    }
    if (wouldLose(card)) {
      if (card.equals(Cards.province)) {
        card = Cards.estate;
      } else if (context.getBuysLeft() == 0) {
        card = null;
      }
    }

    if (!context.canBuy(card)) {
      card = null;
    }

    return card;
  }

  @Override
  public Card feast_cardToObtain(MoveContext context) {
    return null;
  }

  @Override
  public Card remodel_cardToTrash(MoveContext context) {
    for (Card card : getHand()) {
      if (card.getCost(context) == 6) {
        return card;
      }
    }
    for (Card card : getHand()) {
      if (card.equals(Cards.curse)) {
        return card;
      }
    }
    for (Card card : getHand()) {
      if (card.equals(Cards.swindler)) {
        return card;
      }
    }
    for (Card card : getHand()) {
      if (card.equals(Cards.copper) || card.equals(Cards.rats)) {
        return card;
      }
    }
    return null;
  }

  @Override
  public Card[] militiaAttackCardsToKeep(MoveContext context) {
    ArrayList<Card> cards = new ArrayList<>();
    for (Card card : getHand()) {
      if (!(card.is(Type.Victory, context.getPlayer()))
          && !(card.is(Type.Curse, context.getPlayer()))
          && !card.is(Type.Shelter, context.getPlayer())
          && !(card.is(Type.Ruins, context.getPlayer()))) {
        cards.add(card);
      }
    }

    while (cards.size() > 3) {
      cards.remove(0);
    }

    if (cards.size() < 3) {
      cards.clear();
      Card[] hand = getHand().toArray();
      cards.addAll(Arrays.asList(hand).subList(0, 3));
    }

    return cards.toArray(new Card[0]);
  }

  @Override
  public Card mine_treasureFromHandToUpgrade(MoveContext context) {
    Card[] hand = getHand().toArray();
    int silvers = 0;
    int golds = 0;

    for (Card card : hand) {
      if (card.equals(Cards.copper)) {
        return card;
      }
      if (card.equals(Cards.silver)) {
        ++silvers;
      }
      if (card.equals(Cards.gold)) {
        ++golds;
      }
    }

    if (silvers > 0) {
      return Cards.silver;
    }
    if (golds > 0 && context.cardInGame(Cards.platinum)) {
      return Cards.gold;
    }

    return null;
  }

  @Override
  public boolean moneylender_shouldTrashCopper(MoveContext context) {
    return (getCurrencyTotal(context) >= 4);
  }

  @Override
  public Card[] chapel_cardsToTrash(MoveContext context) {
    ArrayList<Card> cards = new ArrayList<>();

    for (Card card : getHand()) {
      if (card.equals(Cards.estate) || card.equals(Cards.curse)) {
        cards.add(card);
      }
    }

    if (getCurrencyTotal(context) >= 3) {
      for (Card card : getHand()) {
        if (card.equals(Cards.copper)) {
          cards.add(card);
        }
      }
    }

    while (cards.size() > 4) {
      cards.remove(cards.size() - 1);
    }

    return cards.toArray(new Card[0]);
  }

  @Override
  public Card[] cellar_cardsToDiscard(MoveContext context) {
    ArrayList<Card> cards = new ArrayList<>();

    Card[] hand = getHand().toArray();
    for (Card card : hand) {
      if (card.is(Type.Victory, context.getPlayer()) || card.is(Type.Curse, context.getPlayer())) {
        cards.add(card);
      }
    }

    if ((cards.isEmpty()) && (inHand(Cards.throneRoom))) {
      cards.add(Cards.throneRoom);
    }

    return cards.toArray(new Card[0]);
  }

  @Override
  public Card[] secretChamber_cardsToDiscard(MoveContext context) {
    ArrayList<Card> cards = new ArrayList<>();
    for (Card card : getHand()) {
      if (card.is(Type.Victory, context.getPlayer()) || card.is(Type.Curse, context.getPlayer())) {
        cards.add(card);
      }
    }
    return cards.toArray(new Card[0]);
  }

  @Override
  public Card swindler_cardToSwitch(MoveContext context, int cost, int debt, boolean potion) {
    if (cost == 0 && !potion) {
      return Cards.curse;
    }

    if (cost == 2 && !potion) {
      return Cards.estate;
    }

    if (cost == 3 && !potion) {
      return Cards.silver;
    }
    if (cost == 5 && !potion) {
      return Cards.duchy;
    }

    Card[] cards = context.getCardsInGame(GetCardsInGameOptions.TopOfPiles, true);
    ArrayList<Card> randList = new ArrayList<>();
    for (Card card : cards) {
      if (Cards.isSupplyCard(card) && (card.getCost(context) == cost) && (card.getDebtCost(context) == debt) && (context
                                                                                                                   .getCardsLeftInPile(
                                                                                                                     card)
                                                                                                                 > 0)
          && card.costPotion() == potion) {
        randList.add(card);
      }
    }

    if (!randList.isEmpty()) {
      return randList.get(rand.nextInt(randList.size()));
    }

    return null;
  }

  @Override
  public Card courtyard_cardToPutBackOnDeck(MoveContext context) {
    if (inHandCount(Cards.treasureMap) == 1) {
      return Cards.treasureMap;
    }

    return super.courtyard_cardToPutBackOnDeck(context);
  }

  @Override
  public Card masquerade_cardToPass(MoveContext context) {
    if (getHand().isEmpty()) {
      return null;
    }

    Card c = getHand().get(0);
    for (Card card : getHand()) {
      if (card.getCost(context) < c.getCost(context)) {
        c = card;
      }
    }
    return c;
  }

  @Override
  public Player.NoblesOption nobles_chooseOptions(MoveContext context) {
    if (context.getActionsLeft() > 1) {
      return Player.NoblesOption.AddCards;
    }
    if (inHandCount(Cards.nobles) > 2) {
      return Player.NoblesOption.AddCards;
    }
    if (inHandCount(Cards.nobles) == 2) {
      return Player.NoblesOption.AddActions;
    }
    if (nonNobleActionCardCount(context.player) > 0) {
      return Player.NoblesOption.AddActions;
    }
    return Player.NoblesOption.AddCards;
  }

  @Override
  public Card[] ghostShip_attack_cardsToPutBackOnDeck(MoveContext context) {
    ArrayList<Card> cards = new ArrayList<>();
    ArrayList<Card> h = Util.copy(getHand());

    if (inHandCount(Cards.treasureMap) == 1) {
      for (Card card : getHand()) {
        if (card.equals(Cards.treasureMap)) {
          if (h.remove(card)) {
            cards.add(card);
          }
        }
      }
    }

    for (Card card : getHand()) {
      if (card.is(Type.Victory, context.getPlayer()) || card.is(Type.Curse, context.getPlayer())) {
        if (h.remove(card)) {
          cards.add(card);
        }
      }
      if (cards.size() == 2) {
        break;
      }
    }

    if (cards.size() < 2) {
      for (Card card : getHand()) {
        if (card.equals(Cards.copper)) {
          if (h.remove(card)) {
            cards.add(card);
          }
        }
        if (cards.size() == 2) {
          break;
        }
      }
    }

    if (cards.size() < 2) {
      Card lowCard = null;
      int lowCost = 100;
      for (Card card : getHand()) {
        if (card.getCost(context) < lowCost) {
          lowCost = card.getCost(context);
          lowCard = card;
        }
      }

      if (lowCard != null) {
        if (h.remove(lowCard)) {
          cards.add(lowCard);
        }
      }
    }

    return cards.toArray(new Card[0]);
  }

  @Override
  public Card embargo_supplyToEmbargo(MoveContext context) {
    return null;
  }

  public Card workshop_cardToGet(MoveContext context) {
    if (shouldBuyGardens(context)) {
      return Cards.gardens;
    }
    return Cards.silver;
  }

  public Card feast_cardToGet(MoveContext context) {
    Card[] cards = {Cards.market, Cards.baker, Cards.laboratory, Cards.mine};

    return pickBalancedAvailable(context, cards);
  }

  public Card remodel_cardToGet(MoveContext context, int maxCost) {
    return calculateBuy(context, maxCost);
  }

  public boolean chancellor_shouldDiscardDeck(MoveContext context, Card responsible) {
    return false;
  }

  private HashMap<Integer, Integer> getStat(ArrayList<HistoryItem> historyItems, HistoryItem.Action action) {
    HashMap<Integer, Integer> stat = new HashMap<>();
    for (HistoryItem historyItem : historyItems) {
      if (historyItem.getAction() != action) {
        continue;
      }
      Integer count = null;
      Card card = historyItem.getCard();
      Integer id = card.getId();
      if (stat.containsKey(id)) {
        count = stat.get(id);
      } else {
        count = 0;
      }
      if (card.is(Type.Victory)) {
        stat.put(id, count + card.getVictoryPoints());
      } else if (card.is(Type.Curse, null)) {
        stat.put(id, count + card.getVictoryPoints());
      } else {
        stat.put(id, count += 1);
      }
    }

    return stat;
  }

  private HashMap<Integer, Integer> getVictoryStat(ArrayList<HistoryItem> historyItems) {
    HashMap<Integer, Integer> stat = new HashMap<>();
    for (HistoryItem historyItem : historyItems) {
      Integer count = null;
      Card card = historyItem.getCard();
      Integer id = card.getId();
      if (stat.containsKey(id)) {
        count = stat.get(id);
      } else {
        count = 0;
      }
      if (card.is(Type.Victory)) {
        stat.put(id, count + card.getVictoryPoints());
      } else if (card.is(Type.Curse, null)) {
        stat.put(id, count + card.getVictoryPoints());
      } else {
        stat.put(id, count += 1);
      }
    }

    return stat;
  }

  private void calculateStats(ArrayList<HistoryItem> historyItems) {
    HashMap<String, Integer> allMyCards = new HashMap<>();
    int total = 0;
    for (Card card : getAllCards()) {
      String name = card.getName();
      int count;
      if (allMyCards.containsKey(name)) {
        count = allMyCards.get(name);
      } else {
        count = 0;
      }

      allMyCards.put(name, ++count);
      ++total;
    }
    debug(allMyCards.toString());
    debug("total cards: " + total);
    debug("total turns: " + turnCount);
    debug(getStat(historyItems, HistoryItem.Action.BOUGHT).toString());
    HashMap<Integer, Integer> played = getStat(historyItems, HistoryItem.Action.PLAYED);
    HashMap<Integer, Integer> inHand = getStat(historyItems, HistoryItem.Action.IN_HAND);
    HashMap<Integer, Integer> victoryHelpers = getVictoryStat(historyItems);

    for (HistoryItem historyItem : historyItems) {
      if ((historyItem.getAction() == HistoryItem.Action.BOUGHT) && (historyItem.getCard().is(Type.Action, this))) {
        debug(
          historyItem + ", was in hand " + inHand.get(historyItem.getCard().getId()) + " times, played " +
          played.get(historyItem.getCard().getId()) + " times, and saw " + victoryHelpers
                                                                             .get(historyItem.getCard().getId())
          + " vps");
      }
    }
    debug(getStat(historyItems, HistoryItem.Action.PLAYED).toString());
  }

  private Card calculateAction(int myTurnCount, Card[] hand, int throneRoomsInEffect, MoveContext context) {
    if (inHandCount(Cards.treasureMap) >= 2) {
      return Cards.treasureMap;
    }

    // play prince if action card candidate available
    if (inHand(Cards.prince)) {
      ArrayList<Card> cardList = new ArrayList<>();
      Collections.addAll(cardList, hand);
      if (prince_cardCandidates(context, cardList, false).length != 0) {
        return Cards.prince;
      }
    }

    if ((inHand(Cards.nobles)) &&
        (context.getActionsLeft() > 1)) {
      return Cards.nobles;
    }

    boolean hasThroneRoom = inHand(Cards.throneRoom);

    int actionCards = 0;
    for (Card card : hand) {
      if (card.is(Type.Action, context.player)
          && !card.equals(Cards.rats)
          && !(card.equals(Cards.tactician) && context.countCardsInPlay(Cards.tactician) > 0)
        ) {
        ++actionCards;
      }
    }

    if ((actionCards == 0) || (actionCards == inHandCount(Cards.throneRoom))) {
      return null;
    }

    if (throneRoomsInEffect > 0) {
      if (inHand(Cards.throneRoom)) {
        return Cards.throneRoom;
      }

      if (inHand(Cards.feast)) {
        return Cards.feast;
      }

      if ((inHand(Cards.mine)) && (mineableCards(hand) > 1)) {
        return Cards.mine;
      }

      if ((inHand(Cards.moneyLender)) && (inHandCount(Cards.copper) > 1)) {
        return Cards.moneyLender;
      }

      if (inHand(Cards.bureaucrat)) {
        return Cards.bureaucrat;
      }

      if (inHand(Cards.workshop)) {
        return Cards.workshop;
      }

      if (inHand(Cards.militia)) {
        return Cards.militia;
      }
    }

    ArrayList<Card> dontPlay = new ArrayList<>();
    if (hasThroneRoom) {
      if (inHand(Cards.feast)) {
        THRONE_ROOM_PLAYS += 1;
        return Cards.throneRoom;
      }

      if ((inHand(Cards.mine)) && (mineableCards(hand) > 1)) {
        THRONE_ROOM_PLAYS += 1;
        return Cards.throneRoom;
      }

      if ((inHand(Cards.moneyLender)) && (inHandCount(Cards.copper) > 1)) {
        THRONE_ROOM_PLAYS += 1;
        return Cards.throneRoom;
      }

      if (inHand(Cards.bureaucrat)) {
        THRONE_ROOM_PLAYS += 1;
        return Cards.throneRoom;
      }

      Card bestCard = getBestAddingAction(hasThroneRoom, context.player);
      if (bestCard != null) {
        return Cards.throneRoom;
      }

      if (inHand(Cards.workshop)) {
        return Cards.throneRoom;
      }

      dontPlay.add(Cards.throneRoom);
      THRONE_ROOM_DUDS += 1;
    }

    Card bestCard = getBestAddingAction(hasThroneRoom, context.player);
    if (bestCard != null) {
      return bestCard;
    }

    if ((inHand(Cards.mine)) && (mineableCards(hand) > 0)) {
      return Cards.mine;
    }
    dontPlay.add(Cards.mine);

    if ((inHand(Cards.moneyLender)) && (inHandCount(Cards.copper) >= 1)) {
      return Cards.moneyLender;
    }
    dontPlay.add(Cards.moneyLender);

    Card[] attackCards = {Cards.seaHag, Cards.cutpurse, Cards.militia};
    for (Card card : attackCards) {
      if (inHand(card)) {
        return card;
      }
    }

    Card[] cards = {Cards.militia, Cards.bureaucrat, Cards.library};
    for (Card card : cards) {
      if (inHand(card)) {
        return card;
      }
    }

    for (Card card : getHand().toArray()) {
      if (card.equals(Cards.treasureMap)) {
        continue;
      }
      if (card.is(Type.Action, context.player)) {
        if (dontPlay.contains(card)) {
          continue;
        }

        if (context.canPlay(card)) {
          return card;
        }
      }
    }
    return null;
  }

  private Card getBestAddActionAction(Card[] hand, Player player) {
    Card bestAction = null;
    int bestAddActions = 0;

    for (Card card : hand) {
      if (card.is(Type.Action, player)) {
        if (card.getAddActions() > 0) {
          int addCards = card.getAddCards();

          if (addCards > bestAddActions) {
            bestAction = card;
            bestAddActions = addCards;
          } else if ((addCards == 0) && (bestAddActions == 0)) {
            bestAction = card;
          }
        }
      }
    }

    return bestAction;
  }

  private Card getBestAddActionCard(Card[] hand, Player player) {
    Card bestAction = null;
    int bestAddCards = 0;

    for (Card card : hand) {
      if (card.is(Type.Action, player)) {
        int thisAddCards = card.getAddCards();
        if (thisAddCards > bestAddCards) {
          bestAction = card;
          bestAddCards = thisAddCards;
        }
      }
    }

    return bestAction;
  }

  private Card getBestAddingAction(boolean hasThroneRoom, Player player) {
    Card[] hand = getHand().toArray();

    Card bestAction = getBestAddActionAction(hand, player);

    if (bestAction == null) {
      bestAction = getBestAddActionCard(hand, player);
    }

    if (bestAction != null) {
      return ((hasThroneRoom) ? Cards.throneRoom : bestAction);
    }

    return bestAction;
  }

  private Card handleEightGold(MoveContext context, Card cardToBuy) {
    if (cardToBuy != null) {
      return cardToBuy;
    }

    if (context.canBuy(Cards.colony)) {
      return Cards.colony;
    }

    if (context.canBuy(Cards.platinum) && turnCount < 15 && game.pileSize(Cards.province) > 4) {
      return Cards.platinum;
    }

    if (context.canBuy(Cards.province) && (!game.buyWouldEndGame(Cards.province)
                                           || calculateLead(Cards.province) >= 0)) {
      return Cards.province;
    }

    return null;
  }

  private Card handleSevenGold(MoveContext context, Card cardToBuy) {
    return handleSixGold(context, cardToBuy);
  }

  private Card handleSixGold(MoveContext context, Card cardToBuy) {
    if (cardToBuy != null) {
      return cardToBuy;
    }

    if ((turnCount > 15 || game.pileSize(Cards.province) < 4) && (context.canBuy(Cards.duchy))) {
      return Cards.duchy;
    }

    if ((context.canBuy(Cards.adventurer)) && (getMyCardCount(Cards.gold) >= 3) && (getMyCardCount(Cards.adventurer)
                                                                                    < 2)) {
      return Cards.adventurer;
    }

    if (context.canBuy(Cards.nobles) && getMyCardCount(Cards.gold) >= 3) {
      return Cards.nobles;
    }

    if (context.canBuy(Cards.gold)) {
      return Cards.gold;
    }

    return null;
  }

  private Card handleFiveGold(MoveContext context, Card cardToBuy) {
    if (cardToBuy != null) {
      return cardToBuy;
    }

    if ((turnCount > 15) && (context.canBuy(Cards.duchy))) {
      return Cards.duchy;
    }

    Card[] cards = {Cards.market, Cards.baker, Cards.mine/*, Cards.saboteur */};
    Card thisCard = pickBalancedBuyable(context, cards, 3);
    if (thisCard != null) {
      return thisCard;
    }

    if ((turnCount < 5) && (context.canBuy(Cards.mine)) && (getMyCardCount(Cards.mine) < 2)) {
      return Cards.mine;
    }

    Card[] cards2 = {Cards.festival, Cards.market, Cards.baker};
    thisCard = pickBalancedBuyable(context, cards2, 4);
    if (thisCard != null) {
      return thisCard;
    }

    Card[] cards3 = {Cards.festival, Cards.market, Cards.baker, Cards.laboratory, Cards.witch, Cards.library};
    thisCard = pickBalancedBuyable(context, cards3);
    if (thisCard != null) {
      return thisCard;
    }

    return null;
  }

  private ArrayList<Card> getBuyableCards(MoveContext context, Card[] cards) {
    ArrayList<Card> buyable = new ArrayList<>();

    for (Card card : cards) {
      if (context.canBuy(card)) {
        buyable.add(card);
      }
    }

    return buyable;
  }

  private Card pickBalancedBuyable(MoveContext context, Card[] cards, Integer max) {
    return pickBalancedActual(context, getBuyableCards(context, cards), max);
  }

  private Card pickBalancedBuyable(MoveContext context, Card[] cards) {
    return pickBalancedBuyable(context, cards, null);
  }

  private Card pickBalancedActual(MoveContext context, List<Card> available, Integer max) {
    Integer low = null;

    for (Card card : available) {
      int thisCount = getMyCardCount(card);
      if (low == null) {
        low = thisCount;
      }

      if (thisCount < low) {
        low = thisCount;
      }
    }

    if ((max != null) && (low == max)) {
      return null;
    }

    for (Card thisCard : available) {
      if (getMyCardCount(thisCard) <= low) {
        return thisCard;
      }
    }
    return null;
  }

  private Card handleFourGold(MoveContext context, Card cardToBuy) {
    if (cardToBuy != null) {
      return cardToBuy;
    }

    if ((turnCount < 5) && (!(isChapelGame(context))) && (context.canBuy(Cards.moneyLender)) && (getMyCardCount(
      Cards.moneyLender) < 1)) {
      return Cards.moneyLender;
    }

    if (shouldBuyGardens(context)) {
      return Cards.gardens;
    }

    if ((isChapelGame(context)) && (context.canBuy(Cards.bureaucrat)) && (getMyCardCount(Cards.bureaucrat) < 3)) {
      return Cards.bureaucrat;
    }

    if (turnCount > silverTurnCount) {
      return Cards.silver;
    }

    Card[] cards =
      {Cards.militia, Cards.bureaucrat, Cards.throneRoom, Cards.seaHag, Cards.cutpurse, Cards.miningVillage};
    Card thisCard = pickBalancedBuyable(context, cards, 2);
    if (thisCard != null) {
      return thisCard;
    }

    return null;
  }

  private Card handleThreeGold(MoveContext context, Card cardToBuy) {
    if (cardToBuy != null) {
      return cardToBuy;
    }

    if (shouldBuyChapel(context)) {
      return Cards.chapel;
    }

    if (turnCount > silverTurnCount) {
      return Cards.silver;
    }
    ArrayList<Card> cards = new ArrayList<>();
    cards.add(Cards.village);
    cards.add(Cards.fishingVillage);
    if ((shouldBuyGardens(context)) && (getMyCardCount(Cards.workshop) < 4)) {
      cards.add(Cards.workshop);
    }
    if ((context.canBuy(Cards.swindler)) && (getMyCardCount(Cards.swindler) < 2)) {
      cards.add(Cards.swindler);
    }
    Card thisCard = pickBalancedBuyable(context, cards.toArray(new Card[0]), 5);

    if (thisCard != null) {
      return thisCard;
    }

    return Cards.silver;
  }

  private int attackingCardsInPlay(MoveContext context) {
    int attackingCardsInPlay = 0;
    for (Card card : context.getCardsInGame(GetCardsInGameOptions.Templates, false)) {
      if (Cards.isSupplyCard(card) && DEFENDABLE_ATTACK_CARDS.contains(card)) {
        ++attackingCardsInPlay;
      }
    }

    return attackingCardsInPlay;
  }

  private Card handleTwoGold(MoveContext context, Card cardToBuy) {
    if (cardToBuy != null) {
      return cardToBuy;
    }

    if ((attackingCardsInPlay(context) > 0) && (cardInPlay(context, Cards.moat))) {
      return Cards.moat;
    }

    if ((context.canBuy(Cards.courtyard)) && (inHandCount(Cards.courtyard) < 2)) {
      return Cards.courtyard;
    }

    if (turnCount > 20) {
      return Cards.estate;
    }
    if ((context.canBuy(Cards.cellar)) && (getMyCardCount(Cards.cellar) < 2)) {
      return Cards.cellar;
    }
    if (!(isChapelGame(context))) {
      return Cards.copper;
    }

    return null;
  }

  private Card handleOneGold(MoveContext context, Card cardToBuy) {
    if (cardToBuy != null) {
      return cardToBuy;
    }

    return null;
  }

  private Card handleZeroGold(MoveContext context, Card cardToBuy) {
    if (cardToBuy != null) {
      return cardToBuy;
    }

    if (shouldBuyGardens(context)) {
      return Cards.copper;
    }

    return null;
  }

  private boolean willWin(Card card) {

    return false;
  }

  private boolean wouldLose(Card card) {

    return false;
  }

  private boolean cardInPlay(MoveContext context, Card card) {
    boolean cardInPlay = false;
    for (Card thisCard : context.getCardsInGame(GetCardsInGameOptions.Templates, true)) {
      if (thisCard.equals(card) && Cards.isSupplyCard(thisCard)) {
        cardInPlay = true;
        break;
      }
    }
    return cardInPlay;
  }

  private boolean isChapelGame(MoveContext context) {
    return ((!(shouldBuyGardens(context))) && (cardInPlay(context, Cards.chapel)) && (cardInPlay(context,
      Cards.bureaucrat)));
  }

  private boolean shouldBuyChapel(MoveContext context) {
    int goldAvailable = getCoinEstimate(context);
    return ((isChapelGame(context)) && (turnCount < 3) && (((goldAvailable == 2) || (goldAvailable == 3)))
            && (getMyCardCount(Cards.chapel) < 1));
  }

  private boolean shouldBuyGardens(MoveContext context) {
    return cardInPlay(context, Cards.gardens);
  }

  private Card calculateBuy(MoveContext context, int goldAvailable) {
    Card[] cards = context.getCardsInGame(GetCardsInGameOptions.Buyables);

    if (shouldBuyChapel(context)) {
      return Cards.chapel;
    }

    if ((((goldAvailable == 4) || (goldAvailable == 5))) && (cardInPlay(context, Cards.treasureMap)) && (getMyCardCount(
      Cards.treasureMap) < 2) &&
        (treasureMapsBought < 2)) {
      treasureMapsBought += 1;
      return Cards.treasureMap;
    }
    Card cardToBuy = null;

    cardToBuy = handleEightGold(context, cardToBuy);

    switch (goldAvailable) {
      case 7:
        cardToBuy = handleSevenGold(context, cardToBuy);
      case 6:
        cardToBuy = handleSixGold(context, cardToBuy);
      case 5:
        cardToBuy = handleFiveGold(context, cardToBuy);
      case 4:
        cardToBuy = handleFourGold(context, cardToBuy);
      case 3:
        cardToBuy = handleThreeGold(context, cardToBuy);
      case 2:
        cardToBuy = handleTwoGold(context, cardToBuy);
      case 1:
        cardToBuy = handleOneGold(context, cardToBuy);
      case 0:
        cardToBuy = handleZeroGold(context, cardToBuy);
    }

    if (cardToBuy != null) {
      return cardToBuy;
    }

    if (goldAvailable <= 2) {
      if (turnCount < 15) {
        return Cards.copper;
      }
      return null;
    }

    int tries = 40;

    while (tries-- > 0) {
      Card card = cards[rand.nextInt(cards.length)];
      if (context.canBuy(card)) {
        return card;
      }
    }

    return null;
  }

  private Card pickBalancedAvailable(MoveContext context, Card[] cards) {
    return pickBalancedAvailable(context, Arrays.asList(cards), null);
  }

  private Card pickBalancedAvailable(MoveContext context, List<Card> cards, Integer max) {
    ArrayList<Card> available = new ArrayList<>();

    HashMap<String, Integer> cardCounts = context.getCardCounts();
    for (Card card : cards) {
      if (cardCounts.containsKey(card.getName())) {
        int count = cardCounts.get(card.getName());
        if (count > 0) {
          available.add(card);
        }
      }
    }

    return pickBalancedActual(context, available, max);
  }

  private int nonNobleActionCardCount(Player player) {
    int count = 0;
    for (Card card : getHand()) {
      if (card.equals(Cards.nobles)) {
        continue;
      }
      if (card.is(Type.Action, player)) {
        ++count;
      }
    }

    return count;
  }

  static class HistoryItem {

    private final int turn;
    private final Card card;
    private final int victoryPoints;
    private final Action action;

    public HistoryItem(int turn, Card card, int victoryPoints, Action action) {
      this.turn = turn;
      this.card = card;
      this.victoryPoints = victoryPoints;
      this.action = action;
    }

    public int getTurn() {
      return turn;
    }

    public Card getCard() {
      return card;
    }

    public int getVictoryPoints() {
      return victoryPoints;
    }

    public Action getAction() {
      return action;
    }

    public String toString() {
      return turn + " - " + card + " - " + action;
    }

    enum Action {
      BOUGHT, PLAYED, IN_HAND, VICTORY_HELPER
    }
  }

  static {
    Card[] attackCards =
      {Cards.cutpurse, Cards.ghostShip, Cards.militia, Cards.pirateShip/*, Cards.saboteur */, Cards.seaHag, Cards.thief,
        Cards.torturer,
        Cards.witch, Cards.mountebank, Cards.rabble, Cards.goons, Cards.youngWitch, Cards.margrave, Cards.cultist,
        Cards.marauder, Cards.pillage, Cards.soothsayer};
    Collections.addAll(DEFENDABLE_ATTACK_CARDS, attackCards);
  }

}
