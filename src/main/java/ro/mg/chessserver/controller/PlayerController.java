package ro.mg.chessserver.controller;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ro.mg.chessserver.dto.player.Register;
import ro.mg.chessserver.model.Game;
import ro.mg.chessserver.model.Player;
import ro.mg.chessserver.dto.player.Login;
import ro.mg.chessserver.dto.player.Update;
import ro.mg.chessserver.service.PlayerService;


@RestController
@RequestMapping("/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(@Autowired PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    @ResponseBody
    public List<Player> getAllPlayers() {
        return playerService.getPlayers();
    }

    @PostMapping("/register")
    public ResponseEntity<Register> register(@Valid @RequestBody Register register) {
        if (playerService.addPlayer(register))
            return ResponseEntity.status(HttpStatus.CREATED).body(register);
        else
            return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PostMapping("/login")
    public ResponseEntity<Login> login(@Valid @RequestBody Login login) {
        login = playerService.authenticate(login);

        return ResponseEntity.status(HttpStatus.OK).body(login);
    }

    @PatchMapping
    public ResponseEntity<Player> update(@RequestParam long id, @RequestBody Update update) {
        Player updated = playerService.update(id, update);
        if (updated == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        else
            return ResponseEntity.status(HttpStatus.OK).body(updated);
    }

    @DeleteMapping()
    public ResponseEntity<Void> delete(@RequestParam long id, Authentication auth) {
        long loggedInUserId = Long.parseLong(auth.getName());
        if (loggedInUserId != id)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        playerService.deletePlayer(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history/{id}")
    public List<Game> getHistory(@PathVariable long id) {
        return playerService.getHistory(id);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName,errorMessage);
        });

        return errors;
    }
}
