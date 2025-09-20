package it.sensorplatform.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CredentialsUpdateForm {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @Pattern(regexp = "^$|.{8,255}$", message = "La password deve contenere almeno 8 caratteri")
    private String password;

    private String confirmPassword;

    @AssertTrue(message = "Le password non coincidono")
    public boolean isPasswordConfirmed() {
        if (password == null || password.isBlank()) {
            return confirmPassword == null || confirmPassword.isBlank();
        }
        return password.equals(confirmPassword);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
