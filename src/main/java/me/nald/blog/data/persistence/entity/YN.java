package me.nald.blog.data.persistence.entity;

import lombok.Getter;

@Getter
public enum YN {
    Y(true),
    N(false);

    private boolean v;

    YN(boolean value) {
        this.v = value;
    }

    public boolean isY() {
        return this.equals(YN.Y);
    }

    public boolean isN() {
        return this.equals(YN.N);
    }

    public static YN of (String value) {
        return YN.Y.name().equals(value) ? YN.Y : YN.N;
    }
}
