package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.dto.CreateEmployeeRequest;
import com.sabbpe.dto.EmployeeResponse;
import com.sabbpe.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse employee = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Employee created", employee));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> listEmployees() {
        List<EmployeeResponse> employees = employeeService.listEmployees();
        return ResponseEntity.ok(ApiResponse.success(employees));
    }
}
