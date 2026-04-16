package com.logistics.suppliers.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Exception ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataError(DataIntegrityViolationException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "Ошибка данных: возможно, текст слишком длинный или нарушена уникальность.");
        redirectAttributes.addFlashAttribute("messageType", "error");
        return "redirect:/profile";
    }
}