# 👾 DooR DasH: Scare vs Laugh Touchdown 🚪🏃‍♂️

An advanced, data-driven, turn-based desktop strategy board game engineered natively using the **JavaFX** ecosystem. Set in the corporate world of *Monsters, Inc.*, **DooR DasH** simulates the energy rivalry between traditional **Scarers** (who collect raw child scream power) and revolutionary **Laughers** (who generate ten times more energy via comedy). 

Two players race across a dangerous 100-cell grid matrix to reach **Boo's Door (Cell 99)**. To claim absolute victory, a monster must successfully cross into the final cell while satisfying a crucial corporate requirement: holding an accumulated canister capacity of **$\ge 1000$ units of active energy**.

This repository serves as a professional, production-grade implementation of the **Model-View-Controller (MVC)** architectural design pattern, advanced Object-Oriented Programming (OOP) traits, robust stream-based file I/O operations, and an enterprise-grade custom exception containment framework.

---

## 📸 Architectural Walkthrough & Graphical Tour

### 1. Game Initialization & Strategic Configuration
Upon application launch, the entry view serves as the operational gateway to the session, establishing the thematic aesthetic, handling character registration parameters, and providing players with access to distinct corporate alignments before initializing the primary layout.

![Main Menu](images/mainmenu.jpg)
*Figure 1: Immersive landing window presenting character class parameters and role selection pathways for Scarers and Laughers.*

### 2. The Serpentine Grid Floor Arena
The central game environment constructs an automated 100-tile serpentine grid arrangement. It dynamically tracks concurrent player tokens, calculates coordinate path shifts, and manages interactive tile assets.

![Board View](images/board%20view.jpg)
*Figure 2: Primary gameplay canvas highlighting the 10-column zig-zag layout mapping, character tokens, and environmental hazards.*

### 3. Unified Real-Time Performance Sidebar
Positioned on the right side of the screen, this performance workspace tracks continuous user metrics. It operates under fixed sizing constraints inside a non-collapsing viewport tree to guarantee progress bar updates and card deck elements remain stable at any window scale.

![Stats Preview](images/stats%20preview.png)
*Figure 3: Sidebar control module monitoring live energy tracking bars, active condition counters, and selected tile properties.*

### 4. Asynchronous Modal Action Card Popups
Landing on specific tiles pulls tactical action cards from the deck layer. The game controller instantly caps background canvas interactions, raising a smooth modal popup box explaining state modifications before allowing players to advance the turn.

![Card Drawn Popup](images/carddrawn.png)
*Figure 4: Standalone popup card window intercepting execution tracks to declare rule transformations, such as position swaps.*

### 5. Championship Victory Splash Dashboard
When a monster crosses into Cell 99 while fulfilling the minimum 1,000-energy prerequisite, input handlers freeze and a victory dashboard takes focus, offering terminal scores and a clear callback route back to the main menu.

![Winning Screen](images/winning.jpg)
*Figure 5: High-impact post-game overlay announcing the winner, detailing final canister counts, and resetting the game loop.*

---

## 🏗️ Structural Software Architecture (MVC Model)

To maximize scalability, decoupling, and automated testing across milestones, the system divides core concerns into three independent layers. The backend mechanics calculate game states with zero awareness of graphic rendering nodes, while the frontend visual panes query states via explicit data properties.

```text
       ┌─────────────────────────────────────────────────────────┐
       │                       CONTROLLER                        │
       │                   (game.controller)                     │
       │  • Regulates animation cycles  • Intercepts dev hotkeys │
       │  • Manages input-spam blocks  • Directs view updates    │
       └────────────┬───────────────────────────────▲────────────┘
                    │                               │
    Invokes Updates │                               │ Forwards User Events
    & State Changes │                               │ & Grid Clicks
                    ▼                               │
       ┌────────────────────────┐       ┌───────────┴────────────┐
       │         MODEL          │       │          VIEW          │
       │     (game.engine)      │       │       (game.view)      │
       │ • Invariant State Data │       │ • Custom JavaFX Tree   │
       │ • CSV Map Streamers    ├──────►│ • Stable VBox Sidebar  │
       │ • Rule Verification    │ Syncs │ • Matrix GridPane      │
       └────────────────────────┘ State └────────────────────────┘
