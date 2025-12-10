package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Dùng UserRepository để tìm đối tượng Users của bạn
        Users myUser = userRepository.findByUsername(username);
        if (myUser == null) {
            throw new UsernameNotFoundException("Không tìm thấy người dùng với tên đăng nhập: " + username);
        }

        // 2. Chuyển đổi vai trò (Role) của bạn thành một quyền (GrantedAuthority)
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + myUser.getRole().name());

        // 3. Trả về một đối tượng UserDetails mà Spring Security hiểu được
        // Gồm: username, password (đã băm), và danh sách quyền
        return new User(myUser.getUsername(), myUser.getPassword(), Collections.singleton(authority));
    }
}