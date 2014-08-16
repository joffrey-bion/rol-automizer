package com.jbion.riseoflords;

import java.util.List;

import com.jbion.riseoflords.model.User;

public class Sequencer {

    private static final String INDENT = "   ";

    private Sleeper sleeper = new Sleeper();
    private WSAdapter rol = new WSAdapter();

    public static void main(String[] args) {

        new Sequencer().start();
        // rol.attack("klamberg");
    }

    private void start() {
        login();

        sleeper.sleep(6000, 8000);

        List<User> users = rol.listUsers(3000);
        printUsers(users);
        
        //attack(user);
        //sleeper.sleep(500, 1000);
        //storeGold();
    }

    private void login() {
        System.out.println("Logging in...");
        boolean success = rol.login("darklink", "kili");
        if (success) {
            System.out.println(INDENT + "Success!");
        } else {
            throw new RuntimeException("Login failure.");
        }
    }
    
    private void attack(User user) {
        System.out.println("Starting attack against user " + user.getName());
        System.out.println(INDENT + "Displaying user page...");
        boolean success = rol.displayUserPage(user.getName());
        if (!success) {
            System.err.println(INDENT + INDENT + "Something's wrong...");
            return;
        }
        
        sleeper.sleep(INDENT, 500, 1000);
        
        System.out.println(INDENT + "Attacking user...");
        success = rol.attack(user.getName());
        if (success) {
            System.out.println(INDENT + INDENT + "Victory!");
        } else {
            System.err.println(INDENT + INDENT + "Defeat!");
        }
    }

    private void storeGold() {
        System.out.println("Storing gold in chest...");
        System.out.println(INDENT + "Displaying chest page...");
        int amount = rol.getCurrentGoldFromChestPage();
        System.out.println(INDENT + amount + " gold to store");
        sleeper.sleep(INDENT, 500, 1000);
        System.out.println(INDENT + "Storing everything...");
        boolean success = rol.storeInChest(amount);
        if (success) {
            System.out.println(INDENT + INDENT + "The gold is safe!");
        } else {
            System.err.println(INDENT + INDENT + "Something went wrong!");
        }
    }
    
    private static void printUsers(List<User> users) {
        for (User user : users) {
            System.out.println(user);
        }
    }
}
