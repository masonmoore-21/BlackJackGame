import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class BlackJack {
    // Boolean variable which is true until player wants to quit
    public boolean keepPlaying = true;

    private List<String> deck;
    private final List<String> playerHand;
    private final List<String> dealerHand;
    private final Scanner scanner;

    public BlackJack() {
        scanner = new Scanner(System.in);
        playerHand = new ArrayList<>();
        dealerHand = new ArrayList<>();
    }

    /**
     * Runs the main game loop - calling other functions as needed.
     */
    public void run() {
        System.out.println("  ");
        System.out.println("  Welcome to Black Jack!");

        while (keepPlaying) {
            playRound();
            checkExit();
        }

        System.out.println("Thanks for playing! Goodbye.");
        scanner.close();
    }

    /**
     * Plays a single round of blackjack.
     */
    public void playRound() {
        // Set up fresh deck and clear hands
        buildDeck();
        shuffleDeck();
        playerHand.clear();
        dealerHand.clear();

        // Deal 2 cards each
        playerHand.add(drawCard());
        dealerHand.add(drawCard());
        playerHand.add(drawCard());
        dealerHand.add(drawCard());

        // Show initial hands
        System.out.println("\n--- New Round ---");
        System.out.println("Dealer shows: " + dealerHand.getFirst() + " and [hidden]");
        System.out.println("Your hand:    " + playerHand + " = " + handValue(playerHand));

        // Check for player blackjack
        if (handValue(playerHand) == 21) {
            System.out.println("BLACKJACK! You win!");
            return;
        }

        // Player's turn
        boolean playerBust = playerTurn();

        // Dealer's turn (only if player didn't bust)
        if (!playerBust) {
            dealerTurn();
        }
    }

    /**
     * Handles the player's turn. Returns true if player busts.
     */
    private boolean playerTurn() {
        while (true) {
            System.out.print("\nHit or Stand? (h/s): ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("h")) {
                playerHand.add(drawCard());
                int value = handValue(playerHand);
                System.out.println("Your hand: " + playerHand + " = " + value);

                if (value > 21) {
                    System.out.println("Bust! You went over 21. Dealer wins.");
                    return true;
                } else if (value == 21) {
                    System.out.println("21! Standing automatically.");
                    return false;
                }
            } else if (input.equals("s")) {
                System.out.println("You stand with: " + playerHand + " = " + handValue(playerHand));
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
        System.out.println("Dealer reveals: " + dealerHand + " = " + handValue(dealerHand));

        // Dealer hits until 17 or higher
        while (handValue(dealerHand) < 17) {
            dealerHand.add(drawCard());
            System.out.println("Dealer hits: " + dealerHand + " = " + handValue(dealerHand));
        }

        int playerScore = handValue(playerHand);
        int dealerScore = handValue(dealerHand);

        System.out.println("\n--- Result ---");
        System.out.println("Your score:   " + playerScore);
        System.out.println("Dealer score: " + dealerScore);

        if (dealerScore > 21) {
            System.out.println("Dealer busts! You win!");
        } else if (playerScore > dealerScore) {
            System.out.println("You win!");
        } else if (dealerScore > playerScore) {
            System.out.println("Dealer wins.");
        } else {
            System.out.println("Push!");
        }
    }

    /**
     * Calculates the best value of a hand, treating Aces as 11 or 1.
     */
    public int handValue(List<String> hand) {
        int total = 0;
        int aces = 0;

        for (String card : hand) {
            String rank = card.replaceAll("[♠♥♦♣]", "").trim();
            switch (rank) {
                case "J": case "Q": case "K":
                    total += 10;
                    break;
                case "A":
                    total += 11;
                    aces++;
                    break;
                default:
                    total += Integer.parseInt(rank);
            }
        }

        // Convert Aces from 11 to 1 as needed to avoid bust
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }

        return total;
    }

    /**
     * Builds a standard 52-card deck.
     */
    public void buildDeck() {
        deck = new ArrayList<>();
        String[] suits = {"♠", "♥", "♦", "♣"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};

        for (String suit : suits) {
            for (String rank : ranks) {
                deck.add(rank + suit);
            }
        }
    }

    /**
     * Shuffles the deck.
     */
    public void shuffleDeck() {
        Collections.shuffle(deck);
    }

    /**
     * Draws the top card from the deck.
     */
    public String drawCard() {
        return deck.removeFirst();
    }

    /**
     * Asks the player if they want to exit or keep playing.
     * If they want to exit - change keepPlaying flag (variable) to false.
     */
    public void checkExit() {
        System.out.print("\nPlay again? (y/n): ");
        String input = scanner.nextLine().trim().toLowerCase();
        if (!input.equals("y")) {
            keepPlaying = false;
        }
    }

    public static void main(String[] args) {
        BlackJack game = new BlackJack();
        game.run();
    }
}
