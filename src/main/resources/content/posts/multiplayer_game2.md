---
title: My First Blog Post
date: 2024-12-31
description: A deep dive into building a markdown parser
---

# placeholder title

I have always been a little curious on how socket programming works. I had only worked with HTTP based connections till this point. I was intrigued by how low latency games handle communcation. Thats when i read about sockets. Many attempts to understand how they work and how to use websockets went in vain as i got intimated.

## Background

That's when i decided, I am going to make a multiplayer game based on sockets. Now this is a time, when i had become too dependent on LLMs and ChatGPT for writing code or solving problems.

So i set a few rules for myself.
1. No using ChatGpt or LLM generators, can use good old google search for help
2. Using minimal libraries, try to write from fundamentals.


### The Concept

With a bit of thought and brainstorming, I came up with a game loop.
It had to involve multiplayer, competitiveness and Mazes. Mazes would involve going from a start position to end position in a *mazelike* environment, with multiple misleading paths at regular intervals.
I couldn't visualise how multiple players ( lets keep it at 2 players for simplicity. ) can compete and have fun trying to solve the same maze together.

So, I decided, both players would start with mazes (randomly generated) of a fixed size (say 5 ). It would be a race to finish a fixed number of mazes. When a player completes a maze, a maze grid of higher dimension is generated. The first person to finish the final maze (say 10) **wins**.

To keep the multiplayer feel alive, i added a twist. Everytime a player finishes a maze and a new bigger maze is generated, the maze of the opponent player resets. Now this should add a multiplayer feel. Now its not just a a race to the end, but also involves sabotaging the opponent on each completion.

### Tech Stack

My proffesional work involved working with Java and Spring Boot. So i went ahead with Java for the server side logic implentation to handle multiplayer logic and maze generation. For the visual part of the game, i decided to go with browser based game for easy access. My self-established rules obliged me to use HTML, CSS and JS-Canvas to implement the UI.

## Implementation

### Maze Generation

There are multiple maze generation algorithms available on the internet. They have their own biases. Hunt-and-Kill algorithm was decided to keep bais minimal with our context and generate visually asthetic mazes.


Algorithm:

Choose a random point in the grid as the start cell.

- Walk Phase:
  From the start cell, random neighour is chosen as next cell and this process repeats until a cell is reached with no unvisited neighbours.

- Hunt Phase:
  Choose a cell in the grid with atleast one unvisited neighbour. Connect the cell and an univited neighbour to this cell.

The walk phase repeats from the chosen cell from hunt cell. This process is repeated until, no more univisted cells are found in hunt phase.

In current implementation, the longest path from the start point in the maze is searched to decide the finish cell in the maze.

Code:

