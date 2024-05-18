package dev.dhc.codecrafters_http;

import java.nio.file.Path;

record Args(Path root) {

    public static Args parse(String[] args) {
        Path root = args.length == 0 ? Path.of(".") : Path.of(args[1]);
        return new Args(root);
    }
}
