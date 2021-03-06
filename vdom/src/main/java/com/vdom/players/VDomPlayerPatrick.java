package com.vdom.players;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import com.vdom.api.Card;
import com.vdom.api.CardCostComparator;
import com.vdom.api.GameEvent;
import com.vdom.core.BasePlayer;
import com.vdom.core.CardPile;
import com.vdom.core.Cards;
import com.vdom.core.Game;
import com.vdom.core.MoveContext;
import com.vdom.core.Player;
import com.vdom.core.Type;
import com.vdom.core.Util;

/**
 * @author buralien
 */
public class VDomPlayerPatrick extends BasePlayer {

  private static final ArrayList<Card> specialCards = new ArrayList<>();
  private static final ArrayList<Card> specialTreasureCards = new ArrayList<>();
  private static final ArrayList<Card> specialVictoryCards = new ArrayList<>();
  private static final ArrayList<Card> specialActionCards = new ArrayList<>();
  private static final ArrayList<Card> knownCards = new ArrayList<>();
  private static final ArrayList<Card> knownActionCards = new ArrayList<>();
  private static final ArrayList<Card> knownSingleActionCards = new ArrayList<>();
  // just one in deck is enough (trashing, etc.)
  private static final ArrayList<Card> knownDoubleActionCards = new ArrayList<>();
  // two in deck is ok (mostly attacks and other good terminals)
  private static final ArrayList<Card> knownMultiActionCards = new ArrayList<>();
  // cantrips of which we can have any number without terminal collision
  private static final ArrayList<Card> knownComboActionCards = new ArrayList<>();
  // cantrips and other cards which don't work on their own but need other cards
  private static final ArrayList<Card> knownDefenseCards = new ArrayList<>();
  // can be bought as reaction to aggressive opponent, normally no
  private static final ArrayList<Card> knownCursingCards = new ArrayList<>();
  // cards that add curses to opponent's deck
  private static final ArrayList<Card> knownTrashingCards = new ArrayList<>();
  // cards that allow trashing of Curse by playing them from hand
  private static final ArrayList<Card> knownTier3Cards = new ArrayList<>();
  // cards that can be played without any additional implementation, but are not so good
  private static final ArrayList<Card> knownPrizeCards = new ArrayList<>(); // prize cards that we know how to play
  private static final ArrayList<Card> knownGood52Cards = new ArrayList<>(); // cards that play well with 5/2 start
  private final boolean debug = Game.debug;
  Random rand = new Random(System.currentTimeMillis());
  private OpponentList opponents = new OpponentList();
  private boolean redefineStrategy = false;
  private StrategyOption strategy = StrategyOption.Nothing;
  private Card strategyCard = null;
  private ArrayList<Card> strategyPossibleCards = new ArrayList<>();
  private Card strategyMultiCardTerminal = null;

  @Override
  public boolean isAi() {
    return true;
  }

  /**
   * This function calculates the total amount of coin available to a player in his deck
   * The resulting amount is somehow approximated for cards like Venture, Bank, Fool's Gold
   *
   * @param context context
   */
  @Override
  public int getCurrencyTotal(MoveContext context) {
    return guessCurrencyTotal(getAllCards());
  }

  @Override
  public String getPlayerName() {
    return getPlayerName(Game.maskPlayerNames);
  }

  @Override
  public String getPlayerName(boolean maskName) {
    return maskName ? "Player " + (playerNumber + 1) : "Patrick";
  }

  public boolean shouldTrash(Card card) {
    ArrayList<Card> c = new ArrayList<>();
    c.add(card);
    return (getCardToTrash(c, DiscardOption.NonDestructive) != null);
  }

  @Override
  public void gameEvent(GameEvent event) {
    super.gameEvent(event);

    if (event.player.equals(this)) {
      return; // we keep track of our own events, thank you very much
    }

    if (opponents.isEmpty()) {
      opponents.put(event.player.playerNumber, new Opponent(event.player.playerNumber));
    } else if (!opponents.containsKey(event.player.playerNumber)) {
      opponents.put(event.player.playerNumber, new Opponent(event.player.playerNumber));
    }

    if (opponents.get(event.player.playerNumber).getVP() == -1000) {
      if (game.sheltersInPlay) {
        opponents.get(event.player.playerNumber).setVP(0);
      } else {
        opponents.get(event.player.playerNumber).setVP(3);
      }
    }

    if ((event.getType() == GameEvent.EventType.BuyingCard) || (event.getType() == GameEvent.EventType.CardObtained)) {
      Card card = event.getCard();

      if (card.is(Type.Action, event.player)) {
        opponents.get(event.player.playerNumber).putActionCard(card);
        if (card.is(Type.Attack, event.player)) {
          opponents.get(event.player.playerNumber).setAttacking(true);
        }
      }

      if (card.is(Type.Victory, event.player)) {
        opponents.get(event.player.playerNumber).addVP(card.getVictoryPoints());
      }

      if (card.equals(Cards.curse)) {
        opponents.get(event.player.playerNumber).addVP(card.getVictoryPoints());
      }

    }

    if (event.getType() == GameEvent.EventType.CardTrashed) {
      Card card = event.getCard();

      if (card.is(Type.Victory, event.player)) {
        opponents.get(event.player.playerNumber).addVP(0 - card.getVictoryPoints());
      }

      if (card.equals(Cards.curse)) {
        opponents.get(event.player.playerNumber).addVP(0 - card.getVictoryPoints());
      }
    }
  }
  //private ComboCards combo;

  @Override
  public boolean shouldDiscard(Card card, MoveContext context) {
    ArrayList<Card> c = new ArrayList<>();
    c.add(card);
    return (getCardToDiscard(c, DiscardOption.NonDestructive, context) != null);
  }

  /**
   * @param orig_set original list of cards
   * @param new_set  new list of cards
   */
  //	public double compareCards(ArrayList<Card> orig_set, ArrayList<Card> new_set) {
  //		int vps = this.getVPTotalValue(new_set) - this.getVPTotalValue(orig_set);
  //		double mpc = this.getMoneyPerCard(new_set) - this.getMoneyPerCard(orig_set);
  //		int coins = this.getCurrencyTotal(new_set) - this.getCurrencyTotal(orig_set);
  //		this.log("vps: " + vps + "; coins: " + coins + "; mpc: " + mpc );
  //
  //		return (double) (((vps*0.5) * Math.abs(vps*0.5)) + ((coins*0.8) * Math.abs(vps*0.8)));
  //	}
  @Override
  public boolean shouldBuyPotion() {
    boolean ret = false;

    for (Card c : strategyPossibleCards) {
      ret |= c.costPotion();
    }
    return ret;
  }

  //	public double compareDeckOptions(ArrayList<Card> deck, ArrayList<Card> option1, ArrayList<Card> option2, int hoardsPlayed) {
  //		ArrayList<Card> deck1 = new ArrayList<Card>();
  //		ArrayList<Card> deck2 = new ArrayList<Card>();
  //
  //		for (Card c : deck) {
  //			deck1.add(c);
  //			deck2.add(c);
  //		}
  //
  //		for (Card c : option1) {
  //			deck1.add(c);
  //			if (c.equals(Cards.cache)) {
  //				deck1.add(Cards.copper);
  //				deck1.add(Cards.copper);
  //			}
  //			if (c.equals(Cards.farmland)) {
  //				deck1.remove(Cards.curse);
  //				deck1.add(Cards.estate);
  //			}
  //			if (c instanceof VictoryCard) {
  //				for (int i = 0; i < hoardsPlayed; i++) {
  //					deck1.add(Cards.gold);
  //				}
  //			}
  //			for (int e = 0; e < game.getEmbargos(c.toString()); e++) {
  //				deck1.add(Cards.curse);
  //			}
  //		}
  //		for (Card c : option2) {
  //			deck2.add(c);
  //			if (c.equals(Cards.cache)) {
  //				deck2.add(Cards.copper);
  //				deck2.add(Cards.copper);
  //			}
  //			if (c.equals(Cards.farmland)) {
  //				deck2.remove(Cards.curse);
  //				deck2.add(Cards.estate);
  //			}
  //			if (c instanceof VictoryCard) {
  //				for (int i = 0; i < hoardsPlayed; i++) {
  //					deck2.add(Cards.gold);
  //				}
  //			}
  //			for (int e = 0; e < game.getEmbargos(c.toString()); e++) {
  //				deck2.add(Cards.curse);
  //			}
  //
  //		}
  //
  //		return compareCards(deck1, deck2);
  //	}
  @Override
  public void newGame(MoveContext context) {
    // When multiple games are played in one session, the same Player object
    // is used, so reset any fields in this method.
    super.newGame(context);

    redefineStrategy = false;
    strategy = StrategyOption.Nothing;
    strategyCard = null;
    strategyMultiCardTerminal = null;
    strategyPossibleCards = new ArrayList<>();

    //opponentActionCards = new ArrayList<Card>();
    //opponentVP = -1000;
    //opponentIsAttacking = false;
    opponents = new OpponentList();
  }

  @Override
  public Card doAction(MoveContext context) {
    return advisorPlayAction(hand.toArrayListClone(), context.player);
  }

  @Override
  public Card doBuy(MoveContext context) {
    int gold = context.getCoinAvailableForBuy();

    return advisorGeneral(context, gold, false, false);
  }

  @Override
  public Card workshop_cardToObtain(MoveContext context) {
    log("workshop_cardToObtain");
    return advisorGeneral(context, 4, false, true);
  }

  @Override
  public Card[] militiaAttackCardsToKeep(MoveContext context) {
    ArrayList<Card> cards2keep = hand.toArrayListClone();
    Card card2discard = null;

    while (cards2keep.size() > 3) {
      card2discard = getCardToDiscard(cards2keep, DiscardOption.Destructive, context);
      cards2keep.remove(card2discard);
    }

    return cards2keep.toArray(new Card[3]);
  }

  @Override
  public Card[] chapel_cardsToTrash(MoveContext context) {
    ArrayList<Card> ret = new ArrayList<>();
    ArrayList<Card> temphand = hand.toArrayListClone();

    for (int i = 0; i < 4; i++) {
      Card card = getCardToTrash(temphand, DiscardOption.NonDestructive);
      if (card != null) {
        ret.add(card);
        temphand.remove(card);
      } else {
        break;
      }
    }

    if (!ret.isEmpty()) {
      return ret.toArray(new Card[ret.size()]);
    }

    return null;
  }

