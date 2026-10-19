import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Entry point for the game. Everything below (Card, Deck, Hand,
 * Participant, Player, Dealer, BlackJackGame) lives in this same file
 * so it can be run as a single .java file, but the classes themselves
 * are unchanged from the multi-file version - this just packages them
 * together.
 */
public class BlackJack {
    public static void main(String[] args) {
        // Force UTF-8 output so suit symbols (♠ ♥ ♦ ♣) render correctly
        // regardless of the host platform's default console encoding.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        BlackJackGame game = new BlackJackGame();
        game.run();
    }
}

/**
 * Represents a single playing card with a rank, suit, and blackjack value.
 */
class Card {
    private final String rank;
    private final String suit;
    private final int value;

    public Card(String rank, String suit, int value) {
        this.rank = rank;
        this.suit = suit;
        this.value = value;
    }

    public String getRank() {
        return rank;
    }

    public String getSuit() {
        return suit;
    }

    /**
     * Base blackjack value of the card. Aces are valued at 11 here;
     * Hand is responsible for demoting Aces to 1 when needed.
     */
    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return rank + suit;
    }
}

/**
 * A standard 52-card deck that can be built, shuffled, and drawn from.
 */
class Deck {
    private List<Card> cards;

    public Deck() {
        cards = freshDeck();
    }

    /**
     * Builds (or rebuilds) a fresh, standard 52-card deck.
     */
    public void buildDeck() {
        cards = freshDeck();
    }

    private static List<Card> freshDeck() {
        List<Card> freshCards = new ArrayList<>();
        String[] suits = {"\u2660", "\u2665", "\u2666", "\u2663"}; // ♠ ♥ ♦ ♣
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};

        for (String suit : suits) {
            for (String rank : ranks) {
                freshCards.add(new Card(rank, suit, rankValue(rank)));
            }
        }
        return freshCards;
    }

    /**
     * Shuffles the deck in place.
     */
    public void shuffle() {
        Collections.shuffle(cards);
    }

    /**
     * Draws (removes and returns) the top card of the deck.
     * Rebuilds and reshuffles automatically if the deck runs out.
     */
    public Card drawCard() {
        if (isEmpty()) {
            buildDeck();
            shuffle();
        }
        return cards.removeFirst();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    private static int rankValue(String rank) {
        return switch (rank) {
            case "J", "Q", "K" -> 10;
            case "A" -> 11;
            default -> Integer.parseInt(rank);
        };
    }
}

/**
 * A hand of cards belonging to a Participant (Player or Dealer).
 * Knows how to calculate its own best blackjack value, treating
 * Aces as 11 or 1 as needed to avoid busting.
 */
class Hand {
    private final List<Card> cards;

    public Hand() {
        cards = new ArrayList<>();
    }

    public void addCard(Card card) {
        cards.add(card);
    }

    public void clear() {
        cards.clear();
    }

    public List<Card> getCards() {
        return cards;
    }

    /**
     * Calculates the best possible value of the hand (<= 21 if achievable),
     * treating each Ace as 11 unless that would bust the hand.
     */
    public int calculateValue() {
        int total = 0;
        int aces = 0;

        for (Card card : cards) {
            total += card.getValue();
            if (card.getRank().equals("A")) {
                aces++;
            }
        }

        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }

