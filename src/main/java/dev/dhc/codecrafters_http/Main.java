package dev.dhc.codecrafters_http;

import java.io.IOException;

import dev.dhc.http.Server;

public class Main {

    static final void die(Exception e) {
        e.printStackTrace(System.err);
        System.exit(1);
    }

    public static void main(String[] args) {
        int port = 4221;
        var server = new Server(port, Router.make(Args.parse(args).root()));
        System.out.println("listening at http://localhost:%d".formatted(port));
        try {
            server.run();
        } catch (IOException e) {
            die(e);
        }
    }
}