  @Override
  public TorturerOption torturer_attack_chooseOption(MoveContext context) {
    if (game.pileSize(Cards.curse) <= 0) {
      return Player.TorturerOption.TakeCurse;
    }

    if (inHand(Cards.watchTower) || inHand(Cards.trader)) {
      return Player.TorturerOption.TakeCurse;
    }

    ArrayList<Card> temphand = hand.toArrayListClone();
    int discarded = Math.min(2, temphand.size());
    ArrayList<Card> toDiscard = getCardsToDiscard(temphand, discarded, DiscardOption.SemiDestructive, context);
    if (toDiscard.size() >= discarded) {
      return Player.TorturerOption.DiscardTwoCards;
    }

    for (Card c : toDiscard) {
      temphand.remove(c);
      discarded--;
    }

    log("torturer_attack_chooseOption: keep");
    Card keep = advisorGeneral(context, getCurrencyTotal(temphand), false, false);

    //		this.log("torturer_attack_chooseOption: discard");
    //		Card discard = advisorGeneral(context, this.getCurrencyTotal(temphand), false, false);
    //
    //		if (keep != null && discard !=null && keep.equals(discard)) {
    //			return Player.TorturerOption.DiscardTwoCards;
    //		}

    ArrayList<Card> preffered = new ArrayList<>();
    if (game.isPlatInGame()) {
      preffered.add(Cards.colony);
      preffered.add(Cards.platinum);
    } else {
      preffered.add(Cards.province);
      preffered.add(Cards.gold);
    }

    if (preffered.contains(keep)) {
      return Player.TorturerOption.TakeCurse;
    }

    return Player.TorturerOption.DiscardTwoCards;
  }

  @Override
  public StewardOption steward_chooseOption(MoveContext context) {
    ArrayList<Card> temphand = hand.toArrayListClone();

    Card[] ret = {null, null};

    ret[0] = getCardToTrash(temphand, DiscardOption.SemiDestructive);
    if (ret[0] != null) {
      temphand.remove(ret[0]);

      ret[1] = getCardToTrash(temphand, DiscardOption.SemiDestructive);

      if (ret[1] != null) {
        return StewardOption.TrashCards;
      }
    }

    if ((getMyAddActions() > 1) || (getMoneyPerCard(deck.toArrayList()) > 1)) {
      return StewardOption.AddCards;
    }

    return StewardOption.AddGold;
  }

  @Override
  public Card[] steward_cardsToTrash(MoveContext context) {
    ArrayList<Card> temphand = hand.toArrayListClone();

    Card[] ret = new Card[2];

    ret[0] = getCardToTrash(temphand, getAllCards(), DiscardOption.Destructive, false);
    temphand.remove(ret[0]);

    ret[1] = getCardToTrash(temphand, getAllCards(), DiscardOption.Destructive, false);

    return ret;
  }

  @Override
  public Card[] torturer_attack_cardsToDiscard(MoveContext context) {
    //TODO test
    return getCardsToDiscard(hand.toArrayListClone(), 2, DiscardOption.Destructive, context)
             .toArray(new Card[2]);
  }

  @Override
  public Card courtyard_cardToPutBackOnDeck(MoveContext context) {
    HashMap<Card, Card> options =
      new HashMap<>(); // this list will contain card to discard as key and best card to obtain with the rest as value
    ArrayList<Card> list = hand.toArrayListClone();
    Card ret = hand.get(0);

    // let's see what we can buy with each of the cards in hand removed
    for (Card c : list) {
      ArrayList<Card> temp = new ArrayList<>(list);
      temp.remove(c);
      Card a =
        advisorGeneral(context, getCurrencyTotal(temp) + context.getCoinAvailableForBuy(), false, false);
      if (a != null) {
        options.put(a, c);
      }
    }

    log("courtyard_cardToPutBackOnDeck: " + options);

    // if we can buy a good card with one of the cards removed, we will do that
    if (options.containsKey(Cards.colony)) {
      log("courtyard_cardToPutBackOnDeck: can buy colony without " + options.get(Cards.colony));
      return hand.get(options.get(Cards.colony));
      //return options.get(Cards.colony);
    }
    if (options.containsKey(Cards.platinum)) {
      log("courtyard_cardToPutBackOnDeck: can buy platinum without " + options.get(Cards.platinum));
      return hand.get(options.get(Cards.platinum));
    }
    if (options.containsKey(Cards.province)) {
      log("courtyard_cardToPutBackOnDeck: can buy province without " + options.get(Cards.province));
      return hand.get(options.get(Cards.province));
    }
    if (options.containsKey(Cards.gold)) {
      log("courtyard_cardToPutBackOnDeck: can buy gold without " + options.get(Cards.gold));
      return hand.get(options.get(Cards.gold));
    }

    if (hand.contains(Cards.gold)) {
      log("courtyard_cardToPutBackOnDeck: gold");
      return hand.get(Cards.gold);
    }
    if (hand.contains(Cards.silver)) {
      log("courtyard_cardToPutBackOnDeck: silver");
      return hand.get(Cards.silver);
    }
    if (hand.contains(Cards.copper)) {
      log("courtyard_cardToPutBackOnDeck: copper");
      return hand.get(Cards.copper);
    }

    return ret;
  }

  @Override
  public Card ironworks_cardToObtain(MoveContext context) {
    return advisorGeneral(context, 4, false, true);
  }

  @Override
  public Card masquerade_cardToPass(MoveContext context) {
    return getCardToTrash(DiscardOption.Destructive);
  }

  @Override
  public Card masquerade_cardToTrash(MoveContext context) {
    return getCardToTrash(DiscardOption.NonDestructive);
  }

  @Override
  public boolean miningVillage_shouldTrashMiningVillage(MoveContext context, Card responsible) {
    log("miningVillage_shouldTrashMiningVillage: keep");
    Card normal = advisorGeneral(context, getCurrencyTotal(hand.toArrayListClone()), false, true);

    log("miningVillage_shouldTrashMiningVillage: trash");
    Card extra = advisorGeneral(context, getCurrencyTotal(hand.toArrayListClone()) + 2, false, true);

    //TODO should compare resulting decks
    if (normal.equals(extra)) {
      // card to be obtained with +2 coin is the same
      return false;
    }
    return true;
  }

  @Override
  public Card saboteur_cardToObtain(MoveContext context, int maxCost, int maxDebt, boolean potion) {
    log("saboteur_cardToObtain");
    return advisorGeneral(context, maxCost, false, true);
  }

  @Override
  public MinionOption minion_chooseOption(MoveContext context) {
    int inhand = Util.getCardCount(hand, Cards.minion);
    if (strategy == StrategyOption.Minion) {
      int coins = getCurrencyTotal(hand.toArrayListClone()) + context.getCoinAvailableForBuy();
      int limit = 8;
      if (game.pileSize(Cards.minion) > 0) {
        limit = 5;
      }
      log("minion_chooseOption: inhand=" + inhand + "; coins: " + coins);

      if (inhand > 0) {
        return Player.MinionOption.AddGold;
      }

      if (inhand * 2 + coins + 2 >= limit) {
        return Player.MinionOption.AddGold;
      } else {
        return Player.MinionOption.RolloverCards;
      }
    }

    if (inhand > 0) {
      return Player.MinionOption.AddGold;
    }

    if (context.getCoinAvailableForBuy() >= 5) {
      return Player.MinionOption.AddGold;
    }

    if (context.getPlayer().getHand().size() < 3) {
      return Player.MinionOption.RolloverCards;
    }
    return Player.MinionOption.AddGold;
  }

  @Override
  public Card[] ghostShip_attack_cardsToPutBackOnDeck(MoveContext context) {
    //TODO rewrite
    ArrayList<Card> cards = new ArrayList<>();
    for (int i = 0; i < context.getPlayer().getHand().size() - 3; i++) {
      cards.add(context.getPlayer().getHand().get(i));
    }

    return cards.toArray(new Card[cards.size()]);
  }

  @Override
  public Card island_cardToSetAside(MoveContext context) {
    Card ret = null;
    ArrayList<Card> temphand = hand.toArrayListClone();

    while (!temphand.isEmpty()) {
      ret = getCardToDiscard(temphand, DiscardOption.Destructive, context);
      temphand.remove(ret);
      if (isOnlyVictory(ret, context.getPlayer())) {
        return ret;
      }
    }

    temphand = hand.toArrayListClone();
    return getCardToDiscard(temphand, DiscardOption.Destructive, context);
  }

  //	private Card getCardToDiscard(DiscardOption destructive) {
  //		return getCardToDiscard(this.hand.toArrayListClone(), destructive);
  //	}

  @Override
  public Card lookout_cardToTrash(MoveContext context, Card[] cards) {
    ArrayList<Card> temp = new ArrayList<>();

    Collections.addAll(temp, cards);

    log("lookout_cardToTrash: " + temp);
    return getCardToTrash(temp, DiscardOption.Destructive);
  }

  @Override
  public Card lookout_cardToDiscard(MoveContext context, Card[] cards) {
    ArrayList<Card> temp = new ArrayList<>();

    Collections.addAll(temp, cards);
    log("lookout_cardToDiscard: " + temp);

    return getCardToDiscard(temp, DiscardOption.Destructive, context);
  }

  @Override
  public Card ambassador_revealedCard(MoveContext context) {
    return getCardToTrash(hand.toArrayListClone(), new ArrayList<Card>(), DiscardOption.Destructive, true);
  }

  @Override
  public int ambassador_returnToSupplyFromHand(MoveContext context, Card card) {
    ArrayList<Card> temphand = hand.toArrayListClone();

    log("ambassador_returnToSupplyFromHand: current hand");
    Card zero = advisorGeneral(context, getCurrencyTotal(temphand), false, false);

    temphand.remove(getCardToTrash(temphand, DiscardOption.Destructive));
    log("ambassador_returnToSupplyFromHand: -1 card");
    Card one = advisorGeneral(context, getCurrencyTotal(temphand), false, false);

    temphand.remove(getCardToTrash(temphand, DiscardOption.Destructive));
    log("ambassador_returnToSupplyFromHand: -2 cards");
    Card two = advisorGeneral(context, getCurrencyTotal(temphand), false, false);

    if (zero != null && two != null && zero.equals(two)) {
      return 2;
    } else if (zero != null && one != null && zero.equals(one)) {
      return 1;
    } else if (one != null && two != null && one.equals(two)) {
      return 2;
    } else {
      return 0;
    }
  }

