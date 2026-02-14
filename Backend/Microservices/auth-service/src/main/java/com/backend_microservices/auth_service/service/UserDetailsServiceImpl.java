package com.backend_microservices.auth_service.service;

import com.backend_microservices.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

/*
    this class tells Spring Security: “When you give me a username, I will go to the database, find that user,
    and return its details in a format Spring Security understands.” Spring Security does NOT know our User entity
    It only understands something called: UserDetails !! this class is used when we get a jwt token, and we want to
    get the authorities that the user have depending on his roles, so complex yea, but we can do nothing about it TwT
    main mission here: our User entity  --->  Spring Security UserDetails
*/

// UserDetailsService is an interface from Spring Security, Any class that implements it MUST provide loadUserByUsername method
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // Spring Security will call this method automatically when authenticating a user
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // here we get the user by its username from our database
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // here we get the authorities/permissions of the user depending on its role_name
        // SimpleGrantedAuthority is Spring Security’s way of representing permissions
        /*
        example of the outputs:
                    user.getRoles() = [Role(ROLE_USER), Role(ROLE_ADMIN)]
                    role.getRoleName() = "ROLE_USER"
                    new SimpleGrantedAuthority("ROLE_USER")
                    authorities = ["ROLE_USER", "ROLE_ADMIN"]
        */
        var authorities = user.getRoles().stream()  // stream() lets us process each role one by one
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))  //For each Role object, create a SimpleGrantedAuthority
                .collect(Collectors.toSet());

        // FOCUS very well here, this User is not our entity's User, this is the User that Spring Security
        // understands, that contains the username and password and a set of roles for this user
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
}

