package org.cartscheduler.controller.web;

import lombok.RequiredArgsConstructor;
import org.cartscheduler.service.AdminDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {
    private final AdminDashboardService dashboardService;

    @GetMapping({"/", "/admin", "/admin/"})
    public String dashboard(
            @RequestParam(required = false) Long scheduleId,
            Model model
    ) {
        model.addAttribute("summary", dashboardService.getSummary(scheduleId));
        return "admin/dashboard";
    }
}