  @Override
  public boolean scryingPool_shouldDiscard(MoveContext context, Player targetPlayer, Card card) {
    ArrayList<Card> c = new ArrayList<>();
    c.add(card);
    boolean discard =
      (getCardToDiscard(c, DiscardOption.SemiDestructive, new MoveContext(context.game, targetPlayer)) != null);

    if (targetPlayer == this) {
      log("scryingPool_shouldDiscard: " + (discard ? "discard " : "keep ") + "my " + card);
      return discard;
    } else {
      log("scryingPool_shouldDiscard: " + (!discard ? "discard " : "keep ") + "opponents " + card);
      return !discard;
    }
  }

  @Override
  public Card bishop_cardToTrashForVictoryTokens(MoveContext context) {
    if (inHand(Cards.curse)) {
      return Cards.curse;
    }
    if (inHand(Cards.estate)) {
      return Cards.estate;
    }
    return getCardToTrash(DiscardOption.Destructive);
  }

  @Override
  public Card bishop_cardToTrash(MoveContext context) {
    return getCardToTrash(DiscardOption.NonDestructive);
  }

  @Override
  public Card[] goonsAttackCardsToKeep(MoveContext context) {
    return militiaAttackCardsToKeep(context);
  }

  @Override
  public boolean loan_shouldTrashTreasure(MoveContext context, Card treasure) {
    if ((treasure.equals(Cards.copper)) || (treasure.equals(Cards.illGottenGains))) {
      if ((getMoneyPerCard(getAllCards(), -1, -1) > 0.7) && (getCurrencyTotal(context) > 6)) {
        return true;
      }
    }

    if (treasure.equals(Cards.loan)) {
      return true;
    }

    if ((getMoneyPerCard(getAllCards(), (0 - treasure.getAddGold()), -1) > (Math.pow(treasure.getAddGold(), 2)
                                                                            * 0.7)) && (getCurrencyTotal(
      context) > 6)) {
      return true;
    }

    return false;
  }

  @Override
  public Card tradeRoute_cardToTrash(MoveContext context) {
    return getCardToTrash(DiscardOption.Destructive);
  }

  @Override
  public Card[] vault_cardsToDiscardForGold(MoveContext context) {
    //TODO test
    ArrayList<Card> temphand = hand.toArrayListClone();
    ArrayList<Card> ret = new ArrayList<>();
    Card card = null;

    while (ret.size() < 2) {
      card = getCardToDiscard(temphand, DiscardOption.SemiDestructive, context);
      if (card == null) {
        break;
      }
      temphand.remove(card);
      ret.add(card);
    }

    while (ret.size() < 2) {
      if (getMyAddActions() == 0) {
        for (Card acard : temphand) {
          if (acard.is(Type.Action, context.player)) {
            ret.add(acard);
          }
        }
      }
    }

    while (ret.size() < 2) {
      for (Card vCard : temphand) {
        if (vCard.is(Type.Victory, context.player)) {
          ret.add(vCard);
        }
      }
    }

    while (ret.size() < 2) {
      for (Card tCard : temphand) {
        if (!(tCard.is(Type.Treasure, this))) {
          ret.add(tCard);
        }
      }
    }

    while (ret.size() < 2) {
      for (Card tCard : temphand) {
        ret.add(tCard);
      }
    }
    log("vault: chosen " + ret);

    if (!ret.isEmpty()) {
      return ret.toArray(new Card[ret.size()]);
    }
    return null;
  }

  @Override
  public Card[] vault_cardsToDiscardForCard(MoveContext context) {
    return getCardsToDiscard(hand.toArrayListClone(), 2, DiscardOption.SemiDestructive, context)
             .toArray(new Card[2]);
  }

  @Override
  public WatchTowerOption watchTower_chooseOption(MoveContext context, Card card) {
    if (shouldTrash(card)) {
      return WatchTowerOption.Trash;
    }

    if (isOnlyVictory(card, context.getPlayer())) {
      return WatchTowerOption.Normal;
    }

    return WatchTowerOption.TopOfDeck;
  }

  @Override
  public TournamentOption tournament_chooseOption(MoveContext context) {
    for (Card c : knownPrizeCards) {
      if (c.is(Type.Prize, null) && context.getPileSize(c) > 0) {
        log("tournament_chooseOption: prize");
        return TournamentOption.GainPrize;
      }
    }
    log("tournament_chooseOption: duchy");
    return TournamentOption.GainDuchy;
  }

  @Override
  public Card tournament_choosePrize(MoveContext context) {
    for (Card c : knownPrizeCards) {
      if (c.is(Type.Prize, null) && context.getPileSize(c) > 0) {
        log("tournament_choosePrize: " + c);
        return c;
      }
    }
    log("tournament_choosePrize: nothing");
    return null;
  }

  @Override
  public Card[] followers_attack_cardsToKeep(MoveContext context) {
    return militiaAttackCardsToKeep(context);
  }

  @Override
  public Card borderVillage_cardToObtain(MoveContext context, int maxCost) {
    log("borderVillage_cardToObtain");
    return advisorGeneral(context, maxCost, false, true);
  }

  @Override
  public Card farmland_cardToTrash(MoveContext context) {
    return getCardToTrash(DiscardOption.Destructive);
  }

  @Override
  public Card farmland_cardToObtain(MoveContext context, int exactCost, int debt, boolean potion) {
    //TODO test
    log("farmland_cardToObtain");
    return advisorGeneral(context, exactCost, true, true);
  }

  @Override
  public boolean duchess_shouldDiscardCardFromTopOfDeck(MoveContext context, Card card) {
    return shouldDiscard(card, context);
  }

  @Override
  public boolean duchess_shouldGainBecauseOfDuchy(MoveContext context) {
    return strategyPossibleCards.contains(Cards.duchess);
  }

  @Override
  public boolean foolsGold_shouldTrash(MoveContext context) {
    log("foolsGold_shouldTrash");
    ArrayList<Card> temphand = hand.toArrayListClone();
    Card keep = advisorGeneral(context, getCurrencyTotal(temphand), false, true);
    temphand.remove(Cards.foolsGold);
    Card trash = advisorGeneral(context, getCurrencyTotal(temphand), false, true);

    if (keep.equals(trash)) {
      // card to be obtained with -1 Fools Gold is the same
      return true;
    }

    return (inHandCount(Cards.foolsGold) <= 2);
  }

  @Override
  public boolean jackOfAllTrades_shouldDiscardCardFromTopOfDeck(MoveContext context, Card card) {
    ArrayList<Card> a = new ArrayList<>();
    a.add(card);
    if (card.equals(getCardToDiscard(a, DiscardOption.NonDestructive, context))
        && (jackOfAllTrades_nonTreasureToTrash(context) != null || (card.is(Type.Treasure, this)))) {
      return true;
    }
    return false;
  }

  @Override
  public Card jackOfAllTrades_nonTreasureToTrash(MoveContext context) {
    ArrayList<Card> temphand = hand.toArrayListClone();
    while (!temphand.isEmpty()) {
      Card tcard = getCardToTrash(temphand, DiscardOption.SemiDestructive);
      temphand.remove(tcard);
      temphand.trimToSize();
      if (tcard == null) {
        return null;
      }
      if (tcard.is(Type.Treasure, this)) {
        tcard = null;
      }
      if (tcard != null) {
        return tcard;
      }
    }

    return null;
  }

  @Override
  public boolean trader_shouldGainSilverInstead(MoveContext context, Card card) {
    ArrayList<Card> temp = new ArrayList<>();
    temp.add(card);
    if (card == getCardToTrash(temp, DiscardOption.SemiDestructive)) {
      return true;
    }
    return false;
  }

  @Override
  public Card trader_cardToTrash(MoveContext context) {
    return getCardToTrash(DiscardOption.Destructive);
  }

  @Override
  public boolean illGottenGains_gainCopper(MoveContext context) {
    int gold = getCurrencyTotal(hand.toArrayListClone());
    log("illGottenGains_gainCopper: evaluating with " + gold + " gold");
    return (advisorGeneral(context, gold, false, false) != advisorGeneral(context, gold + 1, false, false));
  }

  //	public double compareDeckOptions(ArrayList<Card> deck, Card option1, Card option2, int hoardsPlayed) {
  //		ArrayList<Card> o1 = new ArrayList<Card>();
  //		ArrayList<Card> o2 = new ArrayList<Card>();
  //
  //		if (option1 != null) {
  //			o1.add(option1);
  //		}
  //
  //		if (option2 != null) {
  //			o2.add(option2);
  //		}
  //
  //		return compareDeckOptions(deck, o1, o2, hoardsPlayed);
  //	}

  @Override
  public Card haggler_cardToObtain(MoveContext context, int maxCost, int maxDebt, boolean potion) {
    log("haggler_cardToObtain");
    if (maxCost < 0) {
      return null;
    } else {
      return advisorGeneral(context, maxCost, false, true);
    }
  }

  @Override
  public Card[] margrave_attack_cardsToKeep(MoveContext context) {
    return militiaAttackCardsToKeep(context);
  }

  @Override
  public Card rogue_cardToGain(MoveContext context) {

    // TODO Auto-generated method stub
    return super.rogue_cardToGain(context);
  }

  @Override
  public Card rogue_cardToTrash(MoveContext context, ArrayList<Card> canTrash) {
    return getCardToTrash(canTrash, DiscardOption.Destructive);
  }

  @Override
  public Card pillage_opponentCardToDiscard(MoveContext context, ArrayList<Card> handCards) {
    // TODO Auto-generated method stub
    return super.pillage_opponentCardToDiscard(context, handCards);
  }

  /**
   * @param s text to log via System.out (if debug enabled)
   */
  private void log(String s) {
    if (debug) {
      System.out.println("<AI> " + s);
    }
  }

  private boolean isCantrip(Card card) {
    if (card == null) {
      return false;
    }
    if (card.equals(Cards.scryingPool)) {
      return true;
    }
    if ((card.getAddActions() > 0) && (card.getAddCards() > 0)) {
      return true;
    }
    return false;
  }

  private int getCardsToEndGame() {
    int prov_col = 1000;
    int min1 = 1000;
    int min2 = 1000;
    int min3 = 1000;

    for (CardPile pile : game.piles.values()) {
      if ((pile.topCard().equals(Cards.province)) || (pile.topCard().equals(Cards.colony))) {
        if (pile.getCount() < prov_col) {
          prov_col = pile.getCount();
        }
      } else {
        if (pile.getCount() < min1) {
          min1 = pile.getCount();
        } else if (pile.getCount() < min2) {
          min2 = pile.getCount();
        } else if (pile.getCount() < min3) {
          min3 = pile.getCount();
        }
      }
    }

    return Math.min(prov_col, (min1 + min2 + min3));
  }

  /**
   * @param context context
   */
  private int numPlayers() {
    return game.getPlayersInTurnOrder().length;
  }

