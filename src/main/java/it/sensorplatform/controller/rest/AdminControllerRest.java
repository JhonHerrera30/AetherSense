package it.sensorplatform.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.sensorplatform.service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminControllerRest {

    @Autowired
    private AdminService adminService;

    @PostMapping("/{adminId}/authorize/{operatorId}")
    public void authorizeOperator(@PathVariable Long adminId, @PathVariable Long operatorId) {
        adminService.authorizeOperator(adminId, operatorId);
    }

    @DeleteMapping("/{adminId}/authorize/{operatorId}")
    public void removeAuthorizedOperator(@PathVariable Long adminId, @PathVariable Long operatorId) {
        adminService.removeAuthorizedOperator(adminId, operatorId);
    }
}
