package org.studyeasy.SpringStarter.Controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.studyeasy.SpringStarter.models.Account;
import org.studyeasy.SpringStarter.services.AccountService;
import org.studyeasy.SpringStarter.services.EmailService;
import org.studyeasy.SpringStarter.util.constants.AppUtil;
import org.studyeasy.SpringStarter.util.constants.email.EmailDetails;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;





@Controller
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private EmailService emailService;

    @Value("${site.domain}")
    private String site_domain;



    @GetMapping("/register")
    public String register(Model model) {
        Account account = new Account();
        model.addAttribute("account", account);
        return "register";
    }

    @PostMapping("/register")
    public String register_user(@Valid @ModelAttribute Account account, BindingResult result) {
        if (result.hasErrors()) {
            return "register";
        }
        accountService.save(account);
        return "redirect:/";
    }
    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String profile(Model model, Principal principal) 
    {
        String authUser = "email";
        if (principal != null) {
            authUser = principal.getName();
        }
        Optional<Account> optionalAccount = accountService.findOneByEmail(authUser);
        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();
            model.addAttribute("account", account);
            model.addAttribute("photo", account.getPhoto());
            return "profile";
        } else {
            return "redirect:/?error";
        }
    }
    
    @PostMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String post_profile(@Valid @ModelAttribute Account account, BindingResult bindingResult, Principal principal,
            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return "profile";
        }
        String authUser = "email";
        if (principal != null) {
            authUser = principal.getName();
        }
        Optional<Account> optionalAccount = accountService.findOneByEmail(authUser);
        if (optionalAccount.isPresent()) {
            Account account_by_id = accountService.findById(account.getId()).get();
            account_by_id.setAge(account.getAge());
            account_by_id.setDate_of_birth(account.getDate_of_birth());
            account_by_id.setFirstName(account.getFirstName());
            account_by_id.setGender(account.getGender());
            account_by_id.setLastName(account.getLastName());
            account_by_id.setPassword(account.getPassword());

            accountService.save(account_by_id);
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            return "redirect:/";
        } else {
            return "redirect:/?error";
        }
    }
    
    @PostMapping("/update_photo")
    @PreAuthorize("isAuthenticated()")
    public String update_photo(@RequestParam("file") MultipartFile file, RedirectAttributes attributes, Principal principal)
    {
        if (file.isEmpty()) {
            attributes.addFlashAttribute("error", "No file uploaded");
            return "redirect:/profile";
        } else {
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            try{
                int length = 10;
                boolean useLetters = true;
                boolean useNumbers = true;
                String generatedString = RandomStringUtils.random(length, useLetters, useNumbers);
                String final_photo_name = generatedString + fileName;
                String absolute_fileLocation = AppUtil.get_upload_path(final_photo_name);
                
                Path path = Paths.get(absolute_fileLocation);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                attributes.addFlashAttribute("message", "Successful upload!");

                String authUser = "email";
                if (principal != null) {
                    authUser = principal.getName();
                }
                Optional<Account> optionalAccount = accountService.findOneByEmail(authUser);
                if (optionalAccount.isPresent())
                {
                    Account account = optionalAccount.get();
                    Account account_by_id = accountService.findById(account.getId()).get();
                    String photo_prefix = "/resources/static/**";
                    String relative_fileLocation = photo_prefix.replace("**", "uploads/" + final_photo_name);
                    account_by_id.setPhoto(relative_fileLocation);
                    accountService.save(account_by_id);
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return "redirect:/profile";
            } catch (Exception e){

            }
        }
        return "redirect:/profile?error";
    }
    
    @GetMapping("/forgot-password")
    public String forgot_password(Model model) {
        return "/forgot_password";
    }
    @PostMapping("/reset-password")
    public String reset_password(@RequestParam("email") String _email, RedirectAttributes attributes, Model model)
    {
        Optional<Account> optionalAccount = accountService.findOneByEmail(_email);
        if (optionalAccount.isPresent()) {
            Account account = accountService.findById(optionalAccount.get().getId()).get();
            String reset_token = UUID.randomUUID().toString();
            account.setPassword_reset_token(reset_token);
            account.setPassword_reset_token_expiry(LocalDateTime.now().plusMinutes(600));
            accountService.save(account);
            String reset_message = "This is the reset password link: " + site_domain + "change-password?token="
                    + reset_token;
            EmailDetails emailDetails = new EmailDetails(account.getEmail(), reset_message, "Reset password demo");
            if (emailService.sendSimpleEmail(emailDetails) == false) {
                attributes.addFlashAttribute("error", "Error while sending email");
                return "redirect:/forgot-password";
            }
            attributes.addFlashAttribute("message", "Password reset, email sent");
            return "redirect:/login";
        } else {
            attributes.addFlashAttribute("error", "No user found with the email provided");
            return "redirect:/forgot-password";
        }
    }
    // @GetMapping("/change-password")
    // public String change_password(Model model, @RequestParam("token") String token, RedirectAttributes attributes)
    // {
    //     Optional<Account> optionalAccount = accountService.findByToken(token);
    //     if (optionalAccount.isPresent())
    //     {
    //         long account_id = optionalAccount.get().getId();
    //         LocalDateTime now = LocalDateTime.now();
    //         if (now.isAfter(optionalAccount.get().getPassword_reset_token_expiry())) {
    //             attributes.addFlashAttribute("error", "Token expired");
    //             return "redirect:/forgot-password";
    //         }
    //         model.addAttribute("account_id", account_id);
    //         return "change_password";
    //     }
    //     attributes.addFlashAttribute("error", "Invalid token");
    //     return "redirect:/forgot-password";
    // }

}