  /**
   * @param context context
   */
  private int guessTurnsToEnd() {
    return Math.round(getCardsToEndGame() / numPlayers()) + 1;
  }

  /**
   * @param context context
   */
  private int guessTurnsToReshuffle() {
    return Math.round(getDeckSize(deck.toArrayList()) / 5);
  }

  /**
   * @param context context
   */
  private int guessReshufflesToEnd(MoveContext context) {
    int t = guessTurnsToEnd() - guessTurnsToReshuffle();
    return (t / (getDeckSize(context) / 5)) + 1;
  }

  /**
   * This function calculates the amount of coins available in the list.
   * It will return the amount as if all treasure cards have been played.
   * For cards with variable value (Venture), it only calculates the minimum guaranteed value.
   * Usually used to evaluate treasure in hand.
   *
   * @param list list of cards
   */
  private int getCurrencyTotal(ArrayList<Card> list) {
    int money = 0;
    int bankcount = 0;

    for (Card card : list) {
      if (card.is(Type.Treasure, this)) {
        money += card.getAddGold();
      }
      if (card.equals(Cards.venture)) {
        money += 1;
      }
      if (card.equals(Cards.bank)) {
        // money += 1 + this.getMoneyPerCard(list);
        money += getTotalTreasureCards(list) - ++bankcount;
      }
      if (card.equals(Cards.philosophersStone)) {
        money += (list.size() - 7) / 5;
      }

      money += card.getAddGold();
    }

    if (Util.getCardCount(list, Cards.foolsGold) > 1) {
      money += (Util.getCardCount(list, Cards.foolsGold) - 1) * 3;
    }

    if (getMinusOneCoinToken() && money > 0) {
      money--;
    }

    return money;
  }

  /**
   * This function calculates the total amount of coin available to a player in his deck
   * The resulting amount is somehow approximated for cards like Venture, Bank, Fool's Gold
   * This function should be used to evaluate treasure in deck (or other similar lists of cards)
   */
  private int guessCurrencyTotal(ArrayList<Card> list) {
    int money = 0;

    for (Card card : list) {
      if (card.is(Type.Treasure, this)) {
        money += card.getAddGold();
      }
      money += card.getAddGold();

      if (card.equals(Cards.venture)) {
        //TODO maybe there is some way of incorporating the avg money per card without creating an infinite loop?
        money += 1;
      }
      if (card.equals(Cards.philosophersStone)) {
        money += ((list.size() - 7) / 5);   /* (list.size()/20); ??? */
      }
      if (card.equals(Cards.bank)) {
        //TODO maybe there is some way of incorporating the avg money per card without creating an infinite loop?
        money += 2;
      }
      if (card.equals(Cards.foolsGold)) {
        // we add +1 coin value to every Fools Gold beyond the first
        money += (Util.getCardCount(list, Cards.foolsGold) - 1);
      }
    }

    if (strategy == StrategyOption.Minion) {
      money += Math.round(1.6 * Util.getCardCount(list, Cards.minion));
    }

    return money;
  }

  /**
   * Function calculates the total amount of VPs for all the cards in the list.
   * Note that for cards with variable amount of VPs (Vineyard, Silk Road), the amount
   * of VP they provide is calculated based on the list only, not based on whole deck.
   *
   * Use getVPTotalValue(MoveContext context) for calculation in whole deck
   *
   * @param list list of cards
   */
  private int getVPTotalValue(ArrayList<Card> list, Player player) {
    int vps = getVictoryTokens();

    for (Card card : list) {
      if (card.is(Type.Victory, player)) {
        vps += card.getVictoryPoints();
      }
      if (card.is(Type.Curse, null)) {
        vps += card.getVictoryPoints();
      }
      if (card.equals(Cards.duke)) {
        vps += Util.getCardCount(list, Cards.duchy);
      }
      if (card.equals(Cards.gardens)) {
        vps += list.size() / 10;
      }
      if (card.equals(Cards.vineyard)) {
        vps += Math.round(getActionCardCount(list, player) / 3);
      }
      if (card.equals(Cards.fairgrounds)) {
        vps += Math.round(getCardNameCount(list) / 5);
      }
      if (card.equals(Cards.silkRoad)) {
        vps += Math.round(getCardCount(Type.Victory, list) / 4);
      }
      if (card.equals(Cards.feodum)) {
        vps += Math.round(Util.getCardCount(list, Cards.silver) / 3);
      }
      //consider also VP token making cards?
    }

    return vps;
  }

  /**
   * @param context context
   */
  private int getTotalTreasureCards(ArrayList<Card> list) {
    int moneycards = 0;
    for (Card card : list) {
      if (card.is(Type.Treasure, this)) {
        moneycards++;
      }
    }
    if (strategy == StrategyOption.Minion) {
      moneycards += Math.round(1.6 * Util.getCardCount(list, Cards.minion));
    }
    return moneycards;
  }

  private double getMoneyPerCard(ArrayList<Card> list) {
    if (!list.isEmpty()) {
      return ((double) (guessCurrencyTotal(list)) / getDeckSize(list));
    }
    return 0.0;
  }

  private double getMoneyPerCard(ArrayList<Card> list, int plustreasure, int pluscards) {
    return (double) (guessCurrencyTotal(list) + plustreasure) / (getDeckSize(list) + pluscards);
  }

  /**
   * @param list        list of cards
   * @param destructive whether this is a forced discard, or optional
   */
  @SuppressWarnings("unchecked")
  private Card getCardToDiscard(ArrayList<Card> list, DiscardOption destructive, MoveContext context) {
    // Tunnel
    if (list.contains(Cards.tunnel)) {
      return list.get(list.indexOf(Cards.tunnel));
    }

    // Curse
    int trashit = 0;
    for (Card c : list) {
      trashit += (knownTrashingCards.contains(c) ? 1 : 0);
    }
    Card[] cardsToMatch =
      {Cards.curse, Cards.rats, Cards.overgrownEstate, Cards.ruinedVillage, Cards.ruinedMarket, Cards.hovel,
        Cards.survivors, Cards.ruinedLibrary, Cards.abandonedMine, Cards.virtualRuins};
    for (Card match : cardsToMatch) {
      if (list.contains(match)) {
        if ((trashit -= Util.getCardCount(list, match)) < 0) {
          return list.get(list.indexOf(match));
        }
      }
    }
    //		if (list.contains(Cards.curse)) {
    //			int trashit = 0;
    //			for (Card c : list) {
    //				trashit += (knownTrashingCards.contains(c) ? 1 : 0);
    //			}
    //			if (trashit < Util.getCardCount(list, Cards.curse)) {
    //				return list.get(list.indexOf(Cards.curse));
    //			}
    //		}

    // Victory cards with no other function
    for (Card card : list) {
      if (isOnlyVictory(card, context.getPlayer())) {
        return card;
      }
    }

    if ((list.contains(Cards.potion)) && (!list.contains(Cards.alchemist))) {
      return list.get(list.indexOf(Cards.potion));
    }

    switch (strategy) {
      case NoAction:
        for (Card card : list) {
          if (card.is(Type.Action, this)) {
            return card;
          }
        }
        break;

      case SingleAction:
      case DoubleAction:
      case MultiAction:
      case Mirror:
      /* clear strategy and try again without strategyPossibleCards.
       * don't pick random so early */
        ArrayList<Card> cards2discard = (ArrayList<Card>) list.clone();
        for (Card card : list) {
          if (strategyPossibleCards.contains(card)) {
            cards2discard.remove(card);
          }
        }
        if (!cards2discard.isEmpty()) {
          StrategyOption saveStrategy = strategy;
          strategy = StrategyOption.Nothing;
          Card card = getCardToDiscard(cards2discard, destructive, context);
          strategy = saveStrategy;
          return card;
        }
        //			for (Card card : list) {
        //				if (!this.strategyPossibleCards.contains(card)) {
        //					return card;
        //				}
        //			}
        break;
      default:
        break;
    }

    // This is as far as we go with useless cards
    if (destructive == DiscardOption.NonDestructive) {
      return null;
    }

    // Necropolis if playing only few actions
    if (strategy != StrategyOption.MultiAction) {
      if (list.contains(Cards.necropolis)) {
        return list.get(list.indexOf(Cards.necropolis));
      }
    }

    // Copper
    if (list.contains(Cards.copper)) {
      return list.get(list.indexOf(Cards.copper));
    }

    // This is as far as we go with semi-useless cards
    if (destructive == DiscardOption.SemiDestructive) {
      return null;
    }

    // Action cards
    for (Card card : list) {
      if (card.is(Type.Action, this)) {
        return card;
      }
    }

    if (list.contains(Cards.illGottenGains)) {
      return list.get(list.indexOf(Cards.illGottenGains));
    }
    if (list.contains(Cards.loan)) {
      return list.get(list.indexOf(Cards.loan));
    }

    // Fool's Gold if only one in hand
    if (Util.getCardCount(list, Cards.foolsGold) == 1) {
      return list.get(list.indexOf(Cards.foolsGold));
    }

    // 2 coin treasures
    if (list.contains(Cards.silver)) {
      return list.get(list.indexOf(Cards.silver));
    }
    if (list.contains(Cards.harem)) {
      return list.get(list.indexOf(Cards.harem));
    }
    if (list.contains(Cards.hoard)) {
      return list.get(list.indexOf(Cards.hoard));
    }
    if (list.contains(Cards.royalSeal)) {
      return list.get(list.indexOf(Cards.royalSeal));
    }
    if (list.contains(Cards.venture)) {
      return list.get(list.indexOf(Cards.venture));
    }

    if (list.contains(Cards.contraband)) {
      return list.get(list.indexOf(Cards.contraband));
    }

    if (!list.isEmpty()) {
      return list.get(0);
    }

    return null;
  }

  /**
   * @param hand        list of cards in players hand
   * @param number      how many cards to discard
   * @param destructive discard is mandatory?
   */
  private ArrayList<Card> getCardsToDiscard(ArrayList<Card> list, int number, DiscardOption destructive,
                                             MoveContext context) {
    ArrayList<Card> ret = new ArrayList<>();
    int discarded = 0;
    Card dcard = null;

    while ((!list.isEmpty()) && (discarded < number)) {
      dcard = getCardToDiscard(list, destructive, context);
      if (dcard != null) {
        ret.add(discarded, dcard);
        list.remove(ret.get(discarded));
      } else {
        break;
      }
      discarded++;
    }

    if (ret.size() == number) {
      Collections.sort(ret, new CardCostComparator());
      return ret;
    }

    return new ArrayList<>();
  }

