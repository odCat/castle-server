# Product Requirements Document — Chess Platform

## 1. Executive Summary

A web-based chess platform enabling two players to play real-time chess online. The system consists of a **Java/Spring Boot REST API server** (with WebSocket for live updates) and a **React SPA client**. Users can register, log in, create or join chess games, spectate ongoing matches, and review their game history. A demo/analysis board is provided for free-form position exploration.

---

## 2. Product Vision

Provide a simple, accessible online chess experience where friends or casual players can:

- Play a full game of chess against another person in real time
- Browse and spectate ongoing games
- Analyze positions using a free-form demo board
- Review their finished games

The focus is on correctness of chess rules, real-time move propagation, and a clean dark-themed UI — not on AI opponents or advanced analytics.

---

## 3. Target Audience

| Persona | Description |
|---|---|
| **Casual Player** | Wants to play chess with a friend online. No account needed to spectate; account required to play. |
| **Spectator** | Browsing in-progress games to watch without interaction. |
| **Tester / Developer** | Uses the API and E2E tests to validate system behavior. |

---

## 4. Core Features

### Priority 1 (MVP)

| ID | Feature | Description |
|---|---|---|
| F1 | User Registration | Create an account with email, username, and password. Username (alphanumeric, 4–24 chars). Password (8–24 chars, must include lowercase, uppercase, digit, and special char from `!@#$%^&*()_+=-`). |
| F2 | User Login | Authenticate with username or email + password. Returns a JWT token used as a Bearer token for subsequent requests. |
| F3 | Create Game | Pick a color (white/black) and open a game for others to join. A player may only have one active game at a time. |
| F4 | Join Game | Browse open (waiting) games and join as the opponent. Game status moves to `INPROGRESS`. Both players receive a WebSocket notification. |
| F5 | Play Chess Moves | Make legal chess moves via drag-and-drop or click. Moves are validated server-side using chesslib. Valid moves are broadcast to the opponent via WebSocket in real time. |
| F6 | Spectate Games | Browse a list of all in-progress games with mini board previews. Click to view the full game board (read-only). |
| F7 | Game History | View a table of finished games for any player, sorted by date. Click to view the final position. |
| F8 | Guest Access | Browse games and spectate without logging in. |

### Priority 2

| ID | Feature | Description |
|---|---|---|
| F9 | Delete Account | Logged-in user can delete their own account. |
| F10 | Cancel Waiting Game | Player who created an open game can cancel it while waiting for an opponent. |
| F11 | Demo / Analysis Board | Free-form board with spare piece trays. Place/remove pieces arbitrarily — no server connection needed. |
| F12 | Account Settings | View current username/email/full name. Delete account button with confirmation dialog. (Full profile update via PATCH listed as future.) |

### Priority 3

| ID | Feature | Description |
|---|---|---|
| F13 | Update Profile (PATCH) | Update username, email, full name, and/or password. Returns a fresh JWT. (Backend implemented and API-tested; not wired in the UI.) |
| F14 | Pawn Promotion UI | Interactive promotion piece selection overlay when a pawn reaches the back rank. |

---

## 5. Functional Requirements

### 5.1 Registration (F1)

| Requirement | Detail |
|---|---|
| Input | `{ email, username, password, fullName? }` |
| Validation | Email must be valid format. Username: 4–24 alphanumeric chars. Password: 8–24 chars with at least one lowercase, one uppercase, one digit, and one special char (`!@#$%^&*()_+=-`). |
| Duplicates | Username and email must be unique (enforced at DB level with UNIQUE constraints). Duplicate returns `403` with an error message. |
| Output | `201 Created` on success. |

### 5.2 Login (F2)

| Requirement | Detail |
|---|---|
| Input | `{ usernameOrEmail, password }` |
| Validation | Credential field must not be blank. Password must not be blank. |
| Auth | BCrypt verification against stored hash. |
| Output | `200 OK` with `{ id, username, email, fullName, password: "<JWT>" }`. `403` on invalid credentials. |

### 5.3 Game Creation (F3)

| Requirement | Detail |
|---|---|
| Input | `{ color: "white" | "black", name: "<username>" }` |
| Constraint | Player must not already have an OPEN or INPROGRESS game. |
| Output | `201 Created` with full Game object. `403` if already in a game. |

### 5.4 Join Game (F4)

| Requirement | Detail |
|---|---|
| Input | `{ id: <gameId>, color: "white" | "black", name: "<username>" }` |
| Constraint | Joining player must not already be in a game. Game must exist and be OPEN. Color must be available. |
| Output | `201 Created` with full Game object. WebSocket broadcast to `/topic/game/{gameId}`. `403` on error. |

### 5.5 Move (F5)

