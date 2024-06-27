package me.nald.blog.model;

public class JsonPatch<T> {

    private String op;
    private String path;
    private T value;

    private JsonPatch() {}

    private JsonPatch(String op) {
        this.op = op;
    }


    private JsonPatch<T> withPath(String path) {
        this.path = path;
        return this;
    }

    private JsonPatch<T> withValue(T value) {
        this.value = value;
        return this;
    }

    public static <T> JsonPatch<T> add(String path, T value) {
        return new JsonPatch<T>(Operation.ADD.getValue()).withPath(path).withValue(value);
    }

    public static <T> JsonPatch<T> remove(String path, T value) {
        return new JsonPatch<T>(Operation.REMOVE.getValue()).withPath(path).withValue(value);
    }

    public static <T> JsonPatch<T> replace(String path, T value) {
        return new JsonPatch<T>(Operation.REPLACE.getValue()).withPath(path).withValue(value);
    }

    public static <T> JsonPatch<T> move(String path, T value) {
        return new JsonPatch<T>(Operation.MOVE.getValue()).withPath(path).withValue(value);
    }

    public static <T> JsonPatch<T> copy(String path, T value) {
        return new JsonPatch<T>(Operation.COPY.getValue()).withPath(path).withValue(value);
    }

    public static <T> JsonPatch<T> test(String path, T value) {
        return new JsonPatch<T>(Operation.TEST.getValue()).withPath(path).withValue(value);
    }

    @Override
    public String toString() {
        return "{" +
                "\"op\":\"" + op + "\"" +
                ", \"path\":\"" + path + "\"" +
                ", \"value\":\"" + value.toString() + "\"" +
                '}';
    }

    enum Operation {
        ADD("add"),
        REMOVE("remove"),
        REPLACE("replace"),
        MOVE("move"),
        COPY("copy"),
        TEST("test");

        private String value;

        private Operation(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }
}