  private Card getCardToTrash(ArrayList<Card> list, DiscardOption destructive) {
    return getCardToTrash(list, new ArrayList<Card>(), destructive, false);
  }

  /**
   * @param context     context
   * @param list        cards in hand
   * @param destructive based on this, return only "useless" cards, or null if no such choice
   */
  private Card getCardToTrash(ArrayList<Card> list, ArrayList<Card> deck, DiscardOption destructive,
                               boolean ambassador) {
    // Curse, Overgrown Estate and Hovel should be trashed at any time
    Card[] cardsToMatch;
    if (ambassador) { // without shelters
      cardsToMatch = new Card[] {Cards.curse, Cards.rats, Cards.ruinedVillage, Cards.ruinedMarket, Cards.survivors,
        Cards.ruinedLibrary, Cards.abandonedMine, Cards.virtualRuins};
    } else {
      cardsToMatch =
        new Card[] {Cards.curse, Cards.rats, Cards.overgrownEstate, Cards.ruinedVillage, Cards.ruinedMarket,
          Cards.hovel, Cards.survivors, Cards.ruinedLibrary, Cards.abandonedMine, Cards.virtualRuins};
    }
    for (Card match : cardsToMatch) {
      if (list.contains(match)) {
        return list.get(list.indexOf(match));
      }
    }
    //		if (list.contains(Cards.curse)) {
    //			return list.get(list.indexOf(Cards.curse));
    //		}
    //		if (list.contains(Cards.overgrownEstate)) {
    //			return list.get(list.indexOf(Cards.overgrownEstate));
    //		}
    //		if (list.contains(Cards.hovel)) {
    //			return list.get(list.indexOf(Cards.hovel));
    //		}

    // Potions should be trashed if they are not useful anymore
    if (list.contains(Cards.potion)) {
      switch (strategy) {
        case NoAction:
          return list.get(list.indexOf(Cards.potion));
        case SingleAction:
          if (Util.getCardCount(deck, strategyCard) > 0) {
            return list.get(list.indexOf(Cards.potion));
          }
          break;
        case DoubleAction:
          if (Util.getCardCount(deck, strategyCard) > 1) {
            return list.get(list.indexOf(Cards.potion));
          }
          break;
        case MultiAction:
          if ((game.pileSize(Cards.alchemist) < 1) && (Util.getCardCount(deck, Cards.alchemist) > 0)) {
            return list.get(list.indexOf(Cards.potion));
          }
          break;
        default:
          break;
      }
    }

    // Estate can be trashed when we have less then 2 province/colony
    if (list.contains(Cards.estate)) {
      if ((game.pileSize(Cards.province) > 3) || (game.pileSize(Cards.colony) > 3)) {
        return list.get(list.indexOf(Cards.estate));
      }
    }

    // Copper can be trashed if we have high money/card ratio
    if ((getMoneyPerCard(deck) >= 1) || (strategyCard != null && strategyCard.equals(Cards.chapel))) {
      if (list.contains(Cards.copper)) {
        return list.get(list.indexOf(Cards.copper));
      }
    }

    if (destructive == DiscardOption.NonDestructive) {
      return null;
    }

    if (list.contains(Cards.estate)) {
      return list.get(list.indexOf(Cards.estate));
    }

    if (list.contains(Cards.copper)) {
      return list.get(list.indexOf(Cards.copper));
    }

    if (destructive == DiscardOption.SemiDestructive) {
      return null;
    }

    Card ret = Cards.colony;
    for (Card c : list) {
      if (c.getCost(null) < ret.getCost(null) && !c.is(Type.Prize, this)) {
        ret = list.get(list.indexOf(c));
      }
    }
    return ((destructive == DiscardOption.Destructive) ? (list.isEmpty() ? null : ret) : null);
  }

  private boolean inDeck(MoveContext context, Card card) {
    return (inDeckCount(context, card) > 0);
  }

  private int inDeckCount(MoveContext context, Card card) {
    return Util.getCardCount(getAllCards(), card);
  }

  /**
   * Function calculates the total amount of VPs for all the cards in the deck.
   *
   * @param context context
   */
  private int getVPTotalValue(MoveContext context) {
    return getVPTotalValue(getAllCards(), context.player);
  }

