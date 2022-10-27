package me.nald.blog.data.persistence.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
