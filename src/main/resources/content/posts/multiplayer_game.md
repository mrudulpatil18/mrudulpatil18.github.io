---
title: Building a Multiplayer Maze Game
date: 2025-05-14
---

# Building a Multiplayer Maze Game

I've always been curious about how socket programming works. Up until then, my experience was limited to HTTP-based connections, and I was intrigued by how low-latency games handled communication. That's when I stumbled upon sockets. Despite several attempts to grasp how they work and how to use WebSockets, I kept getting intimidated.

## Background

That's when i decided, I was going to build a multiplayer game powered by sockets. At this point, I'd become a bit too reliant on LLMs and ChatGPT for writing code or solving problems, so i set some personal ground rules:

- **No LLMs or AI generators:** Good old google was my only allowed companion.

- **Minimal Libraries:** I'd build everything from ground up, focusing on fundamentals.

### The Concept - A Race Through Shifting Mazes

After some thought and brainstorming, I landed on a game loop.
It needed to be **multiplayer, competitive and, of course, involve mazes.** The core idea was navigating from a start to an end position in *maze*-like environment, complete with misleading paths.

Initially, i struggled to visualize how two players could compete and have fun solving the same maze together, so i had to tweak the concept.

Both players would start with **randomly generated mazes of a fixed size** (say, 5x5). It would be a race to complete a set number of mazes. When a player finishes a maze, a new, larger maze (of higher dimension) would be generated for them. **The first player to clear the final maze** (let's say maze 10) **wins!**

To amplify the multiplayer feel, i added a twist: every time a player finishes a maze and a new, bigger one is generated for them, the opponent's current maze resets. This wasn't just about racing anymore; it also introduced an element of *"sabotage"* with each completion.

### Tech Stack

Professionaly, I work with **Java and Spring Boot.** So it was a natural choice server-side logic, handling multiplayer communication and maze generation. For the visual aspect, i opted for **a browser-based game** for easy access. My self-imposed rules meant I'd be using **HTML, CSS and JS-Canvas** to implement the UI.

## Implementation - Bringing the Maze to Life

### Maze Generation - The Hunt-and-Kill Approach

There are tons of maze generation algorithms out there, each with its own biases. To keep things visually appealing and minimize bias in our context, I settled on the **Hunt-and-Kill algorithm**.

Here's a quick rundown of the algorithm:

- **Choose a random cell** in the grid.

- **Walk Phase:** From the current cell, randomly select an unvisited neighbor and move to it. Repeat this process until you reach a cell with no unvisited neighbors.

- **Hunt Phase:** Find a cell in the grid that has at least one unvisited neighbor. Connect this "hunted" cell to one of its visited neighbors.

- The walk phase repeats from this newly connected cell. This entire process continues until no more unvisited cells can be found in hunt phase, meaning the maze is complete.

In my current implementation, I search for the longest path from the start point in the generated maze to determine the target cell.

Here's a snippet of maze generation logic:

```
Maze generateMaze(int width, int height, Coordinate startCoordinate) {
        Coordinate currentCell = startCoordinate;
        Set<Coordinate> visited = new HashSet<>();
        Set<Coordinate> nonVisited = initializeNonVisited(width, height);
        Maze maze = new Maze(width, height);

        
        *if*(startCoordinate == null){
            currentCell = getRandomCell(width, height);
        }

        // Start from random cell
        maze.setStart(currentCell);
        visited.add(currentCell);
        nonVisited.remove(currentCell);

        while (!nonVisited.isEmpty()) {
            // Walk phase - keep walking until no unvisited neighbors
            while (true) {
                List<Coordinate> unvisitedNeighbors = getUnvisitedNeighbors(maze, currentCell, visited);
                if (unvisitedNeighbors.isEmpty()) {
                    break;
                }
                
                Coordinate nextCell = getRandomElement(unvisitedNeighbors);
                maze.carveEdge(currentCell, nextCell);
                visited.add(nextCell);
                nonVisited.remove(nextCell);
                currentCell = nextCell;
            }

            // Hunt phase - find an unvisited cell with at least one visited neighbor
            Coordinate huntResult = hunt(maze, nonVisited, visited);
            if (huntResult == null) {
                break; // No valid cells found during hunt - maze is complete
            }
            
            // Connect the hunted cell to a random visited neighbor

            List<Coordinate> visitedNeighbors = getVisitedNeighbors(maze, huntResult, visited);
            Coordinate connectTo = getRandomElement(visitedNeighbors);
            maze.carveEdge(huntResult, connectTo);
            
            visited.add(huntResult);
            nonVisited.remove(huntResult);
            currentCell = huntResult;
        }
        maze.setEnd(getFarthestCellCoordinates(maze, width, height));
        return maze;
    }
```

Awesome! Now that we can generate mazes of any size, let's get to the core of this project: **socket communication**.

### Sockets: multiplayer holy grail

I decided to utilize WebSockets, conveniently available through The Spring Framework.

First, we need to setup a configuration bean with `@EnableWebSocket` which implements `WebSocketConfigurer` interface. This interface is key for registering `WebSocketHandler` with necessary path mapping.

```
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    // inject WebSocketHandler bean

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/websocket").setAllowedOrigins("*");
    }
}
```

Next, we also need to setup a `WebSocketHandler` Spring bean using `WebSocketHandler` Interface.This interface provids several useful methods:

- `afterConnectionEstablished(WebSocketSession session)`: Called after a new WebSocket session has been established.

- `handleTextMessage(WebSocketSession session, TextMessage message)`: Handles incoming text messages.

- `handleBinaryMessage(WebSocketSession session, BinaryMessage message)`: Handles binary messages. (not required as we'll prefer text-based communication )

- `afterConnectionClosed(WebSocketSession session, CloseStatus status)`: Invoked after a WebSocket session has been closed.



I wanted to implement a room creation and joining, much like popular web-based multiplayer games such as SmashKarts, AmongUs and Scribble. To achieve this, we create rooms with unique 5-digit UUID-based codes and assign socket connections to the room.

- **On CREATE Room:** A new room is created and the player's socket connection is added as first player using `afterConnectionEstablished`. A unique room code is returned to the client.

- **On JOIN Room:** The room is joined based on entered code and socket connection is added as the second player using `afterConnectionEstablished`.


`handleTextMessage` is crucial for communicating game states between frontend and the server. Changes like player moves are sent to the backend for verification. Upon completion of maze or Game start, generated mazes are serialized into JSON and sent back to frontend.

Finally, when a player leaves the lobby or room, `afterConnectionClosed` gracefully closes the socket connection, and the room is removed from memory.


*For a detailed look at the implementation, take a look at the source code at [GitHub](link)*.

### Visuals: From 2D to Isometric Magic

I built the initial game loop using TypeScript and Canvas. My first thought was a simple 2D maze, and I got something like this working:

< img >

I was pretty happy with it, but I really wanted to bring a 3D element to the game to make it feel more alive and aesthetically pleasing. While scrolling through Google Images for inspiration, an isometric view maze caught my eye. Visualizing paths in an isometric style would add another dimension to my game while looking incredibly cool.

I dove down a rabbit hole to understand the math behind isometric games and how to implement it. I then created isometric (3D-like) visuals for the maze by building isometric 3D blocks and arranging them to form the maze.

However, I ran into a challenge: my grids were "thin-walled." This means my backend maze representation only tracked walkable cells. For an isometric view, I needed to convert these to "thick-walled" mazes, where walls are explicitly made of blocks. I faced a dilemma: should I convert my backend to understand and generate thick-walled mazes, or keep it as a frontend-only visualization? The problem was that player moves in a thick-walled maze would mean something different in a thin-walled one, effectively doubling the grid size.

Ultimately, I decided to create a layer in the frontend to **convert coordinates from the thick-walled (visual) version to the thin-walled (backend) version** before communicating with the server. I battled a bunch of wacky bugs and issues during this process, but finally got it working!

![img](link)


Everything was working, but I still wasn't completely satisfied. The game felt competitive, it worked, and it looked good in 3D. What it needed was a cool fantasy wrapper. I was deep in unfamiliar waters, having never worked with Canvas, sockets, or isometric games before.

Scrolling through more isometric games for inspiration, I loved how some were decorated visually with assets and tiles, creatively using isometric layers. After scouring the internet, I found these beautiful **isometric assets** [https://www.google.com/search?q=link].

To use them, I had to change a ton of my implementation to utilize tiles instead of cubes for creating the maze and its surrounding environment. The start cell became a **MAGE/WIZARD**, and the goal was to reach a towering structure.

Another tough challenge was keeping the map centered on the screen and ensuring the entire maze was visible at all times. After a lot of trial and error and some math, I ended up with a decent camera implementation to handle it. I initially planned to allow zooming in and out and moving around the tilemap with a mouse, but I decided to scrap that for now.

I even added a few animations for fun (though, full disclosure, they’re still a bit buggy!).

Here's how it looks now:

![img](link)


### Lore: Welcome to MoonRift!

With all these cool, game-like visuals, it was absolutely essential (in my eyes) for the game to have some awesome lore.

Introducing **MoonRift**

> The universe is collapsing. Only one world can survive, powered by ancient Moon Towers linked to a dying moon.
> Two rival Moon Mages race through shifting mazes to reach their towers. Each time one powers up, the other weakens.
> First to Level 10 saves their world. The other is lost to darkness.


### Lessons Learned: The Journey was the reward

After a lot of cleaning up and bug fixes, I finally deployed the project. I learned so many things throughout this adventure, from sockets and Canvas to communication protocols. But honestly, I got pretty tired towards the end. Why? **Building the UI**. Working on the visual part of the game ended up being far more time-consuming than the multiplayer backend, which was my initial focus.

To be clear, it was still absolutely worth it and a ton of fun!

The game's current state is "playable." It definitely needs more improvements to be polished, but I'm done with it for now. I'd love to revisit it later and make it even better. If you, the reader, want to add something or fix a bug, feel free to fork and contribute to the project however you like!

I didn't go into too much detail about most of the technical implementation in this post, and while I made the development seem linear for simplicity, much of the backend and UI work happened in parallel. A tremendous amount of effort went into coding this game from scratch in Canvas without any libraries or prior experience.

I hope you had fun reading this and maybe even found some inspiration from my rookie self!

Cheers,
Mrudul
