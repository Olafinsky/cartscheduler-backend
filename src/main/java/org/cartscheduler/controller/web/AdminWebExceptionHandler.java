package org.cartscheduler.controller.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(basePackages = "org.cartscheduler.controller.web")
public class AdminWebExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(IllegalArgumentException exception, Model model, HttpServletResponse response) {
        model.addAttribute("status", HttpStatus.NOT_FOUND.value());
        model.addAttribute("message", exception.getMessage());
        return "error";
    }
}