| Requirement | Detail |
|---|---|
| Input | `{ color: "w" | "b", from: "e2", to: "e4", promotion?, fen: "...", pgn: "..." }` |
| Validation | Move must be legal via chesslib. Player must be a participant. Must be that player's turn. Game must not be FINISHED. |
| Output | `200 OK` with the Move object. WebSocket broadcast of `{ from, to }` to opponent. `403` on invalid move. |

### 5.6 Delete Account (F9)

| Requirement | Detail |
|---|---|
| Auth | Bearer token required. Only the authenticated user may delete their own account. |
| Endpoint | `DELETE /players?id={id}` |
| Constraint | `id` must match the authenticated user's ID (from JWT). |
| Output | `200 OK` on success. `403` if attempting to delete another user. |

### 5.7 Spectate (F6)

| Requirement | Detail |
|---|---|
| Browse | `GET /games/inprogress` returns a list of diagrams `{ id, white, black, fen }`. |
| View | `GET /games/id/{gameId}` returns full Game object incl. PGN. |
| Auth | These endpoints are public (no auth required). |

### 5.8 Game History (F7)

| Requirement | Detail |
|---|---|
| Endpoint | `GET /players/history/{playerId}` |
| Output | List of finished games (status = `FINISHED`) for the player. Public. |

---

## 6. Non-Functional Requirements

| Category | Requirement |
|---|---|
| **Security** | Passwords stored as BCrypt hashes. JWT tokens for session management. All mutating endpoints require Bearer auth. |
| **Concurrency** | SQLite is single-writer. A `busy_timeout` PRAGMA (5000ms) mitigates contention. For heavy parallelism, consider switching to H2 or PostgreSQL. |
| **Real-time** | WebSocket (STOMP over SockJS) for live move propagation. Moves broadcast to `/topic/game/{gameId}`. |
| **Validation** | Two-tier: client-side (chess.js, form validation) for instant feedback; server-side (chesslib, Bean Validation) for authority. |
| **Error Handling** | All exceptions return JSON with an `error` field. Validation errors return `400` with per-field messages. Business logic errors return `403`. |
| **CORS** | Currently hardcoded to `http://localhost:5173` (Vite dev server). Must be configurable for production. |
| **Performance** | Games list endpoints should respond in < 500ms. Move validation and broadcast in < 200ms. |
| **Testability** | API tests via Playwright. E2E tests via Playwright. Unit tests via Vitest. |

---

## 7. Technical Architecture

### 7.1 Server (`chess-server`)

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.5.7 |
| Language | Java 21 |
| Database | SQLite via JDBC + Hibernate ORM |
| Auth | Custom JWT (jjwt 0.13.0) |
| Chess Engine | chesslib 1.3.6 |
| WebSocket | Spring WebSocket + STOMP |
| Build | Maven |
| API Docs | springdoc-openapi (Swagger UI at `/swagger-ui/`) |

**Application structure:**
```
controller/    → REST endpoints (PlayerController, GameController)
service/       → Business logic (PlayerService, GameService, JwtService)
model/         → JPA entities (Player, Game)
repository/    → Spring Data JPA repositories
dto/           → Request/response objects with validation
filter/        → JwtFilter (extracts Bearer token, sets SecurityContext)
config/        → SecurityConfig, WebSocketConfig
exception/     → Custom exceptions (GameAlreadyExists, etc.)
```

### 7.2 Client (`chess-client`)

| Layer | Technology |
|---|---|
| Framework | React 19 |
| Build | Vite 7 |
| UI Library | MUI (Material UI) 7 |
| State | Redux Toolkit 2 |
| Routing | react-router 7 |
| Chess Engine | chess.js 1.4 |
| Board UI | react-chessboard 5.8 |
| WebSocket | @stomp/stompjs 7.3 |
| Testing | Vitest 4 (unit), Playwright 1.60 (E2E + API) |

**Application structure:**
```
components/
  OnlineCard    → Landing page (Guest / Login / Register)
  LoginCard     → Login form
  RegisterCard  → Registration form
  TopBar        → Navigation bar with user menu
  Settings/     → Account settings + delete
  Profile/      → Game history table
  Watch/        → Browse + spectate games
    Diagram     → Mini board preview
  Play/         → Create/join games hub
    WaitForPlayer → Waiting state
  Game/         → Active chess game (real-time)
    GameDebug   → Developer debug panel
  Demo/         → Free-form analysis board
  Copyright     → Footer
store/          → Redux actions + reducers
tests/
  api/          → Playwright API integration tests
  e2e/          → Playwright browser E2E tests
  unit/         → Vitest unit tests
  helpers/      → Test utilities
```

### 7.3 Data Flow

```
User Action → React Component → REST API → Spring Controller → Service → DB
                                    ↕
                              WebSocket Broadcast
                                    ↕
                            Opponent's React App
```

---

## 8. API Specification

All endpoints under `http://localhost:8080`.

