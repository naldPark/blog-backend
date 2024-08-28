package me.nald.blog.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static me.nald.blog.util.SecurityUtils.decrypt;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Password {

    @Column(name = "password", nullable = false, length = 200)
    private String hashPassword;

    @Builder
    public Password(String password)  {
      try {
        this.hashPassword = decrypt(password);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

//    public boolean isMatched(final String rawPassword) {
//        final boolean matches = isMatches(rawPassword);
//        return matches;
//    }
//
//    private boolean isMatches(String rawPassword) {
//        return new BCryptPasswordEncoder().matches(rawPassword, this.password);
//    }

}
