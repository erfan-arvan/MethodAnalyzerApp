package com.example;

public class ChildSample extends CustomObject {

    public ChildSample(int value, String name) {
        super(value, name);
    }

    @Override
    public void process() {
        System.out.println("Child processing: " + name);
    }
}

