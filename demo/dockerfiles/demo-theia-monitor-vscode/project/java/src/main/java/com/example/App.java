package com.example;

public class App {

    private String name;

    public App(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        App app = new App("Hello World!");
        System.out.println(app.getName());
    }
}
