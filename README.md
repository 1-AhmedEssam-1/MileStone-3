# 👾 DooR DasH: Scare vs Laugh Touchdown 🚪🏃‍♂️

An advanced, data-driven, turn-based desktop application built natively using **JavaFX** and patterned after the *Monsters, Inc.* universe. In this strategic race, two monsters compete to maximize energy collection, survive dynamic board hazards, and play game-changing cards to reach Boo's Door with enough fuel to power Monstropolis.

This project serves as a comprehensive implementation of the **Model-View-Controller (MVC)** architectural design pattern, advanced Object-Oriented Programming (OOP), file I/O operations, and custom exception-handling pipelines.

---

## 🎨 User Interface & Gameplay Tour

### 1. Welcome & Character Selection Screen
When the application launches, players are greeted with an immersive start screen designed to establish the theme and allow selection of roles (Scarer vs. Laugher).

![Start Screen](images/image_cb7d09.jpg)
*Figure 1: The opening launcher interface where players initialize game parameters and assign active monster roles.*

### 2. The Main Game Board Matrix
The game board features a custom-engineered 100-cell zigzag matrix layout grid. It manages concurrent player tokens, tracks specialty grid components, and displays vibrant tile textures.

![Main Board Grid Matrix](images/image_cb7d42.jpg)
*Figure 2: The full primary game environment displaying token pathways, interactive cell matrices, and functional alignment grids.*

### 3. Unified Real-Time Stats Panel
Positioned on the right side of the screen, this column provides continuous feedback to the player. It uses locked vertical bounds and an underlying scroll pane layout to ensure elements never overlap or clip outside the viewport.

![Live Tracking Stats Sidebar](images/image_cb806e.jpg)
*Figure 3: The custom VBox sidebar displaying current monster metrics, active statuses, specific cell image previews, and the card pile deck wrapper.*

### 4. Interactive Card Deck & Rotated Pile
The card management deck features a custom graphic rotated by -90 degrees to simulate a physical draw pile, complete with an automated card-count indicator text block.

![Card Deck View](images/image_cb8087.png)
*Figure 4: Detailed view of the drawing card stack showing accurate counter tracking numbers.*

### 5. Dynamic Last Played Card Overlays
Whenever a player draws a card, a face-up preview slot seamlessly fades into view directly over the deck stack once the primary alert modal closes, ensuring the visual history is preserved.

![Last Played Preview](images/image_cb808b.png)
*Figure 5: Active overlay displaying the face-up artwork of the most recently executed gameplay card asset.*

### 6. Modal Card Action Popups
When a player lands on a card cell, an asynchronous modal popup blocks inputs, displays the specific flavor art of the drawn card, and details its environmental rule updates.

![Card Action Overlay Popup](images/image_cb80a8.png)
*Figure 6: A high-fidelity popup overlay summarizing card effects before executing UI refresh loops.*

### 7. Core Exception Warnings & Input Throttling
To satisfy stringent application uptime rules, custom input boundaries protect against double-button spamming during active animation phases and notify users of invalid moves without crashing.

![Input Protection Alert](images/image_cb8466.png)
*Figure 7: A warning notification catching user input spikes gracefully using built-in system alerts.*

### 8. Victory Celebration Screen
Once a monster successfully fulfills the dual requirements of reaching Cell 99 with $\ge 1000$ units of active energy, input pipelines lock down and a custom celebratory view overlay takes center stage.

![Victory Achievement Canvas](images/image_cb846e.jpg)
*Figure 8: The game-over overlay summarizing final leaderboard positions and providing a direct pathway back to the main launcher.*

---

## 🏗️ Architectural Overview (MVC Pattern)

The application separates concerns cleanly into three localized layers, ensuring low coupling and high maintainability:

```text
       ┌─────────────────────────────────────────────────────────┐
       │                       CONTROLLER                        │
       │                   (game.controller)                     │
       │  • Captures hotkeys (W/E)    • Throttles input spamming │
       │  • Manages animation states  • Routes engine events     │
       └────────────┬───────────────────────────────▲────────────┘
                    │                               │
            Updates │                               │ Fires User Events
            States  │                               │ & Input Clicks
                    ▼                               │
       ┌────────────────────────┐       ┌───────────┴────────────┐
       │         MODEL          │       │          VIEW          │
       │     (game.engine)      │       │       (game.view)      │
       │ • Game/Board engines   ├──────►│ • JavaFX UI Components │
       │ • CSV Data Parsers     │ Syncs │ • BoardGridPane Layout │
       │ • Exception Matrix     │ Views │ • Stats Panel VBox     │
       └────────────────────────┘       └────────────────────────┘