```
Maze generateMaze(int width, int height, Coordinate startCoordinate) {
        Coordinate currentCell = startCoordinate;
        Set<Coordinate> visited = new HashSet<>();
        Set<Coordinate> nonVisited = initializeNonVisited(width, height);
        Maze maze = new Maze(width, height);

        
        if(startCoordinate == null){
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

Great, we can now generate mazes on any size. Lets look at the aim of the project, to tackle socket communications.

### Sockets

I decided to utlize WebSockets made available by spring framework.

We need to setup configuration bean with `@EnableWebSocket` which implements `WebSocketConfigurer` interface. This interface helps register webSocketHandlers with required path mapping.

Code:
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

We also need to setup a `WebSocketHandler` spring bean using `WebSocketHandler` Interface.

Methods provided by interface

- `afterConnectionEstablished(WebSocketSession session)`: This method is called after a new WebSocket session has been established.

- `handleTextMessage(WebSocketSession session, TextMessage message)`: This method handles incoming text messages.

- `handleBinaryMessage(WebSocketSession session, BinaryMessage message)`: This method handles binary messages.( NOT Required as we'll be using text based communication )

- `afterConnectionClosed(WebSocketSession session, CloseStatus status)`: This method is invoked after a WebSocket session has been closed.



I wanted to implement a room creation and joining logic like how popular web-based multiplayer games like SmashKarts, AmongUs and Scribble handle it. To achieve that, we create rooms with uuid based unique 5 digit codes and assign socket conenctions to the room.

- On CREATE Room -> a new room is created and the player's socket connection is added to the room as first player using `afterConnectionEstablished` and a unique room code is returned.

- On JOIN Room -> the room is joined based on code entered and socket connection is added to the room as second player using `afterConnectionEstablished`


`handleTextMessage` is utilised to communicate states between frontend and this server. Changes like player moves are sent and are verified by backend, and on completion of mazes or game start, generated mazes are communicated back to frontend by serializing and deserialing to JSON.

When a player leaves the lobby / room, `afterConnectionClosed` closes the opponents socket connection and deletes the room from memory.


*for a detailed view at implementation , take take a look at the source code at [github](link)*


### Visuals

I established a game loop with typescript using canvas,
Initially , I had planned to make a simple 2d maze, and this is what i had achieved.

< img >

I was pretty happy with what i had, but i wannted to implement 3D somehow to the game,to make it more alive and asthetic. While scrolling through google images for inspiration, i saw an isometric view maze and it caught my attention. Visualising paths in isometric would add another dimension to my game, while making it look good at the same time.

I went in a rabit hole to understand the maths behind isometric games and how i would use it. I made a isometric based ( 3d-like ) visuals to the maze by creating isometric 3d blocks and laying them up in the maze form.

I did have a challenge in the process though...  My grids were thin walled. what that means is, in a grid representation, i used to represent only the cells which are walkable. for isometric view i had to convert my thin wallled mazes to thick walled (walls are made up of blocks ). I faced a dilemma, should i conevert my backend to work with, generate and understand thick walled mazes or do i keep in the frontend only for visualisation. But it changed things as moves made by player in thick walled maze would mean different thing in thin walled as it is effectively twice the grid size.

I decided to create a layer in the frontend to conevert coordinates from thick walled to thin walled version before communicating with backend. I faced a bunch of wacky bugs and issues in this process, but finally got it working.

![img](link)


Everthing works now, i was still not satisfied, it feels competitive, it works and it looks 3d and good. I wanted to wrap it up in some sort of cool fantasy game. I was deep in unfamiliar waters, never having worked with canvas or sockets or isometric games for that matter..

Scrolling through isometric games for inspiration i really liked how some games decorating it visually with help of assests and tiles and creatively using isometric layers. On scouring the internet i found these beautiful isometric (assests)[link].

To use it , had to change a ton of implementation to utilize tiles insted of cubes to create a maze and the surroudning environment, the start cell was a MAGE / WIZARD now , and the goal was to reach the tower.

Another tough challenge in this process was to keep the map in the center of screen and make sure all of the maze is visible at a time. After a lot of failing and maths, i ended up with a decent camera implementation to deal with it. I did plan to allow zooming in and out and moving around the tilemap with a mouse, but discarded it later.

I also added a bit of animations for fun. ( PS. they are still buggy )

Here's how it looks now:

![img](link)


### Lore

With all the cool game-like visuals, its absolutely essential (in my eyes ) for the game to have a cool lore.

Introducing **MoonRift**

> The universe is collapsing. Only one world can survive, powered by ancient Moon Towers linked to a dying moon.
> Two rival Moon Mages race through shifting mazes to reach their towers. Each time one powers up, the other weakens.
> First to Level 10 saves their world. The other is lost to darkness.


### Learning

Lots of cleaning up and bug fixes later. I deployed the project. I learnt lot many things through the adventure. From sockets, to canvas to communication protocols etc. But the truth is I got tired towards the end. Why ? Writing the UI. Working on the visual part of the game was much much more and time consuming than the multiplayer backend, which was intented goal anyways.

To be clear, it was still absolutely worth it and lots of fun.

The current state of game is - *playable*. Lots of improvements are required to make it polished. But i am done with it for now. I would  love to visit it later and improve it. If you as viewer want to add or fix somthing, feel free to fork and contribute to the project however you want.

I did not go into too much detail about most of the technical implementation for this post. Also most of the developement of backend and ui happened parallely, i have made it seem linear for simplicity of understanding. Lots and lots of work went into the coding the game from strach in canvas without any libraries or past experience.

I hope you had fun reading this and get inspired from my rookie-self.

Cheers,
Mrudul



# placeholder title

I have always been a little curious on how socket programming works. I had only worked with HTTP based connections till this point. I was intrigued by how low latency games handle communcation. Thats when i read about sockets. Many attempts to understand how they work and how to use websockets went in vain as i got intimated.

## Background

That's when i decided, I am going to make a multiplayer game based on sockets. Now this is a time, when i had become too dependent on LLMs and ChatGPT for writing code or solving problems.

So i set a few rules for myself.
1. No using ChatGpt or LLM generators, can use good old google search for help
2. Using minimal libraries, try to write from fundamentals.


### The Concept

With a bit of thought and brainstorming, I came up with a game loop.
It had to involve multiplayer, competitiveness and Mazes. Mazes would involve going from a start position to end position in a *mazelike* environment, with multiple misleading paths at regular intervals.
I couldn't visualise how multiple players ( lets keep it at 2 players for simplicity. ) can compete and have fun trying to solve the same maze together.

So, I decided, both players would start with mazes (randomly generated) of a fixed size (say 5 ). It would be a race to finish a fixed number of mazes. When a player completes a maze, a maze grid of higher dimension is generated. The first person to finish the final maze (say 10) **wins**.

To keep the multiplayer feel alive, i added a twist. Everytime a player finishes a maze and a new bigger maze is generated, the maze of the opponent player resets. Now this should add a multiplayer feel. Now its not just a a race to the end, but also involves sabotaging the opponent on each completion.

### Tech Stack

My proffesional work involved working with Java and Spring Boot. So i went ahead with Java for the server side logic implentation to handle multiplayer logic and maze generation. For the visual part of the game, i decided to go with browser based game for easy access. My self-established rules obliged me to use HTML, CSS and JS-Canvas to implement the UI.

## Implementation

### Maze Generation

There are multiple maze generation algorithms available on the internet. They have their own biases. Hunt-and-Kill algorithm was decided to keep bais minimal with our context and generate visually asthetic mazes.


Algorithm:

Choose a random point in the grid as the start cell.

- Walk Phase:
  From the start cell, random neighour is chosen as next cell and this process repeats until a cell is reached with no unvisited neighbours.

- Hunt Phase:
  Choose a cell in the grid with atleast one unvisited neighbour. Connect the cell and an univited neighbour to this cell.

The walk phase repeats from the chosen cell from hunt cell. This process is repeated until, no more univisted cells are found in hunt phase.

In current implementation, the longest path from the start point in the maze is searched to decide the finish cell in the maze.

Code:

```
Maze generateMaze(int width, int height, Coordinate startCoordinate) {
        Coordinate currentCell = startCoordinate;
        Set<Coordinate> visited = new HashSet<>();
        Set<Coordinate> nonVisited = initializeNonVisited(width, height);
        Maze maze = new Maze(width, height);

        
        if(startCoordinate == null){
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

Great, we can now generate mazes on any size. Lets look at the aim of the project, to tackle socket communications.

### Sockets

I decided to utlize WebSockets made available by spring framework.

We need to setup configuration bean with `@EnableWebSocket` which implements `WebSocketConfigurer` interface. This interface helps register webSocketHandlers with required path mapping.

Code:
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

We also need to setup a `WebSocketHandler` spring bean using `WebSocketHandler` Interface.

Methods provided by interface

- `afterConnectionEstablished(WebSocketSession session)`: This method is called after a new WebSocket session has been established.

- `handleTextMessage(WebSocketSession session, TextMessage message)`: This method handles incoming text messages.

- `handleBinaryMessage(WebSocketSession session, BinaryMessage message)`: This method handles binary messages.( NOT Required as we'll be using text based communication )

- `afterConnectionClosed(WebSocketSession session, CloseStatus status)`: This method is invoked after a WebSocket session has been closed.



I wanted to implement a room creation and joining logic like how popular web-based multiplayer games like SmashKarts, AmongUs and Scribble handle it. To achieve that, we create rooms with uuid based unique 5 digit codes and assign socket conenctions to the room.

- On CREATE Room -> a new room is created and the player's socket connection is added to the room as first player using `afterConnectionEstablished` and a unique room code is returned.

- On JOIN Room -> the room is joined based on code entered and socket connection is added to the room as second player using `afterConnectionEstablished`


`handleTextMessage` is utilised to communicate states between frontend and this server. Changes like player moves are sent and are verified by backend, and on completion of mazes or game start, generated mazes are communicated back to frontend by serializing and deserialing to JSON.

When a player leaves the lobby / room, `afterConnectionClosed` closes the opponents socket connection and deletes the room from memory.


*for a detailed view at implementation , take take a look at the source code at [github](link)*


### Visuals

I established a game loop with typescript using canvas,
Initially , I had planned to make a simple 2d maze, and this is what i had achieved.

< img >

I was pretty happy with what i had, but i wannted to implement 3D somehow to the game,to make it more alive and asthetic. While scrolling through google images for inspiration, i saw an isometric view maze and it caught my attention. Visualising paths in isometric would add another dimension to my game, while making it look good at the same time.

I went in a rabit hole to understand the maths behind isometric games and how i would use it. I made a isometric based ( 3d-like ) visuals to the maze by creating isometric 3d blocks and laying them up in the maze form.

I did have a challenge in the process though...  My grids were thin walled. what that means is, in a grid representation, i used to represent only the cells which are walkable. for isometric view i had to convert my thin wallled mazes to thick walled (walls are made up of blocks ). I faced a dilemma, should i conevert my backend to work with, generate and understand thick walled mazes or do i keep in the frontend only for visualisation. But it changed things as moves made by player in thick walled maze would mean different thing in thin walled as it is effectively twice the grid size.

I decided to create a layer in the frontend to conevert coordinates from thick walled to thin walled version before communicating with backend. I faced a bunch of wacky bugs and issues in this process, but finally got it working.

![img](link)


Everthing works now, i was still not satisfied, it feels competitive, it works and it looks 3d and good. I wanted to wrap it up in some sort of cool fantasy game. I was deep in unfamiliar waters, never having worked with canvas or sockets or isometric games for that matter..

Scrolling through isometric games for inspiration i really liked how some games decorating it visually with help of assests and tiles and creatively using isometric layers. On scouring the internet i found these beautiful isometric (assests)[link].

To use it , had to change a ton of implementation to utilize tiles insted of cubes to create a maze and the surroudning environment, the start cell was a MAGE / WIZARD now , and the goal was to reach the tower.

Another tough challenge in this process was to keep the map in the center of screen and make sure all of the maze is visible at a time. After a lot of failing and maths, i ended up with a decent camera implementation to deal with it. I did plan to allow zooming in and out and moving around the tilemap with a mouse, but discarded it later.

I also added a bit of animations for fun. ( PS. they are still buggy )

Here's how it looks now:

![img](link)


### Lore

With all the cool game-like visuals, its absolutely essential (in my eyes ) for the game to have a cool lore.

Introducing **MoonRift**

> The universe is collapsing. Only one world can survive, powered by ancient Moon Towers linked to a dying moon.
> Two rival Moon Mages race through shifting mazes to reach their towers. Each time one powers up, the other weakens.
> First to Level 10 saves their world. The other is lost to darkness.


### Learning

Lots of cleaning up and bug fixes later. I deployed the project. I learnt lot many things through the adventure. From sockets, to canvas to communication protocols etc. But the truth is I got tired towards the end. Why ? Writing the UI. Working on the visual part of the game was much much more and time consuming than the multiplayer backend, which was intented goal anyways.

To be clear, it was still absolutely worth it and lots of fun.

The current state of game is - *playable*. Lots of improvements are required to make it polished. But i am done with it for now. I would  love to visit it later and improve it. If you as viewer want to add or fix somthing, feel free to fork and contribute to the project however you want.

I did not go into too much detail about most of the technical implementation for this post. Also most of the developement of backend and ui happened parallely, i have made it seem linear for simplicity of understanding. Lots and lots of work went into the coding the game from strach in canvas without any libraries or past experience.

I hope you had fun reading this and get inspired from my rookie-self.

Cheers,
Mrudul



