package dev.dhc.http;

public enum StatusCode {
    BadRequest, NotFound, OK, InternalServerError, Created;

    public int code() {
        return switch (this) {
            case BadRequest -> 400;
            case NotFound -> 404;
            case OK -> 200;
            case InternalServerError -> 500;
            case Created -> 201;
        };
    }

    public String message() {
        return switch (this) {
            case BadRequest -> "Bad Request";
            case NotFound -> "Not Found";
            case OK -> "OK";
            case InternalServerError -> "Internal Server Error";
            case Created -> "Created";
        };
    }
}