### 8.1 Player Endpoints

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| `GET` | `/players` | JWT | — | `200` + `Player[]` |
| `POST` | `/players/register` | Anonymous | `Register` | `201` + `Register` |
| `POST` | `/players/login` | Public | `Login` | `200` + `Login` (with JWT) |
| `PATCH` | `/players?id={id}` | JWT | `Update` | `200` + `Player` / `404` |
| `DELETE` | `/players?id={id}` | JWT | `?id=` | `200` |
| `GET` | `/players/history/{id}` | Public | `{id}` | `200` + `Game[]` |

### 8.2 Game Endpoints

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| `POST` | `/games/create` | JWT | `Open` | `201` + `Game` |
| `POST` | `/games/join` | JWT | `Join` | `201` + `Game` |
| `DELETE` | `/games/id/{gameId}` | JWT | `{gameId}` | `200` |
| `GET` | `/games/inprogress` | Public | — | `200` + `Diagram[]` |
| `GET` | `/games/inprogress/player/{username}` | Public | `{username}` | `200` + `Diagram[]` |
| `GET` | `/games/open` | Public | — | `200` + `Diagram[]` |
| `GET` | `/games/id/{id}` | Public | `{id}` | `200` + `Game` |
| `POST` | `/games/{gameId}/move` | JWT | `Move` | `200` + `Move` |

### 8.3 Error Responses

All errors return JSON: `{ "error": "<message>" }`

| Condition | HTTP Status |
|---|---|
| Validation failure | `400 BAD REQUEST` |
| Invalid credentials | `403 FORBIDDEN` |
| Duplicate username/email | `403 FORBIDDEN` |
| Game already in progress | `403 FORBIDDEN` |
| Game not found | `403 FORBIDDEN` |
| Invalid move | `403 FORBIDDEN` |
| Database locked | `403 FORBIDDEN` |

---

## 9. Data Model

### 9.1 `players`

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PRIMARY KEY, AUTOINCREMENT |
| `email` | TEXT | NOT NULL, UNIQUE |
| `username` | TEXT | NOT NULL, UNIQUE |
| `password` | TEXT | NOT NULL (BCrypt hash) |
| `full_name` | TEXT | — |
| `created` | TEXT | — |
| `image` | BLOB | — |

### 9.2 `games`

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PRIMARY KEY, AUTOINCREMENT |
| `white` | TEXT | DEFAULT '' |
| `white_id` | INTEGER | DEFAULT 0 |
| `black` | TEXT | DEFAULT '' |
| `black_id` | INTEGER | DEFAULT 0 |
| `date` | TEXT | DEFAULT '' |
| `status` | TEXT | DEFAULT 'OPEN', CHECK('OPEN','INPROGRESS','FINISHED') |
| `pgn` | TEXT | DEFAULT '' |
| `fen` | TEXT | DEFAULT starting position |
| `result` | TEXT | DEFAULT '' |

### 9.3 Game Status Lifecycle

```
OPEN → (player joins) → INPROGRESS → (checkmate/draw) → FINISHED
```

---

## 10. Constraints & Assumptions

| Constraint | Detail |
|---|---|
| **SQLite single-writer** | Not suitable for high-concurrency deployments. Busy timeout mitigates test contention but is not a production solution. |
| **No OAuth / social login** | Only username/email + password authentication. |
| **CORS hardcoded** | Server CORS policy allows only `http://localhost:5173`. Must be updated for production. |
| **API URL hardcoded** | Client hardcodes `http://localhost:8080` for the server. Should be an environment variable. |
| **No mobile responsiveness** | Layout has minimum widths (700px) and fixed sizes. Not optimized for small screens. |
| **No game clock/timer** | No time controls — games are untimed. |
| **No chat** | No in-game communication between players. |
| **No AI opponent** | Only human vs. human play. No computer opponent. |
| **No move history navigation** | Previous moves cannot be browsed during a game (only final PGN visible). |
| **Demo board partially implemented** | Some interactive features (drag detection, spare piece updates) are commented out in the codebase. |

---

## 11. Future Considerations

| Feature | Notes |
|---|---|
| Production database (H2 / PostgreSQL) | Replace SQLite for proper concurrent access. |
| Environment configuration | Move API URL, CORS origins, DB path to configurable env vars. |
| Game clock / timers | Add time controls per game. |
| Chat between players | Real-time messaging via the existing WebSocket connection. |
| Move history navigation | Allow browsing previous moves during an active game (undo/redo). |
| Notifications | Alert players when it's their turn, even if they're on a different page. |
| Profile photo | `image` column exists in the schema but is unused. |
| Full settings UI | Wire up the PATCH profile update endpoint in the Settings page. |
| Password change | Allow password change from the Settings page. |
| Mobile responsive UI | Adapt layout for smaller screens. |
| Accessibility | Ensure keyboard navigation, ARIA labels, and screen reader support. |