  /**
   * @param context   context
   * @param gold      coins available to buy
   * @param exact     card must cost exactly the value
   * @param mandatory must return a card (not nothing)
   */
  //@SuppressWarnings("unused")
  private Card advisorGeneral(MoveContext context, int gold, boolean exact, boolean mandatory) {
    if (shouldReEvaluateStrategy()) {
      advisorAction();
      log("strategy options: " + strategyPossibleCards);
      redefineStrategy = false;
    }

    ArrayList<Card> deck = getAllCards();

    double mpc = getMoneyPerCard(deck);
    float cph = getCardsPerHand(context);

    int allowedTerminals = Math.max(1, Math.round(getDeckSize(context) / ((1 + cph) * 2)));
    switch (strategy) {
      case Minion:
        allowedTerminals = 1;
      case SingleAction:
        allowedTerminals = (int) Math.round(allowedTerminals / 1.5);
        break;
      case NoAction:
        allowedTerminals = 0;
        break;
      default:
        break;
    }

    log("allowedTerminals: " + getDeckSize(context) + " / " + ((1 + cph) * 2) + " = " + allowedTerminals);
    log("getStrategyCardsInDeck: " + getStrategyCardsInDeck(context, true));
    log("getTerminalsInDeck: " + getTerminalsInDeck(context));

    // here we check each card available for buy
    // the goal is to find the best VP card, best treasure and best action card
    ArrayList<Card> potentialBuys = new ArrayList<>();
    ArrayList<Card> special_cards = new ArrayList<>();
    Card maxVP_card = null;
    Card maxMPC_card = null;
    int maxvp = -1;
    double maxmpc = -1;
    for (CardPile pile : game.piles.values()) {
      Card card = pile.topCard();

      if (!exact || card.getCost(context) == gold) {
        if (game.isValidBuy(context, card, gold)) {

          if ((card.is(Type.Victory)) && (!specialCards.contains(card))) {
            int vp = card.getVictoryPoints();
            if (card.equals(Cards.gardens)) {
              vp += Math.floor(deck.size() / 10);
            }
            if (card.equals(Cards.duke)) {
              vp += Util.getCardCount(deck, Cards.duchy);
            }
            if (card.equals(Cards.duchy)) {
              vp += Util.getCardCount(deck, Cards.duke);
            }
            if (card.equals(Cards.silkRoad)) {
              vp += Math.floor(getCardCount(Type.Victory, deck) / 4);
            }
            if (card.equals(Cards.feodum)) {
              vp += Math.floor(Util.getCardCount(getAllCards(), Cards.silver) / 3);
            }
            if (game.embargos.containsKey(card.getName())) {
              vp -= (game.embargos.get(card.getName()) + guessReshufflesToEnd(context));
            }

            // don't end the game if losing and the last card will not secure a win
            // this is not really good with +buys, as it will prevent buying multiple cards to secure a win
            // TODO review, because with >1 buys, will buy the last card anyway
            if ((willEndGameGaining(card)) && (winningBy(context) < (0 - vp)) && (context.buys == 1)) {
              log(
                "can't recommend " + card + ", would lose game by " + (winningBy(context) + (vp * (vp > 0 ? 1 : -1))));
              continue;
            }

            if (vp >= maxvp) {
              maxvp = vp;
              maxVP_card = card;
            }
          } // victory

          // don't end the game if losing
          if ((willEndGameGaining(card)) && (winningBy(context) < 0) && (context.buys == 1)) {
            log("can't recommend " + card + ", would lose game by " + Math.abs(winningBy(context)) + " VP");
            continue;
          }

          if ((card.is(Type.Treasure, this)) && (!specialCards.contains(card))) {
            ArrayList<Card> tempdeck = new ArrayList<>(deck);
            tempdeck.add(card);

            if (card.equals(Cards.cache)) {
              tempdeck.add(Cards.copper);
              tempdeck.add(Cards.copper);
            }
            if (game.embargos.containsKey(card.getName())) {
              for (int i = 0; i < game.embargos.get(card.getName()); i++) {
                tempdeck.add(Cards.curse);
              }
            }

            double tmpc = getMoneyPerCard(tempdeck);

            if (tmpc > maxmpc) {
              maxmpc = tmpc;
              maxMPC_card = card;
            }
          } // treasure

          if ((card.equals(Cards.golem)) && (getActionCardCount(deck, context.player) > 1) && (strategy
                                                                                               != StrategyOption.NoAction)) {
            log("action: Golem (have " + getActionCardCount(deck, context.player) + " actions)");
            potentialBuys.add(Cards.golem);
          }

          // action cards
          if ((strategyPossibleCards.contains(card)) && ((game.pileSize(Cards.curse) > 3) || (!knownCursingCards
                                                                                                 .contains(
                                                                                                   card)))) {

            // we can buy another piece of "single" card only when the deck is big enough

            switch (strategy) {
              case Minion:
                log("action: " + card + " (have " + (inDeckCount(context, card) + ")"));
                potentialBuys.add(card);
                break;
              case DoubleAction:
                if ((allowedTerminals < 2) && (Util.getCardCount(deck, Cards.gold) > 0)) {
                  allowedTerminals = 2;
                }

              case SingleAction:
                if (strategyPossibleCards.contains(card)) {
                  if ((getStrategyCardsInDeck(context, false).size() < allowedTerminals) || (isCantrip(card))) {
                    log("action: " + card + " (have " + (inDeckCount(context, card) + ")"));
                    potentialBuys.add(card);
                    potentialBuys.add(card);
                  }
                }

                if ((knownMultiActionCards.contains(card)) && (rand.nextInt(4) == 1)) {
                  //this.log("action: extra " + card);
                  potentialBuys.add(card);
                }
                break;

              case MultiAction:
                // choose at every opportunity
                if (strategyPossibleCards.contains(card)) {
                  log("action: evaluating another " + card);
                  if ((!knownComboActionCards.contains(card)) || (rand.nextBoolean()) || (card
                                                                                            .costPotion())) {
                    ArrayList<Card> temp = new ArrayList<>(getAllCards());
                    temp.retainAll(knownComboActionCards);
                    //this.log(card + (isCantrip(ac) ? " is " : " isn't ") + "cantrip");
                    if (temp.size() * 2 <= getStrategyCardsInDeck(context, false).size() + (isCantrip(card) ? 1
                                                                                              : 0)) {
                      potentialBuys.add(card);
                    }
                  }
                }
                if ((knownSingleActionCards.contains(card) || knownDoubleActionCards.contains(card))
                    && (getTerminalsInDeck(context).size() < allowedTerminals)) {
                  //this.log("action: terminal " + card);
                  potentialBuys.add(card);
                }
                break;

              case Mirror:
                if (opponents.getActionCards().contains(card)) {
                  potentialBuys.add(card);
                  //this.log("action: same " + card);
                }
              default:
                break;
            }
          }

          if (specialCards.contains(card)) {
            special_cards.add(card);
          }
        }
      }
    }

    log("VPs: " + winningBy(context));
    log("best basic mpc: " + maxMPC_card);
    log("best vp: " + maxVP_card);
    log("specials: " + special_cards);
    log("potential actions: " + potentialBuys);

    int embargopiles = 0;

    Card action_card = null;
    while (!potentialBuys.isEmpty()) {
      action_card = Util.randomCard(potentialBuys);
      potentialBuys.remove(action_card);
      if (game.embargos.containsKey(action_card.getName())) {
        log("action " + action_card + " is embargoed, skipping");
        action_card = null;
        embargopiles++;
      }
    }
    log("picked action: " + action_card);
    if ((action_card == null) && (embargopiles > 0)) {
      redefineStrategy = true;
    }

    if (!special_cards.isEmpty()) {

      Card scard = Cards.loan;
      if (special_cards.contains(scard)) {
        // we have no loan in deck and significantly more copper then other treasures
        if ((inDeck(context, scard)) || (inDeckCount(context, Cards.copper) * 2 <= getCurrencyTotal(
          context))) {
          // buying only when there are a lot of coppers and only 1 piece
          log("Loan not good, " + inDeckCount(context, Cards.copper)
              + " coppers in deck and total treasure value " + getCurrencyTotal(context));
          special_cards.remove(scard);
        }
      }

      scard = Cards.bank;
      if (special_cards.contains(scard)) {
        // TODO tpc counted badly, to check again
        double tpc = (getCardCount(Type.Treasure) / getDeckSize(deck));
        if (tpc * 5.0 < 3.0) {
          log("Bank not good, tpc is " + tpc);
          special_cards.remove(scard);
        }
      }

      scard = Cards.hoard;
      if (special_cards.contains(scard)) {
        if ((Util.getCardCount(deck, Cards.gold) <= Util.getCardCount(deck, Cards.hoard)) || game.isPlatInGame()) {
          log("Hoard not good, either platinum in play or have "
              + Util.getCardCount(deck, Cards.gold) + " gold and "
              + Util.getCardCount(deck, Cards.hoard) + " hoards");
          special_cards.remove(scard);
        }
      }

      // here are the generic "Better then Silver" cards based on value

      scard = Cards.harem;
      if (special_cards.contains(scard)) {
        if ((mpc < 1.2) || (game.isPlatInGame())) {
          log("Harem not good, mpc = " + mpc);
          special_cards.remove(scard);
        }
      }

      if (maxMPC_card != null) {
        scard = Cards.venture;
        if (special_cards.contains(scard)) {
          if (maxMPC_card.getAddGold() > 2) {
            special_cards.remove(scard);
          }
        }

        scard = Cards.royalSeal;
        if (special_cards.contains(scard)) {
          if (maxMPC_card.getAddGold() > 2) {
            special_cards.remove(scard);
          }
        }

        scard = Cards.foolsGold;
        if (special_cards.contains(scard)) {
          if (maxMPC_card.getAddGold() > 1) {
            special_cards.remove(scard);
          }
        }
      }

      scard = Cards.contraband;
      if (special_cards.contains(scard)) {
        special_cards.remove(scard);
      }

      scard = Cards.potion;
      if ((special_cards.contains(scard)) && (!needMorePotion(deck))) {
        special_cards.remove(scard);
      }

      scard = Cards.farmland;
      if (special_cards.contains(scard)) {
        if (maxVP_card != null && maxVP_card.getVictoryPoints() < 4) {
          if (!(hand.contains(Cards.curse))) {
            special_cards.remove(scard);
          }
        }
      }
    }

    log("specials : " + special_cards);

    if (!special_cards.isEmpty()) {
      int scost = -1;
      int svalue = -1;
      //int spoints = -1;

      for (Card c : special_cards) {

        // potion is special because it can't be compared based on value
        if (c.equals(Cards.potion)) {
          log("potion: have " + Util.getCardCount(deck, Cards.potion) +
              " and " + Util.getCardCount(deck, Cards.alchemist) +
              " Alchemist(s) in " + deck.size() + " cards");
          if (needMorePotion(deck)) {
            switch (strategy) {
              case SingleAction:
                if ((Util.getCardCount(deck, Cards.potion) < 1) && (Util.getCardCount(deck, strategyCard) < 1)) {
                  maxMPC_card = c;
                  scost = 1000;
                  svalue = 1000;
                }
                break;
              case DoubleAction:
                if ((Util.getCardCount(deck, Cards.potion) < 1) && (Util.getCardCount(deck, strategyCard) < 2)) {
                  maxMPC_card = c;
                  scost = 1000;
                  svalue = 1000;
                }
                break;
              case MultiAction:
                if ((Util.getCardCount(deck, Cards.potion) * Math.max(10,
                  (20 - (Util.getCardCount(deck, Cards.alchemist) * 2)))) < deck.size()) {
                  maxMPC_card = c;
                  scost = 1000;
                  svalue = 1000;
                }
                break;
              case Mirror:
                for (Card op : opponents.getActionCards()) {
                  if (op.costPotion()) {
                    if ((Util.getCardCount(deck, Cards.potion) * Math.max(10,
                      (20 - (Util.getCardCount(deck, Cards.alchemist) * 2)))) < deck.size()) {
                      maxMPC_card = c;
                      scost = 1000;
                      svalue = 1000;
                    } else if (Util.getCardCount(deck, Cards.potion) < 1) {
                      maxMPC_card = c;
                      scost = 1000;
                      svalue = 1000;
                    }
                  }
                }
                break;
              default:
                break;
            }
          }
        } // potion
        else if (specialTreasureCards.contains(c)) {
          if ((c.getAddGold() > svalue) && (c.getCost(context) > scost)) {
            scost = c.getCost(context);
            svalue = c.getAddGold();
            maxMPC_card = c;
          }
        } else if (specialVictoryCards.contains(c)) {
          if (c.equals(Cards.farmland)) {
            if ((hand.contains(Cards.curse)) && (maxVP_card == null || maxVP_card.getVictoryPoints() <= 4)) {
              maxVP_card = c;
            }
          }
        }
      }
    }

    log("best final mpc: " + maxMPC_card);
    log("best final vp: " + maxVP_card);

    if (maxMPC_card != null) {
      if (maxMPC_card.equals(Cards.copper)) {
        if (mpc * 5 > 3) {
          maxMPC_card = null;
        }
      }
    }

    log("current deck mpc: " + mpc);
    log("guessTurnsToReshuffle(): " + guessTurnsToReshuffle());
    log("guessTurnsToEnd(): " + guessTurnsToEnd());
    log("best action: " + action_card);

    // this condition is the reason why Patrick doesn't buy princes
    if (action_card != null && maxVP_card != null && (maxVP_card.getVictoryPoints() < 6)) {
      log("choosing action");

      switch (strategy) {
        case Minion:
          return action_card;
        case Mirror:
          opponents.getActionCards().remove(action_card);
          return action_card;

        case SingleAction:
        case DoubleAction:
          if (strategyPossibleCards.contains(action_card)) {
            return action_card;
          }
          break;

        case MultiAction:
          int acvalue =
            Math.max(action_card.getCost(context), 3) + action_card.getAddGold() + action_card.getAddActions();

          if (action_card.costPotion()) {
            acvalue += 2;
          }

          log("action card value: " + acvalue);

          if (acvalue >= gold) {
            if (!knownMultiActionCards.contains(action_card)) {
              strategyMultiCardTerminal = action_card;
              log("multi action terminal: " + strategyMultiCardTerminal);
            }
            if (guessCurrencyTotal(getAllCards()) > 7 + getActionCardCount(context.player)) {
              return action_card;
            }
          }
          break;
        default:
          break;
      }
    }

    if (maxVP_card != null) {
      if (!exact || maxVP_card.getCost(context) == gold) {
        if ((mpc > (1.9 - (maxVP_card.getVictoryPoints() * 0.15 * (context.countCardsInPlay(Cards.hoard) + 1))))
            || ((guessTurnsToReshuffle() > guessTurnsToEnd()) && (maxVP_card.getVictoryPoints() > 1))) {
          log("choosing victory (hoards: " + context.countCardsInPlay(Cards.hoard) + ")");
          return maxVP_card;
        }
      }
    }

    if (maxMPC_card != null) {
      if (!exact || maxMPC_card.getCost(context) == gold) {
        log("choosing treasure");
        return maxMPC_card;
      }
    }

    if (mandatory) {
      log("must choose a card");
      for (CardPile pile : game.piles.values()) {
        Card card = pile.topCard();
        if (!exact || card.getCost(context) == gold) {
          if ((game.isValidBuy(context, card, gold)) && !(card.equals(Cards.curse))) {
            return card;
          }
        }
      }
    }

    log("choosing nothing");
    return null;
  }

  private boolean willEndGameGaining(Card card) {
    if (game.emptyPiles() > 2) {
      return true;
    }
    if ((card.equals(Cards.province)) && (game.pileSize(card) == 1)) {
      return true;
    }
    if ((card.equals(Cards.colony)) && (game.pileSize(card) == 1)) {
      return true;
    }
    if (game.emptyPiles() == 2) {
      if (game.pileSize(card) == 1) {
        return true;
      }
      if ((knownCursingCards.contains(card)) && (game.pileSize(Cards.curse) == 1)) {
        return true;
      }
      if ((card.equals(Cards.cache)) && (game.pileSize(Cards.copper) <= 2) && (game.pileSize(Cards.copper) > 0)) {
        return true;
      }
    }

    return false;
  }

