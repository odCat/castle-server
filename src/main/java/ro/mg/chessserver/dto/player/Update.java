package ro.mg.chessserver.dto.player;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Update {

    @Email(message = "Must be a valid email address")
    private String email;

    @Size(min = 4, max = 24, message = "Username must have 4-24 characters and include only letters and digits.")
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "Username must have 4-24 characters and include only letters and digits.")
    private String username;


    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,24}$",
            message = "Password must have 8-24 characters and include at least a digit, a lowercase, an uppercase and a symbol")
    private String password;

    private String fullName;
}
