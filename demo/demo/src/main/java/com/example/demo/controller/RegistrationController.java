package com.example.demo.controller;

import com.example.demo.dto.registrationclub.FullRegistrationDTO;
import com.example.demo.dto.registrationclub.RegistrationDetailDTO;
import com.example.demo.dto.registrationclub.RegistrationSummaryDTO;
import com.example.demo.entity.registerclub.RegistrationStatus;
import com.example.demo.service.registrationclub.AdminApprovalService;
import com.example.demo.service.registrationclub.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registrations")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;
    private final AdminApprovalService adminApprovalService;

    @PostMapping
    public ResponseEntity<RegistrationSummaryDTO> submitRegistration(@RequestBody FullRegistrationDTO dto) {
        return ResponseEntity.ok(registrationService.submitRegistration(dto));
    }

    @GetMapping
    public List<RegistrationSummaryDTO> getRegistrations(@RequestParam(required = false) RegistrationStatus status) {
        return registrationService.getRegistrations(status);
    }

    @GetMapping("/{id}")
    public RegistrationDetailDTO getRegistrationDetail(@PathVariable Long id) {
        return registrationService.getRegistrationDetail(id);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, String>> approveRegistration(@PathVariable Long id) {
        adminApprovalService.approveRegistration(id);
        return ResponseEntity.ok(Map.of("message", "Duyệt đơn đăng ký thành công"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectRegistration(@PathVariable Long id,
                                                                  @RequestParam(required = false) String note) {
        adminApprovalService.rejectRegistration(id, note);
        return ResponseEntity.ok(Map.of("message", "Từ chối đơn đăng ký thành công"));
    }
}
