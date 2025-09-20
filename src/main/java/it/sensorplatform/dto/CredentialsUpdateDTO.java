package it.sensorplatform.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object used to update credentials information from the personal
 * area page.
 */
public class CredentialsUpdateDTO {

    @NotBlank
    private String visibleUsername;

    private String newPassword;

    private String confirmPassword;

    public String getVisibleUsername() {
        return visibleUsername;
    }

    public void setVisibleUsername(String visibleUsername) {
        this.visibleUsername = visibleUsername;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
