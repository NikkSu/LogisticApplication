package com.logistics.suppliers.controller.rest;

import com.logistics.suppliers.model.Role;
import com.logistics.suppliers.model.User;
import com.logistics.suppliers.repository.OrderRepository;
import com.logistics.suppliers.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String viewProfile(Model model, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);

        long orderCount = orderRepository.countByCreatedBy(user);
        model.addAttribute("orderCount", orderCount);

        return "user-profile";
    }

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute User updatedData, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        user.setFirstName(updatedData.getFirstName());
        user.setLastName(updatedData.getLastName());
        user.setPhone(updatedData.getPhone());
        userRepository.save(user);
        return "redirect:/profile?success=true";
    }

    // В UserProfileController.java

    @PostMapping("/update-avatar")
    public String updateAvatar(@RequestParam String avatarUrl, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return "redirect:/profile?success=avatar";
    }

    @GetMapping("/change-password")
    public String changePasswordForm() {
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(authentication.getName()).get();

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Старый пароль введен неверно");
            return "redirect:/profile/change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Пароли не совпадают");
            return "redirect:/profile/change-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("message", "Пароль успешно изменен");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/profile";
    }
}