  private void advisorAction() {
    strategyPossibleCards.clear();

    boolean shouldReCurse = false;
    for (Card card : opponents.getActionCards()) {
      if ((knownCursingCards.contains(card)) && (game.pileSize(Cards.curse) > (guessTurnsToReshuffle() + 2))) {
        shouldReCurse = true;
      }
    }

    ArrayList<Card> cards = new ArrayList<>();
    for (CardPile pile : game.piles.values()) {
      if ((knownActionCards.contains(pile.topCard())) && (pile.getCount() > 2)) {
        if ((knownCursingCards.contains(pile.topCard())) || (!shouldReCurse)) {
          if (game.embargos.containsKey(pile.topCard().getName())) {
            log("advisorAction: skipped " + pile.topCard() + " due to embargo");
          } else {
            cards.add(pile.topCard());
          }
        }
      }
    }

    log("advisorAction: considering " + cards.size() + " action cards out of " + knownActionCards.size()
        + " total known cards");

    ArrayList<Card> tier1 = new ArrayList<>(cards);
    tier1.retainAll(knownDoubleActionCards);
    if (!tier1.isEmpty()) {
      log("advisorAction: found Tier1 cards " + tier1);
      cards.clear();
      for (Card c : tier1) {
        if (game.pileSize(c) > 2) {
          cards.add(c);
        }
      }
    }

    if (cards.isEmpty()) {
      strategy = StrategyOption.NoAction;
      log("advisorAction: pure big money");
    } else {
      // pick random card to base strategy on
      strategyCard = null;
      while ((!cards.isEmpty()) && (strategyCard == null)) {
        strategyCard = cards.get(rand.nextInt(cards.size()));
        cards.remove(strategyCard);
        if (game.pileSize(strategyCard) < 3) {
          strategyCard = null;
        }
      }
      if (strategyCard != null) {
        strategyPossibleCards.add(strategyCard);
      } else {
        return;
      }

      if (strategyCard.equals(Cards.minion)) {
        strategy = StrategyOption.Minion;
        log("advisorAction: " + strategyCard);
      } else if (knownMultiActionCards.contains(strategyCard)) {
        strategy = StrategyOption.MultiAction;
        log("advisorAction: multiple cantrips and combo cards (via " + strategyCard + ")");

        for (CardPile pile : game.piles.values()) {
          if (knownMultiActionCards.contains(pile.topCard())) {
            strategyPossibleCards.add(pile.topCard());
          }
        }
        for (CardPile pile : game.piles.values()) {
          if (knownComboActionCards.contains(pile.topCard())) {
            strategyPossibleCards.add(pile.topCard());
          }
        }
      } else if (knownDoubleActionCards.contains(strategyCard)) {
        strategy = StrategyOption.DoubleAction;
        log("advisorAction: double " + strategyCard);
      } else if (knownSingleActionCards.contains(strategyCard)) {
        strategy = StrategyOption.SingleAction;
        log("advisorAction: single " + strategyCard);
      }
    }

  }

  private int getDeckSize(MoveContext context) {
    return getDeckSize(getAllCards());
  }

  private int getDeckSize(ArrayList<Card> deck) {
    int size = 0;
    for (Card card : deck) {
      size++;
      if (isCantrip(card)) {
        size--;
      }
    }
    return size;
  }

  private int getCardNameCount(ArrayList<Card> deck) {
    ArrayList<String> names = new ArrayList<>();

    for (Card card : deck) {
      if (!names.contains(card.toString())) {
        names.add(card.toString());
      }

    }

    return names.size();
  }

  private boolean needMorePotion(ArrayList<Card> deck) {
    if (!strategyPossibleCards.isEmpty()) {
      if (strategyCard.costPotion()) {
        switch (strategy) {
          case SingleAction:
          case DoubleAction:
            return (Util.getCardCount(deck, Cards.potion) == 0);
          case NoAction:
            return false;
          case MultiAction:
            if (Util.getCardCount(deck, Cards.potion) < 1) {
              return true;
            }
            if ((Util.getCardCount(deck, Cards.alchemist) > 1) && (deck.size() / 5 > Util.getCardCount(deck,
              Cards.potion))) {
              return true;
            }
            break;
          default:
            break;
        }
      }
    }
    return false;
  }

  @SuppressWarnings("unused")
  private int needPotion() { // TODO must be tested

    if (strategyPossibleCards.isEmpty()) {
      return 0;
    }

    float needpotion = 0;
    for (Card ac : strategyPossibleCards) {
      if (ac.costPotion() && needpotion < 1) {
        needpotion = 1;
      }
      if (ac.equals(Cards.alchemist)) {
        needpotion = (getAllCards().size() / (6 + (Util.getCardCount(getAllCards(), Cards.alchemist) * 3)));
      }
    }
    return Math.round(needpotion);
  }

  private int winningBy(MoveContext context) {
    int maxVP = opponents.maxVP();
    return (maxVP > -1000 ? getVPTotalValue(context) - maxVP : 0);
  }

  private Card getCardToTrash(DiscardOption destructive) {
    return getCardToTrash(hand.toArrayListClone(), getAllCards(), destructive, false);
  }

  private boolean shouldReEvaluateStrategy() {
    if (strategy == StrategyOption.Nothing) {
      return true;
    }

    if (redefineStrategy) {
      return true;
    }

    if (opponents != null && opponents.getIsAttacking() && strategyCard != null && !strategyCard
                                                                                      .is(Type.Attack,
                                                                                        this)) {
      return true;
    }

    if (strategy == StrategyOption.Minion) {
      return false;
    }

    int available = 0;
    for (Card c : strategyPossibleCards) {
      available += game.pileSize(c);
    }
    return (available == 0);
  }

  private ArrayList<Card> getStrategyCardsInDeck(MoveContext context, boolean onlyTerminals) {

    ArrayList<Card> ret = new ArrayList<>();

    for (Card card : getAllCards()) {
      if (strategyPossibleCards.contains(card)) {
        if (!isCantrip(card) || !onlyTerminals) {
          ret.add(card);
        }
      }
    }

    return ret;
  }

  private ArrayList<Card> getTerminalsInDeck(MoveContext context) {
    ArrayList<Card> ret = new ArrayList<>();

    for (Card card : getAllCards()) {
      if (card.is(Type.Action, context.player)) {
        if (card.getAddActions() <= 0) {
          ret.add(card);
        }
      }
    }

    return ret;
  }

  private float getCardsPerHand(MoveContext context) {
    int addcards = 0;

    for (Card card : getAllCards()) {
      if (card.is(Type.Action, context.player)) {
        addcards += card.getAddCards();
      }
    }

    return (addcards / 5) + 5;
  }

  private Card advisorPlayAction(ArrayList<Card> hand, Player player) {
    log("advisorPlayAction: " + hand);
    ArrayList<Card> ac = new ArrayList<>();
    for (Card card : hand) {
      if (card.is(Type.Action, player)) {
        ac.add(card);
      }
    }
    ArrayList<Card> temphand = new ArrayList<>(hand);

    if (!ac.isEmpty()) {
      if (ac.contains(Cards.kingsCourt)) {
        temphand.remove(Cards.kingsCourt);
        Card temp = advisorPlayAction(temphand, player);
        if (temp != null) {
          return hand.get(hand.indexOf(Cards.kingsCourt));
        }
      }
      if (ac.contains(Cards.throneRoom)) {
        temphand.remove(Cards.throneRoom);
        Card temp = advisorPlayAction(temphand, player);
        if (temp != null) {
          return hand.get(hand.indexOf(Cards.throneRoom));
        }
      }
      if (ac.contains(Cards.disciple)) {
        temphand.remove(Cards.disciple);
        Card temp = advisorPlayAction(temphand, player);
        if (temp != null) {
          return hand.get(hand.indexOf(Cards.disciple));
        }
      }

      for (Card a : ac) {
        if (knownTrashingCards.contains(ac)) {
          if ((getDeckSize() < 6) || (getCardToTrash(DiscardOption.SemiDestructive) == null) || (a.equals(
            Cards.masquerade))) {
            ac.remove(a);
          }
        }
      }

      // don't play candidates if prince is on hand
      Card[] princeCards;
      if (ac.contains(Cards.prince)) {
        ArrayList<Card> cardList = new ArrayList<>();
        for (Card c : ac) {
          cardList.add(c);
        }
        // we haven't got context, so we can't regard highways
        princeCards = prince_cardCandidates(null/*context*/, cardList, false);
      } else {
        princeCards = new Card[0];
      }

      for (Card a : ac) {
        if (isCantrip(a) && !isInCardArray(a, princeCards)) {
          return a;
        }
      }

      for (Card a : ac) {
        if (a.getAddActions() > 0 && !isInCardArray(a, princeCards)) {
          return a;
        }
      }

      if (princeCards.length != 0) {
        return hand.get(hand.indexOf(Cards.prince));
      }

      Card bestcoin = Cards.village;
      Card bestcards = Cards.militia;

      for (Card a : ac) {
        if (a.equals(Cards.tactician) && playedCards.contains(Cards.tactician)) {
          continue;
        }
        if (a.getAddGold() > bestcoin.getAddGold()) {
          bestcoin = a;
        }
        if (a.getAddCards() > bestcards.getAddCards()) {
          bestcards = a;
        }
      }

      if ((bestcoin.getAddGold() > (bestcards.getAddCards() * getMoneyPerCard(deck.toArrayList()))) && (!bestcoin
                                                                                                           .equals(
                                                                                                             Cards.village))) {
        return bestcoin;
      }
      if (!bestcards.equals(Cards.militia)) {
        return bestcards;
      }

      return ac.get(rand.nextInt(ac.size()));
    }

    return null;
  }

  private enum DiscardOption {
    Destructive,
    SemiDestructive,
    NonDestructive
  }

  private enum StrategyOption {
    Nothing,
    NoAction,
    SingleAction,
    DoubleAction,
    MultiAction,
    Mirror,
    Minion
  }

  private static class Opponent {

    private final ArrayList<Card> actionCards;
    private final int playerID;
    private int VP;
    private boolean isAttacking;

    public Opponent(int id) {
      actionCards = new ArrayList<>();
      VP = -1000;
      isAttacking = false;
      playerID = id;
    }

    public int getVP() {
      return VP + Game.players[playerID].getVictoryTokens();
    }

    public void setVP(int vP) {
      VP = vP;
    }

    public boolean getIsAttacking() {
      return isAttacking;
    }

    public ArrayList<Card> getActionCards() {
      return actionCards;
    }

    public void addVP(int vP) {
      VP += vP;
    }

    public void setAttacking(boolean isAttacking) {
      this.isAttacking = isAttacking;
    }

    public void putActionCard(Card card) {
      actionCards.add(card);
    }

    @Override
    public String toString() {
      return "Opponent [actionCards=" + actionCards + ", VP=" + VP
             + ", isAttacking=" + isAttacking + "]";
    }
  }

  private static class OpponentList extends HashMap<Integer, Opponent> {

    private static final long serialVersionUID = -9007482931936952794L;
    private final HashMap<Integer, Opponent> opponents;

    public OpponentList() {
      opponents = new HashMap<>();
    }

    public int maxVP() {
      int ret = -1000;
      for (Opponent o : opponents.values()) {
        if (o.getVP() > ret) {
          ret = o.getVP();
        }
      }
      if (ret > -1000) {
        return ret;
      } else {
        return -1;
      }
    }

