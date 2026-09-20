package com.openSupports.demo.infra.util;

public final class Constants {
    private Constants() {}

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final int PASSWORD_MAX_LENGTH = 200;
    public static final long REMEMBER_TOKEN_EXPIRY_DAYS = 30;
    public static final String DEPT_DEFAULT_NAME = "Software Support";
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_REOPENED = "REOPENED";
    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
}