        return total;
    }

    public boolean isBust() {
        return calculateValue() > 21;
    }

    public boolean isBlackjack() {
        return cards.size() == 2 && calculateValue() == 21;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cards.size(); i++) {
            sb.append(cards.get(i));
            if (i < cards.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}

/**
 * Abstract superclass for anyone who holds a hand of cards in the game
 * (a human Player or the Dealer). Shared state and behavior live here;
 * Player and Dealer add their own specialized rules.
 */
abstract class Participant {
    protected String name;
    protected Hand hand;

    public Participant(String name) {
        this.name = name;
        this.hand = new Hand();
    }

    public void addCardToHand(Card card) {
        hand.addCard(card);
    }

    public int getHandValue() {
        return hand.calculateValue();
    }

    public Hand getHand() {
        return hand;
    }

    public String getName() {
        return name;
    }

    public boolean isBust() {
        return hand.isBust();
    }

    /**
     * Full, un-hidden view of this participant's hand and value.
     */
    public String showHand() {
        return name + "'s hand: " + hand + " = " + hand.calculateValue();
    }

    public void resetHand() {
        hand.clear();
    }
}

/**
 * A human player. Adds chip/bet tracking on top of the shared
 * Participant behavior (hand management, hand value, etc.).
 */
class Player extends Participant {
    private int chips;
    private int currentBet;

    public Player(String name, int chips) {
        super(name);
        this.chips = chips;
        this.currentBet = 0;
    }

    /**
     * Places a bet, deducting it from the player's chip stack.
     * Does nothing if the player cannot cover the amount.
     *
     * @return true if the bet was placed successfully
     */
    public boolean placeBet(int amount) {
        if (amount <= 0 || amount > chips) {
            return false;
        }
        currentBet = amount;
        chips -= amount;
        return true;
    }

    /**
     * Pays out winnings for the current bet (e.g. 2x on a win) and
     * resets the bet back to zero.
     */
    public void winBet(double multiplier) {
        chips += (int) Math.round(currentBet * multiplier);
        currentBet = 0;
    }

    /**
     * Clears the current bet with no payout (a loss).
     */
    public void loseBet() {
        currentBet = 0;
    }

    /**
     * Returns the current bet to the player (a push).
     */
    public void pushBet() {
        chips += currentBet;
        currentBet = 0;
    }

    public int getChips() {
        return chips;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public boolean hasBlackjack() {
        return hand.isBlackjack();
    }
}

/**
 * The dealer. Adds the "hole card" (hidden first card) behavior and
 * the fixed house rule for when to keep hitting.
 */
class Dealer extends Participant {
    private boolean hiddenCardRevealed;

    public Dealer() {
        super("Dealer");
        this.hiddenCardRevealed = false;
    }

    public void revealHiddenCard() {
        hiddenCardRevealed = true;
    }

    public boolean isHiddenCardRevealed() {
        return hiddenCardRevealed;
    }

    /**
     * House rule: the dealer must keep hitting while the hand value is
     * below 17.
     */
    public boolean shouldHit() {
        return getHandValue() < 17;
    }

    /**
     * Shows only the first card while the hole card is still hidden;
     * shows the full hand once revealed.
     */
    public String showHandHidden() {
        if (hiddenCardRevealed || hand.getCards().isEmpty()) {
            return showHand();
        }
        return name + " shows: " + hand.getCards().get(0) + " and [hidden]";
    }

    @Override
    public void resetHand() {
        super.resetHand();
        hiddenCardRevealed = false;
    }
}

/**
 * Orchestrates a game of Blackjack: owns the deck and the two
 * participants, and drives the round-by-round flow.
 */
class BlackJackGame {
    private final Player player;
    private final Dealer dealer;
    private final Deck deck;
    private boolean keepPlaying;
    private final Scanner scanner;

    public BlackJackGame() {
        scanner = new Scanner(System.in);
        deck = new Deck();
        player = new Player("Player", 100);
        dealer = new Dealer();
        keepPlaying = true;
    }

    /**
     * Runs the main game loop, calling other methods as needed.
     */
    public void run() {
        System.out.println("  ");
        System.out.println("  Welcome to Black Jack!");
        System.out.println("  You start with " + player.getChips() + " chips.\n");

        try {
            while (keepPlaying) {
                playRound();

                if (player.getChips() <= 0) {
                    System.out.println("You're out of chips. Game over.");
                    break;
                }
                checkExit();
            }
        } catch (NoSuchElementException e) {
            // Input stream closed/ran out (e.g. redirected input, closed terminal).
            System.out.println("\nNo more input detected.");
        }

        System.out.println("Thanks for playing! Goodbye.");
        scanner.close();
    }

    /**
     * Plays a single round of blackjack: betting, dealing, player
     * turn, dealer turn, and resolution.
     */
    public void playRound() {
        placeBetFromInput();

        // Fresh deck and hands for the round
        deck.buildDeck();
        deck.shuffle();
        player.resetHand();
        dealer.resetHand();

        // Deal 2 cards each
        player.addCardToHand(deck.drawCard());
        dealer.addCardToHand(deck.drawCard());
        player.addCardToHand(deck.drawCard());
        dealer.addCardToHand(deck.drawCard());

        System.out.println("\n--- New Round ---");
        System.out.println(dealer.showHandHidden());
        System.out.println(player.showHand());

        if (player.hasBlackjack()) {
            System.out.println("BLACKJACK! You win!");
            player.winBet(2.5); // 3:2 payout plus original bet back
            System.out.println("Chips: " + player.getChips());
            return;
        }

        boolean playerBust = playerTurn();

        if (!playerBust) {
            dealerTurn();
        } else {
            player.loseBet();
        }
    }

    /**
     * Asks the player how much they want to bet before the round starts.
     */
    private void placeBetFromInput() {
        while (true) {
            System.out.print("You have " + player.getChips() + " chips. Place your bet: ");
            String input = scanner.nextLine().trim();
            try {
                int amount = Integer.parseInt(input);
                if (player.placeBet(amount)) {
                    break;
                }
                System.out.println("Enter a bet between 1 and " + player.getChips() + ".");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    /**
     * Handles the player's turn. Returns true if the player busts.
     */
    private boolean playerTurn() {
        while (true) {
            System.out.print("\nHit or Stand? (h/s): ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("h")) {
                player.addCardToHand(deck.drawCard());
                int value = player.getHandValue();
                System.out.println(player.showHand());

                if (value > 21) {
                    System.out.println("Bust! You went over 21. Dealer wins.");
                    return true;
                } else if (value == 21) {
                    System.out.println("21! Standing automatically.");
                    return false;
                }
            } else if (input.equals("s")) {
                System.out.println("You stand with: " + player.getHand() + " = " + player.getHandValue());
                return false;
            } else {
                System.out.println("Please enter 'h' to hit or 's' to stand.");
            }
        }
    }

    /**
     * Handles the dealer's turn and determines the winner.
     */
    private void dealerTurn() {
        System.out.println("\n--- Dealer's Turn ---");
        dealer.revealHiddenCard();
        System.out.println(dealer.showHand());

        while (dealer.shouldHit()) {
            dealer.addCardToHand(deck.drawCard());
            System.out.println("Dealer hits: " + dealer.getHand() + " = " + dealer.getHandValue());
        }

        int playerScore = player.getHandValue();
        int dealerScore = dealer.getHandValue();

        System.out.println("\n--- Result ---");
        System.out.println("Your score:   " + playerScore);
        System.out.println("Dealer score: " + dealerScore);

        if (dealer.isBust()) {
            System.out.println("Dealer busts! You win!");
            player.winBet(2.0);
        } else if (playerScore > dealerScore) {
            System.out.println("You win!");
            player.winBet(2.0);
        } else if (dealerScore > playerScore) {
            System.out.println("Dealer wins.");
            player.loseBet();
        } else {
            System.out.println("Push!");
            player.pushBet();
        }

        System.out.println("Chips: " + player.getChips());
    }

    /**
     * Asks the player if they want to exit or keep playing.
     * If they want to exit, sets keepPlaying to false.
     */
    private void checkExit() {
        System.out.print("\nPlay again? (y/n): ");
        String input = scanner.nextLine().trim().toLowerCase();
        if (!input.equals("y")) {
            keepPlaying = false;
        }
    }
}
