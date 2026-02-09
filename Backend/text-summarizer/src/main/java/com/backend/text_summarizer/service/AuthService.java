package com.backend.text_summarizer.service;

import com.backend.text_summarizer.dto.AuthResponse;
import com.backend.text_summarizer.dto.LoginRequest;
import com.backend.text_summarizer.dto.RegisterRequest;
import com.backend.text_summarizer.entity.Role;
import com.backend.text_summarizer.entity.RoleName;
import com.backend.text_summarizer.entity.User;
import com.backend.text_summarizer.repository.RoleRepository;
import com.backend.text_summarizer.repository.UserRepository;
import com.backend.text_summarizer.security.JwtUtil;
import com.backend.text_summarizer.security.PasswordValidator;
import lombok.Data;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@Data
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private com.backend.text_summarizer.entity.User User;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;


    public String Register(RegisterRequest request){
        //1.check if username already exists
        if(userRepository.findByUsername(request.getUsername()).isPresent()){
            throw new RuntimeException("Error: Username is already taken!");}
        //check email
        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Error: Email is already in use!");
        }
        //2.check the password matches the pattern
        if(!PasswordValidator.isValid(request.getPassword())){
            throw new IllegalArgumentException("Password too weak! Needs: 8+ chars, " +
                    "1 Upper, 1 Number, 1 Special Char.");
        }

        //3. create new user object
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        //4.assign userRole
        // 1. Get the actual Role object (No Optional wrapping needed!)
        Role userRole = roleRepository.findByRoleName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        // 2. Create a Set that holds ROLES (not Optionals)
        Set<Role> roles = new HashSet<>();
        // 3. Add the role to the box
        roles.add(userRole);
        // 4. Give the box to the user
        user.setRoles(roles);
        // 5. Save to Database
        userRepository.save(user);

        return "User registered successfully!";
        }
        public AuthResponse Login(LoginRequest request){
        //1. check username&password
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        //2.if we reach this part that means user is valid now we will get the userobject
            var user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

        //3.Generate Token
            String jwtToken = jwtUtil.generateToken(user.getUsername());
        //4. return teh token
            return new AuthResponse(jwtToken);

        }
    }

