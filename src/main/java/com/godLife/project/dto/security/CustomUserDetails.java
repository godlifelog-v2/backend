package com.godLife.project.dto.security;

import com.godLife.project.dto.model.user.UserDTO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

public class CustomUserDetails implements UserDetails {

  private final UserDTO userDTO;
  private final int userIdx;

  public CustomUserDetails(UserDTO userDTO) {
    this.userDTO = userDTO;
    this.userIdx = userDTO.getUserIdx();
  }

  public CustomUserDetails(UserDTO userDTO, int userIdx) {
    this.userDTO = userDTO;
    this.userIdx = userIdx;
  }

  public int getUserIdx() {
    return userIdx;
  }

  public int getAuthorityIdx() {
    return userDTO.getAuthorityIdx();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {

    Collection<GrantedAuthority> collection = new ArrayList<>();

    collection.add((GrantedAuthority) () -> String.valueOf(userDTO.getAuthorityIdx()));

    return collection;
  }

  @Override
  public String getPassword() {

    return userDTO.getUserPw();
  }

  @Override
  public String getUsername() {

    return userDTO.getUserId();
  }

  @Override
  public boolean isAccountNonExpired() {

    return true;
  }

  @Override
  public boolean isAccountNonLocked() {

    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {

    return true;
  }

  @Override
  public boolean isEnabled() {

    return true;
  }

  public UserDTO getUserDTO() {
    return userDTO;
  }
}
