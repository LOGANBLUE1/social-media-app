package com.example.app.services;


import com.example.app.dataAccess.UserRepository;
import com.example.app.entities.User;
import com.example.app.security.JWTUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService { //UserDetails için service oluşturduk.

    private UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByName(username);
        return JWTUserDetails.create(user);  //gelen user' i UserDetails olarak döner.
    }

    public UserDetails loadUserById(Long id) {  //lazım olursa diye yazdım.
        User user = userRepository.findById(id).get();
        return JWTUserDetails.create(user);
    }

}
