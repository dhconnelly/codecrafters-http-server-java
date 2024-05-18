package dev.dhc.http;

public interface Handler {

    Response handle(Request req);

}