    public ArrayList<Card> getActionCards() {
      ArrayList<Card> ret = new ArrayList<>();
      for (Opponent o : opponents.values()) {
        for (Card c : o.getActionCards()) {
          ret.add(c);
        }
      }
      return ret;
    }

    public boolean getIsAttacking() {
      boolean ret = false;
      for (Opponent o : opponents.values()) {
        if (o.getIsAttacking()) {
          ret = true;
        }
      }
      return ret;
    }

    @Override
    public String toString() {
      return "OpponentList [opponents=" + opponents + "]";
    }

    @Override
    public int size() {
      return opponents.size();
    }

    @Override
    public boolean isEmpty() {
      return opponents.isEmpty();
    }

    @Override
    public Opponent get(Object key) {
      return opponents.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
      return opponents.containsKey(key);
    }

    @Override
    public Opponent put(Integer key, Opponent value) {
      return opponents.put(key, value);
    }

    @Override
    public void clear() {
      opponents.clear();
    }
  }

  static { // specialCards
    specialTreasureCards.add(Cards.foolsGold);
    specialTreasureCards.add(Cards.loan);
    specialTreasureCards.add(Cards.hoard);
    specialTreasureCards.add(Cards.royalSeal);
    specialTreasureCards.add(Cards.venture);
    specialTreasureCards.add(Cards.bank);
    specialTreasureCards.add(Cards.contraband);
    specialTreasureCards.add(Cards.potion);

    specialVictoryCards.add(Cards.harem);
    specialVictoryCards.add(Cards.farmland);
    specialVictoryCards.add(Cards.feodum);

    //populate
    for (Card c : specialTreasureCards) {
      specialCards.add(c);
    }
    for (Card c : specialVictoryCards) {
      specialCards.add(c);
    }
    for (Card c : specialActionCards) {
      specialCards.add(c);
    }
  }

  static { // knownActionCards
    knownSingleActionCards.add(Cards.smithy);
    knownSingleActionCards.add(Cards.councilRoom);
    knownSingleActionCards.add(Cards.woodcutter);
    knownSingleActionCards.add(Cards.moneyLender);
    knownSingleActionCards.add(Cards.chapel);
    knownSingleActionCards.add(Cards.nomadCamp);
    knownSingleActionCards.add(Cards.steward);
    knownSingleActionCards.add(Cards.bishop);
    knownSingleActionCards.add(Cards.library);
    knownSingleActionCards.add(Cards.haggler);
    knownSingleActionCards.add(Cards.monument);
    knownSingleActionCards.add(Cards.vault);
    knownSingleActionCards.add(Cards.merchantShip);
    knownSingleActionCards.add(Cards.jackOfAllTrades);
    knownSingleActionCards.add(Cards.bridge);
    knownSingleActionCards.add(Cards.harvest);
    knownSingleActionCards.add(Cards.tactician);
    knownSingleActionCards.add(Cards.tournament);
    knownSingleActionCards.add(Cards.nobleBrigand);
    knownSingleActionCards.add(Cards.tradeRoute);
    knownSingleActionCards.add(Cards.envoy);
    knownSingleActionCards.add(Cards.butcher);
    knownSingleActionCards.add(Cards.journeyman);
    knownSingleActionCards.add(Cards.prince);

    knownDoubleActionCards.add(Cards.wharf);
    knownDoubleActionCards.add(Cards.jackOfAllTrades);
    knownDoubleActionCards.add(Cards.ghostShip);
    knownDoubleActionCards.add(Cards.courtyard);
    knownDoubleActionCards.add(Cards.witch);
    knownDoubleActionCards.add(Cards.mountebank);
    knownDoubleActionCards.add(Cards.seaHag);
    knownDoubleActionCards.add(Cards.militia);
    knownDoubleActionCards.add(Cards.rabble);
    knownDoubleActionCards.add(Cards.margrave);
    knownDoubleActionCards.add(Cards.familiar);
    knownDoubleActionCards.add(Cards.torturer);
    knownDoubleActionCards.add(Cards.ambassador);
    knownDoubleActionCards.add(Cards.saboteur);
    knownDoubleActionCards.add(Cards.minion);
    knownDoubleActionCards.add(Cards.masquerade);
    knownDoubleActionCards.add(Cards.rogue);
    knownDoubleActionCards.add(Cards.pillage);
    knownDoubleActionCards.add(Cards.butcher);

    knownMultiActionCards.add(Cards.laboratory);
    knownMultiActionCards.add(Cards.market);
    knownMultiActionCards.add(Cards.bazaar);
    knownMultiActionCards.add(Cards.treasury);
    knownMultiActionCards.add(Cards.miningVillage);
    knownMultiActionCards.add(Cards.caravan);
    knownMultiActionCards.add(Cards.alchemist);
    knownMultiActionCards.add(Cards.scryingPool);
    knownMultiActionCards.add(Cards.baker);
    knownMultiActionCards.add(Cards.governor);

    knownComboActionCards.add(Cards.throneRoom);
    knownComboActionCards.add(Cards.disciple);
    knownComboActionCards.add(Cards.kingsCourt);
    knownComboActionCards.add(Cards.huntingParty);
    knownComboActionCards.add(Cards.peddler);
    knownComboActionCards.add(Cards.city);
    knownComboActionCards.add(Cards.grandMarket);
    knownComboActionCards.add(Cards.village);
    knownComboActionCards.add(Cards.workersVillage);
    knownComboActionCards.add(Cards.fishingVillage);
    knownComboActionCards.add(Cards.farmingVillage);
    knownComboActionCards.add(Cards.borderVillage);
    knownComboActionCards.add(Cards.shantyTown);
    knownComboActionCards.add(Cards.highway);
    knownComboActionCards.add(Cards.festival);
    knownComboActionCards.add(Cards.sage);
    knownComboActionCards.add(Cards.fortress);
    knownComboActionCards.add(Cards.banditCamp);
    knownComboActionCards.add(Cards.marketSquare);
    knownComboActionCards.add(Cards.wanderingMinstrel);
    knownComboActionCards.add(Cards.advisor);
    knownComboActionCards.add(Cards.candlestickMaker);
    knownComboActionCards.add(Cards.herald);
    knownComboActionCards.add(Cards.plaza);
    knownComboActionCards.add(Cards.walledVillage);

    knownTier3Cards.add(Cards.bureaucrat);
    knownTier3Cards.add(Cards.adventurer);
    knownTier3Cards.add(Cards.conspirator);
    knownTier3Cards.add(Cards.coppersmith);
    knownTier3Cards.add(Cards.scout);
    knownTier3Cards.add(Cards.tribute);
    knownTier3Cards.add(Cards.lighthouse);
    knownTier3Cards.add(Cards.cutpurse);
    knownTier3Cards.add(Cards.outpost);
    knownTier3Cards.add(Cards.apothecary);
    knownTier3Cards.add(Cards.countingHouse);
    knownTier3Cards.add(Cards.fortuneTeller);
    knownTier3Cards.add(Cards.menagerie);
    knownTier3Cards.add(Cards.crossroads);
    knownTier3Cards.add(Cards.ironworks);
    knownTier3Cards.add(Cards.duchess);
    knownTier3Cards.add(Cards.watchTower);
    knownTier3Cards.add(Cards.lookout);
    knownTier3Cards.add(Cards.rebuild);

    // knownPrizeCards should be sorted according to importance
    knownPrizeCards.add(Cards.followers);
    knownPrizeCards.add(Cards.diadem);
    knownPrizeCards.add(Cards.princess);
    knownPrizeCards.add(Cards.bagOfGold);

    // implemented separately
    //knownMultiActionCards.add(Cards.golem);

    //populate
    for (Card c : knownSingleActionCards) {
      knownActionCards.add(c);
    }
    for (Card c : knownDoubleActionCards) {
      knownActionCards.add(c);
    }
    for (Card c : knownMultiActionCards) {
      knownActionCards.add(c);
    }

    knownDefenseCards.add(Cards.watchTower);
    knownDefenseCards.add(Cards.moat);

    knownCursingCards.add(Cards.witch);
    knownCursingCards.add(Cards.seaHag);
    knownCursingCards.add(Cards.youngWitch);
    knownCursingCards.add(Cards.mountebank);
    knownCursingCards.add(Cards.torturer);
    knownCursingCards.add(Cards.jester);
    knownCursingCards.add(Cards.familiar);
    knownCursingCards.add(Cards.soothsayer);
    //knownCursingCards.add(Cards.followers);

    knownTrashingCards.add(Cards.chapel);
    knownTrashingCards.add(Cards.remodel);
    knownTrashingCards.add(Cards.masquerade);
    knownTrashingCards.add(Cards.steward);
    knownTrashingCards.add(Cards.tradingPost);
    knownTrashingCards.add(Cards.upgrade);
    knownTrashingCards.add(Cards.salvager);
    knownTrashingCards.add(Cards.apprentice);
    knownTrashingCards.add(Cards.transmute);
    knownTrashingCards.add(Cards.tradeRoute);
    knownTrashingCards.add(Cards.bishop);
    knownTrashingCards.add(Cards.expand);
    knownTrashingCards.add(Cards.forge);
    knownTrashingCards.add(Cards.remake);
    knownTrashingCards.add(Cards.develop);
    knownTrashingCards.add(Cards.jackOfAllTrades);
    knownTrashingCards.add(Cards.trader);
    knownTrashingCards
      .add(Cards.ambassador); // it is not actually trashing cards, but uses similar mechanism to get rid of them
    knownTrashingCards.add(Cards.altar);
    knownTrashingCards.add(Cards.count);
    knownTrashingCards.add(Cards.counterfeit);
    knownTrashingCards.add(Cards.forager);
    knownTrashingCards.add(Cards.graverobber);
    knownTrashingCards.add(Cards.junkDealer);
    knownTrashingCards.add(Cards.procession);
    knownTrashingCards.add(Cards.rats);
    knownTrashingCards.add(Cards.rebuild);
    knownTrashingCards.add(Cards.butcher);
    knownTrashingCards.add(Cards.stonemason);

    knownGood52Cards.add(Cards.wharf);
    knownGood52Cards.add(Cards.jackOfAllTrades);
    knownGood52Cards.add(Cards.ghostShip);

    for (Card c : knownActionCards) {
      knownCards.add(c);
    }
    for (Card c : specialTreasureCards) {
      knownCards.add(c);
    }
    for (Card c : specialVictoryCards) {
      knownCards.add(c);
    }

  }
}
