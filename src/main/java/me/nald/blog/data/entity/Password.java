package me.nald.blog.data.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Password {

    @Column(name = "password", nullable = false, length = 200)
    private String password;

    @Builder
    public Password(String password) {
        this.password = new BCryptPasswordEncoder().encode(password);
    }

    public boolean isMatched(final String rawPassword) {
        final boolean matches = isMatches(rawPassword);
        return matches;
    }

    private boolean isMatches(String rawPassword) {
        return new BCryptPasswordEncoder().matches(rawPassword, this.password);
    }



}
